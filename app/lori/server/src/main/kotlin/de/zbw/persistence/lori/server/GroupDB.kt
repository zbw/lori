package de.zbw.persistence.lori.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.GroupVersion
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Tracer
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import kotlin.collections.map

/**
 * Execute SQL queries strongly related to groups.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GroupDB(
    connectionPool: ConnectionPool,
    batchConnectionPool: ConnectionPool,
    tracer: Tracer,
    private val gson: Gson,
) : AbstractDB(connectionPool, batchConnectionPool, tracer, TABLE_NAME_RIGHT_GROUP) {
    suspend fun insertGroup(
        group: Group,
        useGivenId: Boolean = false,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql =
                    if (useGivenId) {
                        STATEMENT_INSERT_GROUP_WITH_ID
                    } else {
                        STATEMENT_INSERT_GROUP
                    },
                tracer = tracer,
                spanName = "insertGroup",
                params = { stmt ->
                    val now = Instant.now()
                    stmt.setIfNotNull(1, group.description) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    val jsonObj = PGobject()
                    jsonObj.type = "json"
                    jsonObj.value = gson.toJson(group.entries)
                    stmt.setObject(2, jsonObj)
                    stmt.setString(3, group.title)
                    stmt.setIfNotNull(4, group.createdBy) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    stmt.setTimestamp(5, Timestamp.from(now), utcCalendar)
                    stmt.setIfNotNull(6, group.createdBy) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    stmt.setTimestamp(7, Timestamp.from(now), utcCalendar)
                    stmt.setInt(8, group.version)
                    if (useGivenId) {
                        stmt.setInt(9, group.groupId)
                    }
                },
                fetchGenerated = { rs ->
                    rs.getInt(1)
                },
            ).first()

    suspend fun getLatestVersionGroupsByIds(
        groupIds: List<Int>,
        isBatchJob: Boolean = false,
    ): List<Group> =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_GROUPS_BY_IDS,
                tracer = tracer,
                spanName = "getLatestVersionGroupsByIds",
                params = { stmt ->
                    stmt.setArray(1, stmt.connection.createArrayOf("integer", groupIds.toTypedArray()))
                },
                mapper = { rs ->
                    extractGroupRS(rs, gson)
                },
            ).groupBy { it.groupId }
            .mapValues { e: Map.Entry<Int, List<Group>> ->
                e.value.maxByOrNull { g -> g.version }
            }.values
            .filterNotNull()

    suspend fun getGroupById(
        groupId: Int,
        isBatchJob: Boolean = false,
    ): Group? =
        DatabaseConnector
            .select(
                sql = STATEMENT_GET_GROUP_BY_ID_WITH_LATEST_VERSION,
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                tracer = tracer,
                spanName = "getGroupById",
                params = { stmt ->
                    stmt.setInt(1, groupId)
                },
                mapper = { rs ->
                    extractGroupRS(rs, gson)
                },
            ).firstOrNull()
            ?.let {
                it.copy(
                    oldVersions =
                        getAllGroupVersionsById(it.groupId)
                            .takeIf { it.size > 1 }
                            ?.let { it.sortedByDescending { g -> g.version } }
                            ?.drop(1)
                            ?: emptyList(),
                )
            }

    suspend fun getAllGroupVersionsById(
        groupId: Int,
        isBatchJob: Boolean = false,
    ): List<GroupVersion> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_GET_GROUPS_BY_ID,
            tracer = tracer,
            spanName = "getAllGroupVersionsById",
            params = { stmt ->
                stmt.setInt(1, groupId)
            },
            mapper = { rs ->
                var localCounter = 1
                GroupVersion(
                    groupId = rs.getInt(localCounter++),
                    createdBy = rs.getString(localCounter++),
                    createdOn = rs.getTimestamp(localCounter++, utcCalendar)?.toOffsetDateTime(),
                    description = rs.getString(localCounter++),
                    version = rs.getInt(localCounter++),
                    title = rs.getString(localCounter++),
                )
            },
        )

    suspend fun getGroupByIdAndVersion(
        groupId: Int,
        version: Int,
        isBatchJob: Boolean = false,
    ): Group? =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_GROUP_BY_ID_AND_VERSION,
                tracer = tracer,
                spanName = "getGroupByIdAndVersion",
                params = { stmt ->
                    stmt.setInt(1, groupId)
                    stmt.setInt(2, version)
                },
                mapper = { rs ->
                    extractGroupRS(rs, gson)
                },
            ).firstOrNull()
            ?.let {
                it.copy(
                    oldVersions =
                        getAllGroupVersionsById(it.groupId)
                            .takeIf { it.size > 1 }
                            ?.let { it.sortedByDescending { g -> g.version } }
                            ?.drop(1)
                            ?: emptyList(),
                )
            }

    suspend fun deleteGroupPair(
        groupId: Int,
        rightId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_GROUP_RIGHT_PAIR,
            tracer = tracer,
            spanName = "deleteGroupPair",
            params = { stmt ->
                stmt.setInt(1, groupId)
                stmt.setString(2, rightId)
            },
        )

    suspend fun deleteGroupPairsByRightId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_GROUP_RIGHT_PAIR_BY_RIGHT_ID,
            tracer = tracer,
            spanName = "deleteGroupPairsByRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
        )

    /**
     * Get the ids of all rights that use a given group-id.
     */
    suspend fun getRightsByGroupId(
        groupId: Int,
        isBatchJob: Boolean = false,
    ): List<String> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_GET_RIGHTS_BY_GROUP_ID,
            tracer = tracer,
            spanName = "getRightsByGroupId",
            params = { stmt ->
                stmt.setInt(1, groupId)
            },
            mapper = { rs ->
                rs.getString(1)
            },
        )

    suspend fun getGroupsByRightId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): List<Group> =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_GROUPS_BY_RIGHT_ID,
                tracer = tracer,
                spanName = "getGroupsByRightId",
                params = { stmt ->
                    stmt.setString(1, rightId)
                },
                mapper = { rs ->
                    rs.getInt(1)
                },
            ).let {
                getLatestVersionGroupsByIds(it)
            }

    suspend fun getGroupsByRightIds(
        rightIds: List<String>,
        isBatchJob: Boolean = false,
    ): Map<String, List<Group>> {
        val rightIdToGroupIds: Map<String, List<Int>> =
            DatabaseConnector
                .select(
                    connectionPool =
                        if (isBatchJob) {
                            batchConnectionPool
                        } else {
                            connectionPool
                        },
                    sql = STATEMENT_GET_GROUPS_BY_RIGHT_IDS,
                    tracer = tracer,
                    spanName = "getGroupsByRightIds",
                    params = { stmt ->
                        stmt.setArray(1, stmt.connection.createArrayOf("text", rightIds.toTypedArray()))
                    },
                    mapper = { rs ->
                        val groupId = rs.getInt(1)
                        val rightId = rs.getString(2)
                        rightId to groupId
                    },
                ).fold(initial = mutableMapOf<String, List<Int>>()) { acc, p ->
                    acc.merge(p.first, listOf(p.second)) { oldValue, newValue ->
                        oldValue + newValue
                    }
                    acc
                }

        val allGroupIds =
            rightIdToGroupIds.values.fold(mutableSetOf<Int>()) { acc, l ->
                acc.addAll(l)
                acc
            }
        val allGroups =
            getLatestVersionGroupsByIds(allGroupIds.toList()).map {
                it.groupId to it
            }
        return rightIdToGroupIds.entries.associate { entry ->
            entry.key to entry.value.mapNotNull { allGroups.find { pair -> pair.first == it }?.second }
        }
    }

    suspend fun insertGroupRightPair(
        rightId: String,
        groupId: Int,
        createdBy: String,
        isBatchJob: Boolean = false,
    ): String =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_INSERT_GROUP_RIGHT_PAIR,
                tracer = tracer,
                spanName = "insertGroupRightPair",
                params = { stmt ->
                    val now = Instant.now()
                    stmt.setInt(1, groupId)
                    stmt.setString(2, rightId)
                    stmt.setString(3, createdBy)
                    stmt.setTimestamp(4, Timestamp.from(now), utcCalendar)
                    stmt.setString(5, createdBy)
                    stmt.setTimestamp(6, Timestamp.from(now), utcCalendar)
                },
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
            ).first()

    suspend fun getGroupList(
        limit: Int,
        offset: Int,
        isBatchJob: Boolean = false,
    ): List<Group> =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_GROUP_LIST,
                tracer = tracer,
                spanName = "getGroupList",
                params = { stmt ->
                    stmt.setInt(1, limit)
                    stmt.setInt(2, offset)
                },
                mapper = { rs ->
                    extractGroupRS(rs, gson)
                },
            ).let { groups ->
                groups.map {
                    it.copy(
                        oldVersions =
                            getAllGroupVersionsById(it.groupId)
                                .takeIf { it.size > 1 }
                                ?.let { it.sortedByDescending { g -> g.version } }
                                ?.drop(1)
                                ?: emptyList(),
                    )
                }
            }

    suspend fun deleteGroupById(
        groupId: Int,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_GROUP_BY_ID,
            tracer = tracer,
            spanName = "deleteGroupById",
            params = { stmt ->
                stmt.setInt(1, groupId)
            },
        )

    suspend fun updateGroup(
        group: Group,
        updateBy: String,
        isBatchJob: Boolean = false,
    ): Int {
        // 1. Get existing group
        val existingGroup: Group = getGroupById(group.groupId, isBatchJob) ?: return 0
        // 2. Check if differences exist
        return if (group.entries == existingGroup.entries &&
            group.title == existingGroup.title &&
            group.description == existingGroup.description
        ) {
            // No changes
            0
        } else {
            insertGroup(
                group.copy(
                    version = existingGroup.version + 1,
                    lastUpdatedBy = updateBy,
                    createdBy = updateBy,
                ),
                true,
                isBatchJob,
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

        val utcCalendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone(TimezoneUtil.TIME_ZONE_UTC))

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
                "$COLUMN_DESCRIPTION,$COLUMN_VERSION,$COLUMN_TITLE" +
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
                " ($COLUMN_GROUP_ID, $COLUMN_RIGHT_ID,$COLUMN_CREATED_BY," +
                "$COLUMN_CREATED_ON,$COLUMN_LAST_UPDATED_BY,$COLUMN_LAST_UPDATED_ON)" +
                " VALUES(?,?,?," +
                "?,?,?);"

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

        const val STATEMENT_GET_GROUPS_BY_RIGHT_IDS =
            "SELECT $COLUMN_GROUP_ID,$COLUMN_RIGHT_ID" +
                " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                " WHERE $COLUMN_RIGHT_ID = ANY(?);"

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
            var localCounter = 1
            val groupListType: Type = object : TypeToken<ArrayList<GroupEntry>>() {}.type
            val groupId = rs.getInt(localCounter++)
            val description = rs.getString(localCounter++)
            val ipAddressJson: String? =
                rs
                    .getObject(localCounter++, PGobject::class.java)
                    .value
            val title = rs.getString(localCounter++)
            val createdBy = rs.getString(localCounter++)
            val createdOn = rs.getTimestamp(localCounter++)?.toOffsetDateTime()
            val lastUpdatedBy = rs.getString(localCounter++)
            val lastUpdatedOn = rs.getTimestamp(localCounter++, utcCalendar)?.toOffsetDateTime()
            val version = rs.getInt(localCounter++)

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
                // This information need to be queried separately
                oldVersions = emptyList(),
            )
        }
    }
}
