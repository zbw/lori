package de.zbw.persistence.lori.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.GroupVersion
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Tracer
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant

/**
 * Execute SQL queries strongly related to groups.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GroupDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
    private val gson: Gson,
) {
    suspend fun insertGroup(group: Group, useGivenId: Boolean = false): Int =
        connectionPool.useConnection { connection ->
            val stmt = if (useGivenId) {
                STATEMENT_INSERT_GROUP_WITH_ID
            } else {
                STATEMENT_INSERT_GROUP
            }
            val prepStmt: PreparedStatement =
                connection
                    .prepareStatement(stmt, Statement.RETURN_GENERATED_KEYS)
                    .apply {
                        val now = Instant.now()
                        this.setIfNotNull(1, group.description) { value, idx, prepStmt ->
                            prepStmt.setString(idx, value)
                        }
                        val jsonObj = PGobject()
                        jsonObj.type = "json"
                        jsonObj.value = gson.toJson(group.entries)
                        this.setObject(2, jsonObj)
                        this.setString(3, group.title)
                        this.setIfNotNull(4, group.createdBy) { value, idx, prepStmt ->
                            prepStmt.setString(idx, value)
                        }
                        this.setTimestamp(5, Timestamp.from(now))
                        this.setIfNotNull(6, group.createdBy) { value, idx, prepStmt ->
                            prepStmt.setString(idx, value)
                        }
                        this.setTimestamp(7, Timestamp.from(now))
                        this.setInt(8, group.version)
                        if (useGivenId) {
                            this.setInt(9, group.groupId)
                        }
                    }
            val span = tracer.spanBuilder("insertGroup").startSpan()
            try {
                span.makeCurrent()
                val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
                return@useConnection if (affectedRows > 0) {
                    val rs: ResultSet = prepStmt.generatedKeys
                    rs.next()
                    rs.getInt(1)
                } else {
                    throw IllegalStateException("No row has been inserted.")
                }
            } finally {
                span.end()
            }
        }

    // TODO: Get old versions as well
    suspend fun getLatestVersionGroupsByIds(groupIds: List<Int>): List<Group> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_GROUPS_BY_IDS).apply {
                    this.setArray(1, connection.createArrayOf("integer", groupIds.toTypedArray()))
                }
            val span = tracer.spanBuilder("getGroupsByIds").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            val allGroupVersions: List<Group> = generateSequence {
                if (rs.next()) {
                    extractGroupRS(rs, gson)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
            allGroupVersions.groupBy { it.groupId }.mapValues { e: Map.Entry<Int, List<Group>> ->
                e.value.sortedByDescending { g -> g.version }.firstOrNull()
            }.values.filterNotNull()
        }

    suspend fun getGroupById(groupId: Int): Group? =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_GROUP_BY_ID_WITH_LATEST_VERSION).apply {
                    this.setInt(1, groupId)
                }

            val span = tracer.spanBuilder("getGroupByIdWithLatestVersion").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            val receivedGroup = if (rs.next()) {
                extractGroupRS(rs, gson)
            } else {
                return@useConnection null
            }
            return@useConnection receivedGroup.copy(
                oldVersions = getAllGroupVersionsById(receivedGroup.groupId)
                    .takeIf{it.size > 1}
                    ?.let{it.sortedByDescending { g -> g.version }}
                    ?.drop(1)
                    ?:emptyList()
            )
        }

    suspend fun getAllGroupVersionsById(groupId: Int): List<GroupVersion> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_GROUPS_BY_ID).apply {
                    this.setInt(1, groupId)
                }
            val span = tracer.spanBuilder("getAllGroupVersionsById").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            generateSequence {
                if (rs.next()) {
                    GroupVersion(
                        groupId = rs.getInt(1),
                        createdBy = rs.getString(2),
                        createdOn = rs.getTimestamp(3)?.toOffsetDateTime(),
                        description = rs.getString(4),
                        version = rs.getInt(5)
                    )
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun getGroupByIdAndVersion(groupId: Int, version: Int): Group? =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_GROUP_BY_ID_AND_VERSION).apply {
                    this.setInt(1, groupId)
                    this.setInt(2, version)
                }

            val span = tracer.spanBuilder("getGroupByIdAndVersion").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            val receivedGroup = if (rs.next()) {
                extractGroupRS(rs, gson)
            } else {
                return@useConnection null
            }
            return@useConnection receivedGroup.copy(
                oldVersions = getAllGroupVersionsById(receivedGroup.groupId)
                    .takeIf{it.size > 1}
                    ?.let{it.sortedByDescending { g -> g.version }}
                    ?.drop(1)
                    ?:emptyList()
            )
        }

    suspend fun deleteGroupPair(
        groupId: Int,
        rightId: String,
    ): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_GROUP_RIGHT_PAIR).apply {
                    this.setInt(1, groupId)
                    this.setString(2, rightId)
                }
            val span = tracer.spanBuilder("deleteGroupPair").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun deleteGroupPairsByRightId(rightId: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_GROUP_RIGHT_PAIR_BY_RIGHT_ID).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("deleteGroupPairsByRightId").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    /**
     * Get the ids of all rights that use a given group-id.
     */
    suspend fun getRightsByGroupId(groupId: Int): List<String> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_RIGHTS_BY_GROUP_ID).apply {
                    this.setInt(1, groupId)
                }
            val span = tracer.spanBuilder("getRightsByGroupId").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            return@useConnection generateSequence {
                if (rs.next()) {
                    rs.getString(1)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun getGroupsByRightId(rightId: String): List<Group> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_GROUPS_BY_RIGHT_ID).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("getGroupsByRightId").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            val groupIds: List<Int> =
                generateSequence {
                    if (rs.next()) {
                        rs.getInt(1)
                    } else {
                        null
                    }
                }.takeWhile { true }.toList()

            return@useConnection getLatestVersionGroupsByIds(groupIds)
        }

    suspend fun insertGroupRightPair(
        rightId: String,
        groupId: Int,
    ): String =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection
                    .prepareStatement(STATEMENT_INSERT_GROUP_RIGHT_PAIR, Statement.RETURN_GENERATED_KEYS)
                    .apply {
                        this.setInt(1, groupId)
                        this.setString(2, rightId)
                    }
            val span = tracer.spanBuilder("insertGroupRightPair").startSpan()
            try {
                span.makeCurrent()
                val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
                return@useConnection if (affectedRows > 0) {
                    val rs: ResultSet = prepStmt.generatedKeys
                    rs.next()
                    rs.getString(1)
                } else {
                    throw IllegalStateException("No row has been inserted.")
                }
            } finally {
                span.end()
            }
        }

    suspend fun getGroupList(
        limit: Int,
        offset: Int,
    ): List<Group> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_GROUP_LIST).apply {
                    this.setInt(1, limit)
                    this.setInt(2, offset)
                }

            val span = tracer.spanBuilder("getGroupList").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            val groups = generateSequence {
                if (rs.next()) {
                    extractGroupRS(rs, gson)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
            return@useConnection groups.map{
                it.copy(
                    oldVersions = getAllGroupVersionsById(it.groupId)
                        .takeIf{it.size > 1}
                        ?.let{it.sortedByDescending { g -> g.version }}
                        ?.drop(1)
                        ?:emptyList()
                )
            }
        }

    suspend fun deleteGroupById(groupId: Int): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_GROUP_BY_ID).apply {
                    this.setInt(1, groupId)
                }
            val span = tracer.spanBuilder("deleteGroup").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun updateGroup(group: Group, updateBy: String): Int =
        connectionPool.useConnection { connection ->
            // 1. Get existing group
            val existingGroup: Group = getGroupById(group.groupId) ?: return@useConnection 0
            // 2. Check if differences exist
            if (group.entries == existingGroup.entries &&
                group.title == existingGroup.title &&
                group.description == existingGroup.description
            ) {
                // No changes
                return@useConnection 0
            } else {
                insertGroup(
                    group.copy(
                        version = existingGroup.version + 1,
                        lastUpdatedBy = updateBy,
                        createdBy = updateBy,
                    ), true
                )
            }

        }

    companion object {
        private const val TABLE_NAME_GROUP_RIGHT_MAP = "group_right_map"
        private const val TABLE_NAME_RIGHT_GROUP = "right_group"
        const val COLUMN_CREATED_BY = "created_by"
        const val COLUMN_CREATED_ON = "created_on"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_GROUP_ID = "group_id"
        const val COLUMN_IP_ADDRESSES = "ip_addresses"
        const val COLUMN_LAST_UPDATED_BY = "last_updated_by"
        const val COLUMN_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_RIGHT_ID = "right_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_VERSION = "version"
        const val STATEMENT_INSERT_GROUP =
            "INSERT INTO $TABLE_NAME_RIGHT_GROUP" +
                    " ($COLUMN_DESCRIPTION,$COLUMN_IP_ADDRESSES,$COLUMN_TITLE," +
                    "$COLUMN_CREATED_BY,$COLUMN_CREATED_ON,$COLUMN_LAST_UPDATED_BY," +
                    "$COLUMN_LAST_UPDATED_ON,$COLUMN_VERSION)" +
                    " VALUES(" +
                    "?,?,?," +
                    "?,?,?," +
                    "?,?)"

        const val STATEMENT_INSERT_GROUP_WITH_ID =
            "INSERT INTO $TABLE_NAME_RIGHT_GROUP" +
                    " ($COLUMN_DESCRIPTION,$COLUMN_IP_ADDRESSES,$COLUMN_TITLE," +
                    "$COLUMN_CREATED_BY,$COLUMN_CREATED_ON,$COLUMN_LAST_UPDATED_BY," +
                    "$COLUMN_LAST_UPDATED_ON,$COLUMN_VERSION,$COLUMN_GROUP_ID)" +
                    " VALUES(" +
                    "?,?,?," +
                    "?,?,?," +
                    "?,?,?)"

        const val STATEMENT_GET_GROUP_BY_ID_AND_VERSION =
            "SELECT $COLUMN_GROUP_ID,$COLUMN_DESCRIPTION,$COLUMN_IP_ADDRESSES,$COLUMN_TITLE," +
                    "$COLUMN_CREATED_BY,$COLUMN_CREATED_ON,$COLUMN_LAST_UPDATED_BY," +
                    "$COLUMN_LAST_UPDATED_ON,$COLUMN_VERSION" +
                    " FROM $TABLE_NAME_RIGHT_GROUP" +
                    " WHERE $COLUMN_GROUP_ID = ? AND $COLUMN_VERSION = ?;"

        const val STATEMENT_GET_GROUPS_BY_ID =
            "SELECT $COLUMN_GROUP_ID,$COLUMN_CREATED_BY,$COLUMN_CREATED_ON," +
                    "$COLUMN_DESCRIPTION,$COLUMN_VERSION" +
                    " FROM $TABLE_NAME_RIGHT_GROUP" +
                    " WHERE $COLUMN_GROUP_ID = ?;"

        const val STATEMENT_GET_GROUP_BY_ID_WITH_LATEST_VERSION =
            "SELECT $COLUMN_GROUP_ID,$COLUMN_DESCRIPTION,$COLUMN_IP_ADDRESSES,$COLUMN_TITLE," +
                    "$COLUMN_CREATED_BY,$COLUMN_CREATED_ON,$COLUMN_LAST_UPDATED_BY," +
                    "$COLUMN_LAST_UPDATED_ON,$COLUMN_VERSION" +
                    " FROM $TABLE_NAME_RIGHT_GROUP" +
                    " WHERE $COLUMN_GROUP_ID = ?" +
                    " ORDER BY $COLUMN_VERSION DESC" +
                    " LIMIT 1;"

        const val STATEMENT_GET_GROUP_LIST =
            "SELECT t1.$COLUMN_GROUP_ID,t1.$COLUMN_DESCRIPTION,t1.$COLUMN_IP_ADDRESSES,t1.$COLUMN_TITLE," +
                    "t1.$COLUMN_CREATED_BY,t1.$COLUMN_CREATED_ON,t1.$COLUMN_LAST_UPDATED_BY," +
                    "t1.$COLUMN_LAST_UPDATED_ON,t1.$COLUMN_VERSION" +
                    " FROM $TABLE_NAME_RIGHT_GROUP t1" +
                    " JOIN (" +
                    " SELECT $COLUMN_GROUP_ID, MAX(${COLUMN_VERSION}) AS max_version" +
                    " FROM $TABLE_NAME_RIGHT_GROUP" +
                    " GROUP BY $COLUMN_GROUP_ID" +
                    ") as t2" +
                    " ON t1.${COLUMN_GROUP_ID}=t2.${COLUMN_GROUP_ID} AND t1.${COLUMN_VERSION}=t2.max_version" +
                    " LIMIT ? OFFSET ?;"

        const val STATEMENT_INSERT_GROUP_RIGHT_PAIR =
            "INSERT INTO $TABLE_NAME_GROUP_RIGHT_MAP" +
                    " ($COLUMN_GROUP_ID, $COLUMN_RIGHT_ID)" +
                    " VALUES(?,?);"

        const val STATEMENT_DELETE_GROUP_RIGHT_PAIR =
            "DELETE" +
                    " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                    " WHERE $COLUMN_GROUP_ID = ? AND" +
                    " $COLUMN_RIGHT_ID = ?;"

        const val STATEMENT_DELETE_GROUP_RIGHT_PAIR_BY_RIGHT_ID =
            "DELETE" +
                    " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                    " WHERE $COLUMN_RIGHT_ID = ?;"

        const val STATEMENT_DELETE_GROUP_BY_ID =
            "DELETE " +
                    "FROM $TABLE_NAME_RIGHT_GROUP" +
                    " WHERE $COLUMN_GROUP_ID = ?;"

        const val STATEMENT_GET_RIGHTS_BY_GROUP_ID =
            "SELECT $COLUMN_RIGHT_ID" +
                    " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                    " WHERE $COLUMN_GROUP_ID = ?;"

        const val STATEMENT_GET_GROUPS_BY_RIGHT_ID =
            "SELECT $COLUMN_GROUP_ID" +
                    " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                    " WHERE $COLUMN_RIGHT_ID = ?;"

        const val STATEMENT_GET_GROUPS_BY_IDS =
            "SELECT $COLUMN_GROUP_ID,$COLUMN_DESCRIPTION,$COLUMN_IP_ADDRESSES,$COLUMN_TITLE," +
                    "$COLUMN_CREATED_BY,$COLUMN_CREATED_ON,$COLUMN_LAST_UPDATED_BY," +
                    "$COLUMN_LAST_UPDATED_ON,$COLUMN_VERSION" +
                    " FROM $TABLE_NAME_RIGHT_GROUP" +
                    " WHERE $COLUMN_GROUP_ID = ANY(?);"

        private fun extractGroupRS(
            rs: ResultSet,
            gson: Gson,
        ): Group {
            val groupListType: Type = object : TypeToken<ArrayList<GroupEntry>>() {}.type
            val groupId = rs.getInt(1)
            val description = rs.getString(2)
            val ipAddressJson: String? =
                rs
                    .getObject(3, PGobject::class.java)
                    .value
            val title = rs.getString(4)
            val createdBy = rs.getString(5)
            val createdOn = rs.getTimestamp(6)?.toOffsetDateTime()
            val lastUpdatedBy = rs.getString(7)
            val lastUpdatedOn = rs.getTimestamp(8)?.toOffsetDateTime()
            val version = rs.getInt(9)

            return Group(
                groupId = groupId,
                description = description,
                entries =
                ipAddressJson
                    ?.let { gson.fromJson(it, groupListType) }
                    ?: emptyList(),
                title = title,
                createdBy = createdBy,
                createdOn = createdOn,
                lastUpdatedBy = lastUpdatedBy,
                lastUpdatedOn = lastUpdatedOn,
                version = version,
                oldVersions = emptyList(), // These information needs to received through another challenges
            )
        }
    }
}
