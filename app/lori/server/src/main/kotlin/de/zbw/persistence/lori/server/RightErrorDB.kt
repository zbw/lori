package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.DashboardSearchFilter
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.RightError
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_RIGHT_ERROR
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant

/**
 * Execute SQL queries strongly related to [RightError].
 *
 * Created on 01-17-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightErrorDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    suspend fun deleteErrorById(errorId: Int): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_ID).apply {
                    this.setInt(1, errorId)
                }
            val span = tracer.spanBuilder("deleteRightErrorById").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun deleteErrorByAge(isOlderThan: Instant): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_AGE).apply {
                    this.setTimestamp(1, Timestamp.from(isOlderThan))
                }
            val span = tracer.spanBuilder("deleteRightErrorByAge").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun deleteByCausingRightId(rightId: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_CAUSING_RIGHT_ID).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("deleteByCausingRightId").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun getErrorList(
        limit: Int,
        offset: Int,
        filters: List<DashboardSearchFilter> = emptyList(),
    ): List<RightError> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(buildFilterQuery(filters)).apply {
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, this)
                    }
                    this.setInt(counter++, limit)
                    this.setInt(counter++, offset)
                }

            val span = tracer.spanBuilder("getRightErrorList").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            return@useConnection generateSequence {
                if (rs.next()) {
                    RightError(
                        errorId = rs.getInt(1),
                        handle = rs.getString(2),
                        conflictByRightId = rs.getString(3),
                        conflictingWithRightId = rs.getString(4),
                        message = rs.getString(5),
                        createdOn = rs.getTimestamp(6).toOffsetDateTime(),
                        conflictType = ConflictType.valueOf(rs.getString(7)),
                        conflictByTemplateName = rs.getString(8),
                    )
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun getOccurrences(
        column: String,
        filters: List<DashboardSearchFilter> = emptyList(),
    ): List<String> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(buildOccurrenceQuery(column, filters)).apply {
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, this)
                    }
                }

            val span = tracer.spanBuilder("getOccurrences").startSpan()
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

    suspend fun getCount(filters: List<DashboardSearchFilter> = emptyList()): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(buildCountFilterQuery(filters)).apply {
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, this)
                    }
                }

            val span = tracer.spanBuilder("getRightErrorCount").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            return@useConnection if (rs.next()) {
                rs.getInt(1)
            } else {
                0
            }
        }

    suspend fun insertError(rightError: RightError): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection
                    .prepareStatement(STATEMENT_INSERT_RIGHT_ERROR, Statement.RETURN_GENERATED_KEYS)
                    .apply {
                        this.setString(1, rightError.handle)
                        this.setString(2, rightError.conflictByRightId)
                        this.setString(3, rightError.conflictingWithRightId)
                        this.setString(4, rightError.message)
                        this.setTimestamp(5, Timestamp.from(rightError.createdOn.toInstant()))
                        this.setString(6, rightError.conflictType.toString())
                        this.setIfNotNull(7, rightError.conflictByTemplateName) { value, idx, prepStmt ->
                            prepStmt.setString(idx, value)
                        }
                    }
            val span = tracer.spanBuilder("insertRightError").startSpan()
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

    companion object {
        private const val COLUMN_CONFLICTING_WITH = "conflicting_right_id"
        const val COLUMN_CONFLICTING_TYPE = "conflict_type"
        const val COLUMN_CREATED_ON = "created_on"
        private const val COLUMN_ERROR_ID = "error_id"
        private const val COLUMN_HANDLE_ID = "handle_id"
        private const val COLUMN_CONFLICT_BY_RIGHT_ID = "conflict_by_right_id"
        const val COLUMN_CONFLICT_BY_TEMPLATE_NAME = "conflict_by_template_name"
        private const val COLUMN_MESSAGE = "message"

        const val STATEMENT_GET_RIGHT_LIST_SELECT =
            "SELECT" +
                " $COLUMN_ERROR_ID,$COLUMN_HANDLE_ID,$COLUMN_CONFLICT_BY_RIGHT_ID," +
                "$COLUMN_CONFLICTING_WITH,$COLUMN_MESSAGE,$COLUMN_CREATED_ON," +
                "$COLUMN_CONFLICTING_TYPE,$COLUMN_CONFLICT_BY_TEMPLATE_NAME" +
                " FROM $TABLE_NAME_RIGHT_ERROR"

        const val STATEMENT_INSERT_RIGHT_ERROR =
            "INSERT INTO $TABLE_NAME_RIGHT_ERROR" +
                "($COLUMN_HANDLE_ID,$COLUMN_CONFLICT_BY_RIGHT_ID," +
                "$COLUMN_CONFLICTING_WITH,$COLUMN_MESSAGE,$COLUMN_CREATED_ON," +
                "$COLUMN_CONFLICTING_TYPE,$COLUMN_CONFLICT_BY_TEMPLATE_NAME)" +
                " VALUES(?,?," +
                "?,?,?," +
                "?,?)"

        const val STATEMENT_DELETE_ERROR_BY_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_ERROR_ID = ?"

        const val STATEMENT_DELETE_ERROR_BY_AGE =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_CREATED_ON < ?"

        const val STATEMENT_DELETE_ERROR_BY_CAUSING_RIGHT_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_CONFLICT_BY_RIGHT_ID = ?"

        internal fun buildFilterQuery(filters: List<DashboardSearchFilter>): String {
            val whereClause: String =
                filters
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = " WHERE ", separator = " AND ") { f ->
                        f.toWhereClause()
                    }
                    ?: ""

            return STATEMENT_GET_RIGHT_LIST_SELECT +
                whereClause +
                " ORDER BY $COLUMN_ERROR_ID LIMIT ? OFFSET ?;"
        }

        internal fun buildCountFilterQuery(filters: List<DashboardSearchFilter>): String {
            val whereClause: String =
                filters
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = " WHERE ", separator = " AND ") { f ->
                        f.toWhereClause()
                    }
                    ?: ""

            return "SELECT COUNT(*)" +
                " FROM $TABLE_NAME_RIGHT_ERROR" +
                whereClause
        }

        internal fun buildOccurrenceQuery(
            column: String,
            filters: List<DashboardSearchFilter>,
        ): String {
            val whereClause: String =
                filters
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(prefix = " WHERE ", separator = " AND ") { f ->
                        f.toWhereClause()
                    }
                    ?: ""

            return "SELECT $column" +
                " FROM $TABLE_NAME_RIGHT_ERROR" +
                whereClause +
                " GROUP BY $column;"
        }
    }
}
