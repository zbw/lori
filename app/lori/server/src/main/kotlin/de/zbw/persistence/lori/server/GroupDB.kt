package de.zbw.persistence.lori.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * Execute SQL queries strongly related to groups.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GroupDB(
    val connection: Connection,
    private val tracer: Tracer,
    private val gson: Gson,
) {
    fun insertGroup(group: Group): Int {
        val prepStmt: PreparedStatement =
            connection
                .prepareStatement(STATEMENT_INSERT_GROUP, Statement.RETURN_GENERATED_KEYS)
                .apply {
                    this.setIfNotNull(1, group.description) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    val jsonObj = PGobject()
                    jsonObj.type = "json"
                    jsonObj.value = gson.toJson(group.entries)
                    this.setObject(2, jsonObj)
                    this.setString(3, group.title)
                }
        val span = tracer.spanBuilder("insertGroup").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
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

    fun getGroupsByIds(groupIds: List<Int>): List<Group> {
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

        return generateSequence {
            if (rs.next()) {
                extractGroupRS(rs, gson)
            } else {
                null
            }
        }.takeWhile { true }.toList()
    }

    fun getGroupById(groupId: Int): Group? {
        val prepStmt =
            connection.prepareStatement(STATEMENT_GET_GROUP_BY_ID).apply {
                this.setInt(1, groupId)
            }

        val span = tracer.spanBuilder("getGroupById").startSpan()
        val rs =
            try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.executeQuery() }
            } finally {
                span.end()
            }

        return if (rs.next()) {
            extractGroupRS(rs, gson)
        } else {
            null
        }
    }

    fun deleteGroupPair(
        groupId: Int,
        rightId: String,
    ): Int {
        val prepStmt =
            connection.prepareStatement(STATEMENT_DELETE_GROUP_RIGHT_PAIR).apply {
                this.setInt(1, groupId)
                this.setString(2, rightId)
            }
        val span = tracer.spanBuilder("deleteGroupPair").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun deleteGroupPairsByRightId(rightId: String): Int {
        val prepStmt =
            connection.prepareStatement(STATEMENT_DELETE_GROUP_RIGHT_PAIR_BY_RIGHT_ID).apply {
                this.setString(1, rightId)
            }
        val span = tracer.spanBuilder("deleteGroupPairsByRightId").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    /**
     * Get the ids of all rights that use a given group-id.
     */
    fun getRightsByGroupId(groupId: Int): List<String> {
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

        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else {
                null
            }
        }.takeWhile { true }.toList()
    }

    fun getGroupsByRightId(rightId: String): List<Group> {
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

        return getGroupsByIds(groupIds)
    }

    fun insertGroupRightPair(
        rightId: String,
        groupId: Int,
    ): String {
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
            return if (affectedRows > 0) {
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

    fun getGroupList(
        limit: Int,
        offset: Int,
    ): List<Group> {
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

        return generateSequence {
            if (rs.next()) {
                extractGroupRS(rs, gson)
            } else {
                null
            }
        }.takeWhile { true }.toList()
    }

    fun getGroupListIdsOnly(
        limit: Int,
        offset: Int,
    ): List<Int> {
        val prepStmt =
            connection.prepareStatement(STATEMENT_GET_GROUP_LIST_ID_ONLY).apply {
                this.setInt(1, limit)
                this.setInt(2, offset)
            }

        val span = tracer.spanBuilder("getGroupListIdOnly").startSpan()
        val rs =
            try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.executeQuery() }
            } finally {
                span.end()
            }

        return generateSequence {
            if (rs.next()) {
                rs.getInt(1)
            } else {
                null
            }
        }.takeWhile { true }.toList()
    }

    fun deleteGroupById(groupId: Int): Int {
        val prepStmt =
            connection.prepareStatement(STATEMENT_DELETE_GROUP_BY_ID).apply {
                this.setInt(1, groupId)
            }
        val span = tracer.spanBuilder("deleteGroup").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun updateGroup(group: Group): Int {
        val prepStmt =
            connection.prepareStatement(STATEMENT_UPDATE_GROUP).apply {
                this.setIfNotNull(1, group.description) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                val jsonObj = PGobject()
                jsonObj.type = "json"
                jsonObj.value = gson.toJson(group.entries)
                this.setObject(2, jsonObj)
                this.setString(3, group.title)
                this.setInt(4, group.groupId)
            }
        val span = tracer.spanBuilder("updateGroup").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    companion object {
        private const val TABLE_NAME_GROUP_RIGHT_MAP = "group_right_map"
        private const val TABLE_NAME_RIGHT_GROUP = "right_group"
        const val STATEMENT_INSERT_GROUP =
            "INSERT INTO $TABLE_NAME_RIGHT_GROUP" +
                " (description, ip_addresses, title)" +
                " VALUES(?,?,?)"

        const val STATEMENT_GET_GROUP_BY_ID =
            "SELECT group_id, description, ip_addresses, title" +
                " FROM $TABLE_NAME_RIGHT_GROUP" +
                " WHERE group_id = ?"

        const val STATEMENT_GET_GROUP_LIST =
            "SELECT group_id, description, ip_addresses, title" +
                " FROM $TABLE_NAME_RIGHT_GROUP" +
                " ORDER BY group_id LIMIT ? OFFSET ?;"

        const val STATEMENT_GET_GROUP_LIST_ID_ONLY =
            "SELECT group_id" +
                " FROM $TABLE_NAME_RIGHT_GROUP" +
                " ORDER BY group_id LIMIT ? OFFSET ?;"

        const val STATEMENT_INSERT_GROUP_RIGHT_PAIR =
            "INSERT INTO $TABLE_NAME_GROUP_RIGHT_MAP" +
                " (group_id, right_id)" +
                " VALUES(?,?)"

        const val STATEMENT_DELETE_GROUP_RIGHT_PAIR =
            "DELETE" +
                " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                " WHERE group_id = ? AND" +
                " right_id = ?"

        const val STATEMENT_DELETE_GROUP_RIGHT_PAIR_BY_RIGHT_ID =
            "DELETE" +
                " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                " WHERE right_id = ?"

        const val STATEMENT_DELETE_GROUP_BY_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_GROUP" +
                " WHERE group_id = ?"

        const val STATEMENT_GET_RIGHTS_BY_GROUP_ID =
            "SELECT right_id" +
                " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                " WHERE group_id = ?"

        const val STATEMENT_GET_GROUPS_BY_RIGHT_ID =
            "SELECT group_id" +
                " FROM $TABLE_NAME_GROUP_RIGHT_MAP" +
                " WHERE right_id = ?"

        const val STATEMENT_GET_GROUPS_BY_IDS =
            "SELECT group_id, description, ip_addresses, title" +
                " FROM $TABLE_NAME_RIGHT_GROUP" +
                " WHERE group_id = ANY(?)"

        const val STATEMENT_UPDATE_GROUP =
            "UPDATE $TABLE_NAME_RIGHT_GROUP SET description=?, ip_addresses=?, title=? WHERE group_id=?;"

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
            return Group(
                groupId = groupId,
                description = description,
                entries =
                    ipAddressJson
                        ?.let { gson.fromJson(it, groupListType) }
                        ?: emptyList(),
                title = title,
            )
        }
    }
}
