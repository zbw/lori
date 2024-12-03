package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.DashboardSearchFilter
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.RightError
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_RIGHT_ERROR
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Tracer
import java.sql.PreparedStatement
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

    suspend fun deleteErrorByTestId(testId: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_TEST_ID).apply {
                    this.setString(1, testId)
                }
            val span = tracer.spanBuilder("deleteRightErrorByTestId").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun deleteErrorsByAge(isOlderThan: Instant): Int =
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

    suspend fun deleteErrorsByType(conflictType: ConflictType): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_CONFLICT_TYPE).apply {
                    this.setString(1, conflictType.toString())
                }
            val span = tracer.spanBuilder("deleteByConflictType").startSpan()
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
        testId: String? = null,
    ): List<RightError> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(buildFilterQuery(filters, testId)).apply {
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, this)
                    }
                    if (testId != null) {
                        this.setString(counter++, testId)
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
                        conflictByContext = rs.getString(8),
                        testId = rs.getString(9),
                        createdBy = rs.getString(10),
                    )
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun getOccurrences(
        column: String,
        filters: List<DashboardSearchFilter> = emptyList(),
        testId: String?,
    ): List<String> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                // Input parameters values are not evaluated in buildOccurrenceQuery. Only counts number of ?
                connection.prepareStatement(buildOccurrenceQuery(column, filters, testId)).apply {
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, this)
                    }
                    if (testId != null) {
                        this.setString(counter++, testId)
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

    suspend fun getCount(
        filters: List<DashboardSearchFilter> = emptyList(),
        testId: String?,
    ): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(buildCountFilterQuery(filters, testId)).apply {
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, this)
                    }
                    if (testId != null) {
                        this.setString(counter++, testId)
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
                    .let {
                        insertRightErrorSetParameter(rightError, it)
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

    suspend fun insertErrorsBatch(errors: List<RightError>): List<Int> =
        connectionPool.useConnection { connection ->
            val prep = connection.prepareStatement(STATEMENT_INSERT_RIGHT_ERROR, Statement.RETURN_GENERATED_KEYS)
            errors.map {
                val p = insertRightErrorSetParameter(it, prep)
                p.addBatch()
            }
            val span = tracer.spanBuilder("insertErrorBatch").startSpan()
            try {
                span.makeCurrent()
                runInTransaction(connection) {
                    prep.executeBatch()
                }
                val rs: ResultSet = prep.generatedKeys
                return@useConnection generateSequence {
                    if (rs.next()) {
                        rs.getInt(1)
                    } else {
                        null
                    }
                }.takeWhile { true }.toList()
            } finally {
                span.end()
            }
        }

    companion object {
        private const val COLUMN_CONFLICTING_WITH = "conflicting_right_id"
        const val COLUMN_CONFLICTING_TYPE = "conflict_type"
        const val COLUMN_CREATED_ON = "created_on"
        const val COLUMN_CREATED_BY = "created_by"
        const val COLUMN_ERROR_ID = "error_id"
        private const val COLUMN_HANDLE_ID = "handle_id"
        private const val COLUMN_CONFLICT_BY_RIGHT_ID = "conflict_by_right_id"
        const val COLUMN_CONFLICT_BY_CONTEXT = "conflict_by_context"
        const val COLUMN_TEST_ID = "test_id"
        private const val COLUMN_MESSAGE = "message"

        const val STATEMENT_GET_RIGHT_LIST_SELECT =
            "SELECT" +
                " $COLUMN_ERROR_ID,$COLUMN_HANDLE_ID,$COLUMN_CONFLICT_BY_RIGHT_ID," +
                "$COLUMN_CONFLICTING_WITH,$COLUMN_MESSAGE,$COLUMN_CREATED_ON," +
                "$COLUMN_CONFLICTING_TYPE,$COLUMN_CONFLICT_BY_CONTEXT,$COLUMN_TEST_ID,$COLUMN_CREATED_BY" +
                " FROM $TABLE_NAME_RIGHT_ERROR"

        const val STATEMENT_INSERT_RIGHT_ERROR =
            "INSERT INTO $TABLE_NAME_RIGHT_ERROR" +
                "($COLUMN_HANDLE_ID,$COLUMN_CONFLICT_BY_RIGHT_ID,$COLUMN_CONFLICTING_WITH," +
                "$COLUMN_MESSAGE,$COLUMN_CREATED_ON,$COLUMN_CONFLICTING_TYPE," +
                "$COLUMN_CONFLICT_BY_CONTEXT,$COLUMN_TEST_ID,$COLUMN_CREATED_BY)" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?)"

        const val STATEMENT_DELETE_ERROR_BY_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_ERROR_ID = ?"

        const val STATEMENT_DELETE_ERROR_BY_TEST_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_TEST_ID = ?"

        const val STATEMENT_DELETE_ERROR_BY_AGE =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_CREATED_ON < ?"

        const val STATEMENT_DELETE_ERROR_BY_CONFLICT_TYPE =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_CONFLICTING_TYPE = ?"

        const val STATEMENT_DELETE_ERROR_BY_CAUSING_RIGHT_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_CONFLICT_BY_RIGHT_ID = ?"

        internal fun buildFilterQuery(
            filters: List<DashboardSearchFilter>,
            testId: String?,
        ): String {
            val whereClause = buildWhereClause(filters, testId)
            return STATEMENT_GET_RIGHT_LIST_SELECT +
                whereClause +
                " ORDER BY $COLUMN_ERROR_ID LIMIT ? OFFSET ?;"
        }

        private fun buildWhereClause(
            filters: List<DashboardSearchFilter>,
            testId: String?,
        ): String {
            val filterClause: String? =
                filters
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = " AND ") { f ->
                        f.toWhereClause()
                    }
            val testIdClause = testId?.let { "$COLUMN_TEST_ID = ?" } ?: "$COLUMN_TEST_ID IS NULL"
            return listOfNotNull(filterClause, testIdClause).joinToString(prefix = " WHERE ", separator = " AND ")
        }

        internal fun buildCountFilterQuery(
            filters: List<DashboardSearchFilter>,
            testId: String?,
        ): String {
            val whereClause = buildWhereClause(filters, testId)
            return "SELECT COUNT(*)" +
                " FROM $TABLE_NAME_RIGHT_ERROR" +
                whereClause
        }

        internal fun buildOccurrenceQuery(
            column: String,
            filters: List<DashboardSearchFilter>,
            testId: String?,
        ): String {
            val whereClause = buildWhereClause(filters, testId)
            return "SELECT $column" +
                " FROM $TABLE_NAME_RIGHT_ERROR" +
                whereClause +
                " GROUP BY $column;"
        }

        private fun insertRightErrorSetParameter(
            rightError: RightError,
            prep: PreparedStatement,
        ): PreparedStatement =
            prep.apply {
                this.setString(1, rightError.handle)
                this.setIfNotNull(2, rightError.conflictByRightId) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(3, rightError.conflictingWithRightId) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setString(4, rightError.message)
                this.setTimestamp(5, Timestamp.from(rightError.createdOn.toInstant()))
                this.setString(6, rightError.conflictType.toString())
                this.setIfNotNull(7, rightError.conflictByContext) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(8, rightError.testId) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(9, rightError.createdBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
            }
    }
}
