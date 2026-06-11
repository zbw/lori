package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.DashboardSearchFilter
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_RIGHT_ERROR
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Tracer
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone

/**
 * Execute SQL queries strongly related to [RightError].
 *
 * Created on 01-17-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightErrorDB(
    connectionPool: ConnectionPool,
    batchConnectionPool: ConnectionPool,
    tracer: Tracer,
) : AbstractDB(connectionPool, batchConnectionPool, tracer, TABLE_NAME_RIGHT_ERROR) {
    suspend fun deleteErrorById(
        errorId: Int,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_ERROR_BY_ID,
            tracer = tracer,
            spanName = "deleteErrorById",
            params = { stmt ->
                stmt.setInt(1, errorId)
            },
        )

    suspend fun deleteErrorByTestId(
        testId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_ERROR_BY_TEST_ID,
            tracer = tracer,
            spanName = "deleteErrorByTestId",
            params = { stmt ->
                stmt.setString(1, testId)
            },
        )

    suspend fun deleteErrorsByAge(
        isOlderThan: Instant,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_ERROR_BY_AGE,
            tracer = tracer,
            spanName = "deleteRightErrorByAge",
            params = { stmt ->
                stmt.setTimestamp(1, Timestamp.from(isOlderThan), utcCalendar)
            },
        )

    suspend fun deleteErrorsByType(
        conflictType: ConflictType,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_ERROR_BY_CONFLICT_TYPE,
            tracer = tracer,
            spanName = "deleteByConflictType",
            params = { stmt ->
                stmt.setString(1, conflictType.toString())
            },
        )

    suspend fun deleteByCausingRightId(
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
            sql = STATEMENT_DELETE_ERROR_BY_CAUSING_RIGHT_ID,
            tracer = tracer,
            spanName = "deleteByCausingRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
        )

    suspend fun getErrorList(
        limit: Int,
        offset: Int,
        filters: List<DashboardSearchFilter> = emptyList(),
        testId: String? = null,
        isBatchJob: Boolean = false,
    ): List<RightError> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = buildFilterQuery(filters, testId),
            tracer = tracer,
            spanName = "getRightErrorList",
            params = { stmt ->
                var counter = 1
                filters.forEach { f ->
                    counter = f.setSQLParameter(counter, stmt)
                }
                if (testId != null) {
                    stmt.setString(counter++, testId)
                }
                stmt.setInt(counter++, limit)
                stmt.setInt(counter++, offset)
            },
            mapper = { rs ->
                RightError(
                    errorId = rs.getInt(1),
                    handle = rs.getString(2),
                    conflictCausedByRightId = rs.getString(3),
                    conflictWithExistingRightId = rs.getString(4),
                    message = rs.getString(5),
                    createdOn = rs.getTimestamp(6, utcCalendar).toOffsetDateTime(),
                    conflictType = ConflictType.valueOf(rs.getString(7)),
                    conflictCausedInContext = rs.getString(8),
                    testId = rs.getString(9),
                    createdBy = rs.getString(10),
                )
            },
        )

    suspend fun getOccurrences(
        column: String,
        filters: List<DashboardSearchFilter> = emptyList(),
        testId: String?,
        isBatchJob: Boolean = false,
    ): List<String> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = buildOccurrenceQuery(column, filters, testId),
            tracer = tracer,
            spanName = "getOccurrences",
            params = { stmt ->
                var counter = 1
                filters.forEach { f ->
                    counter = f.setSQLParameter(counter, stmt)
                }
                if (testId != null) {
                    stmt.setString(counter++, testId)
                }
            },
            mapper = { rs ->
                rs.getString(1)
            },
        )

    suspend fun getCount(
        filters: List<DashboardSearchFilter> = emptyList(),
        testId: String?,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = buildCountFilterQuery(filters, testId),
                tracer = tracer,
                spanName = "getRightErrorCount",
                params = { stmt ->
                    var counter = 1
                    filters.forEach { f ->
                        counter = f.setSQLParameter(counter, stmt)
                    }
                    if (testId != null) {
                        stmt.setString(counter++, testId)
                    }
                },
                mapper = { rs ->
                    rs.getInt(1)
                },
            ).first()

    suspend fun insertError(
        rightError: RightError,
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
                sql = STATEMENT_INSERT_RIGHT_ERROR,
                tracer = tracer,
                spanName = "insertRightError",
                fetchGenerated = { rs ->
                    rs.getInt(1)
                },
                params = { stmt ->
                    insertRightErrorSetParameter(rightError, stmt)
                },
            ).first()

    suspend fun insertErrorsBatch(
        errors: List<RightError>,
        isBatchJob: Boolean = false,
    ): List<Int> =
        DatabaseConnector.insertBatchReturningKeys(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_INSERT_RIGHT_ERROR,
            tracer = tracer,
            spanName = "insertErrorBatch",
            params = { stmt ->
                errors.map {
                    val p = insertRightErrorSetParameter(it, stmt)
                    p.addBatch()
                }
            },
            fetchGenerated = { rs ->
                rs.getInt(1)
            },
        )

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

        val utcCalendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone(TimezoneUtil.TIME_ZONE_UTC))

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
                this.setIfNotNull(2, rightError.conflictCausedByRightId) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(3, rightError.conflictWithExistingRightId) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setString(4, rightError.message)
                this.setTimestamp(5, Timestamp.from(rightError.createdOn.toInstant()), utcCalendar)
                this.setString(6, rightError.conflictType.toString())
                this.setIfNotNull(7, rightError.conflictCausedInContext) { value, idx, prepStmt ->
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
