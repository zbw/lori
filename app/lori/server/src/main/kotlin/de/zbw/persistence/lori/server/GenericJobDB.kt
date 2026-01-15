package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.GenericJob
import de.zbw.business.lori.server.type.JobKind
import de.zbw.business.lori.server.type.JobStatus
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_JOBS
import de.zbw.persistence.lori.server.UserDB.Companion.utcCalendar
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * SQL queries regarding jobs.
 *
 * Created on 09-22-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GenericJobDB(
    connectionPool: ConnectionPool,
    tracer: Tracer,
) : AbstractDB(connectionPool, tracer, tableName = TABLE_NAME_JOBS) {
    suspend fun insertJob(genericJob: GenericJob): String =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool = connectionPool,
                sql = STATEMENT_INSERT_JOB,
                tracer = tracer,
                spanName = "insertJob",
                params = { stmt ->
                    val now = Instant.now()
                    stmt.setString(1, genericJob.id.toString())
                    stmt.setString(2, genericJob.status.toString())
                    stmt.setTimestamp(3, Timestamp.from(now), utcCalendar)
                    stmt.setString(4, genericJob.createdBy)
                    stmt.setTimestamp(5, Timestamp.from(now), utcCalendar)
                    stmt.setString(6, genericJob.errorMessage)
                    stmt.setString(7, genericJob.kind.toString())
                    stmt.setString(8, genericJob.summary)
                },
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
            ).first()

    suspend fun getJobById(id: UUID): GenericJob? =
        DatabaseConnector
            .select(
                sql = STATEMENT_GET_JOB_BY_ID,
                connectionPool = connectionPool,
                tracer = tracer,
                spanName = "getJobById",
                params = { stmt ->
                    stmt.setString(1, id.toString())
                },
                mapper = { rs ->
                    GenericJob(
                        id = UUID.fromString(rs.getString(1)),
                        status = JobStatus.valueOf(rs.getString(2)),
                        createdOn =
                            rs.getTimestamp(3, BookmarkDB.utcCalendar).toInstant(),
                        createdBy = rs.getString(4),
                        lastUpdatedOn =
                            rs.getTimestamp(5, BookmarkDB.utcCalendar).toInstant(),
                        errorMessage = rs.getString(6),
                        kind = JobKind.valueOf(rs.getString(7)),
                        summary = rs.getString(8),
                    )
                },
            ).firstOrNull()

    suspend fun getJobsOlderThan(instant: Instant): List<GenericJob> =
        DatabaseConnector.select(
            connectionPool = connectionPool,
            sql = STATEMENT_GET_ALL_IDS_OLDER_THAN,
            tracer = tracer,
            spanName = "getJobsOlderThan",
            params = { stmt ->
                stmt.setTimestamp(1, Timestamp.from(instant))
            },
            mapper = { rs: ResultSet ->
                GenericJob(
                    id = UUID.fromString(rs.getString(1)),
                    status = JobStatus.valueOf(rs.getString(2)),
                    createdOn =
                        rs.getTimestamp(3, BookmarkDB.utcCalendar).toInstant(),
                    createdBy = rs.getString(4),
                    lastUpdatedOn =
                        rs.getTimestamp(5, BookmarkDB.utcCalendar).toInstant(),
                    errorMessage = rs.getString(6),
                    kind = JobKind.valueOf(rs.getString(7)),
                    summary = rs.getString(8),
                )
            },
        )

    suspend fun deleteJobsByIds(ids: List<UUID>): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_DELETE_JOBS_BY_IDS,
            tracer = tracer,
            spanName = "deleteJobsByIds",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", ids.map { it.toString() }.toTypedArray()))
            },
        )

    suspend fun updateJobStatusById(exportGenericJob: GenericJob): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_UPDATE_JOB,
            tracer = tracer,
            spanName = "updateJobStatusById",
            params = { stmt ->
                val now = Instant.now()
                stmt.setTimestamp(1, Timestamp.from(now), RightDB.utcCalendar) // last_applied_on
                stmt.setString(2, exportGenericJob.status.toString())
                stmt.setString(3, exportGenericJob.errorMessage)
                stmt.setString(4, exportGenericJob.summary)
                stmt.setString(5, exportGenericJob.id.toString())
            },
        )

    suspend fun getSuccessfulJobsSince(instant: Instant): List<GenericJob> =
        DatabaseConnector.select(
            connectionPool = connectionPool,
            sql = STATEMENT_GET_SUCCESSFUL_JOBS_SINCE,
            tracer = tracer,
            spanName = "getSuccessfulJobsSince",
            params = { stmt ->
                stmt.setTimestamp(1, Timestamp.from(instant))
            },
            mapper = { rs ->
                GenericJob(
                    id = UUID.fromString(rs.getString(1)),
                    status = JobStatus.valueOf(rs.getString(2)),
                    createdOn =
                        rs.getTimestamp(3, BookmarkDB.utcCalendar).toInstant(),
                    createdBy = rs.getString(4),
                    lastUpdatedOn =
                        rs.getTimestamp(5, BookmarkDB.utcCalendar).toInstant(),
                    errorMessage = rs.getString(6),
                    kind = JobKind.valueOf(rs.getString(7)),
                    summary = rs.getString(8),
                )
            },
        )

    companion object {
        const val COLUMN_JOB_ID = "id"
        const val COLUMN_JOB_STATUS = "status"
        const val COLUMN_JOB_CREATED_ON = "created_on"
        const val COLUMN_JOB_CREATED_BY = "created_by"
        const val COLUMN_JOB_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_JOB_ERROR_MESSAGE = "error_message"
        const val COLUMN_JOB_SUMMARY = "summary"
        const val COLUMN_JOB_KIND = "kind"

        const val STATEMENT_INSERT_JOB =
            "INSERT INTO $TABLE_NAME_JOBS" +
                " ($COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_ERROR_MESSAGE," +
                "$COLUMN_JOB_KIND,$COLUMN_JOB_SUMMARY)" +
                " VALUES (?,?,?," +
                "?,?,?," +
                "?,?);"

        const val STATEMENT_GET_JOB_BY_ID =
            "SELECT $COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_ERROR_MESSAGE," +
                "$COLUMN_JOB_KIND,$COLUMN_JOB_SUMMARY" +
                " FROM $TABLE_NAME_JOBS" +
                " WHERE $COLUMN_JOB_ID=?;"

        const val STATEMENT_UPDATE_JOB =
            "UPDATE $TABLE_NAME_JOBS" +
                " SET $COLUMN_JOB_LAST_UPDATED_ON=?," +
                " $COLUMN_JOB_STATUS=?," +
                " $COLUMN_JOB_ERROR_MESSAGE=?," +
                " $COLUMN_JOB_SUMMARY=?" +
                " WHERE $COLUMN_JOB_ID=?;"

        const val STATEMENT_DELETE_JOBS_BY_IDS =
            "DELETE " +
                "FROM $TABLE_NAME_JOBS r " +
                "WHERE r.$COLUMN_JOB_ID = ANY(?)"

        const val STATEMENT_GET_ALL_IDS_OLDER_THAN =
            "SELECT $COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_ERROR_MESSAGE," +
                "$COLUMN_JOB_KIND,$COLUMN_JOB_SUMMARY" +
                " FROM $TABLE_NAME_JOBS" +
                " WHERE $COLUMN_JOB_LAST_UPDATED_ON < ?"

        val STATEMENT_GET_SUCCESSFUL_JOBS_SINCE =
            "SELECT $COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_ERROR_MESSAGE," +
                "$COLUMN_JOB_KIND,$COLUMN_JOB_SUMMARY" +
                " FROM $TABLE_NAME_JOBS" +
                " WHERE $COLUMN_JOB_LAST_UPDATED_ON > ? AND $COLUMN_JOB_STATUS = '${JobStatus.SUCCESSFUL}';"
    }
}
