package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.ExportJobStatus
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_EXPORT_JOBS
import de.zbw.persistence.lori.server.UserDB.Companion.utcCalendar
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * SQL queries regarding export jobs.
 *
 * Created on 09-10-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ExportJobDB(
    connectionPool: ConnectionPool,
    batchConnectionPool: ConnectionPool,
    tracer: Tracer,
) : AbstractDB(connectionPool, batchConnectionPool, tracer, TABLE_NAME_EXPORT_JOBS) {
    suspend fun insertJob(
        exportJob: ExportJob,
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
                tracer = tracer,
                spanName = "insertJob",
                sql = STATEMENT_INSERT_EXPORT_JOB,
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
                params = { stmt ->
                    val now = Instant.now()
                    stmt.setString(1, exportJob.id.toString())
                    stmt.setString(2, exportJob.status.toString())
                    stmt.setTimestamp(3, Timestamp.from(now), utcCalendar)
                    stmt.setString(4, exportJob.createdBy)
                    stmt.setTimestamp(5, Timestamp.from(now), utcCalendar)
                    stmt.setString(6, exportJob.filePath)
                    stmt.setString(7, exportJob.searchTerm)
                    stmt.setString(8, exportJob.format.toString())
                },
            ).first()

    suspend fun getJobById(
        id: UUID,
        isBatchJob: Boolean = false,
    ): ExportJob? =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                tracer = tracer,
                spanName = "getExportJobById",
                sql = STATEMENT_GET_EXPORT_JOB_BY_ID,
                mapper = { rs ->
                    ExportJob(
                        id = UUID.fromString(rs.getString(1)),
                        status = ExportJobStatus.valueOf(rs.getString(2)),
                        createdOn =
                            rs.getTimestamp(3, BookmarkDB.utcCalendar).toInstant(),
                        createdBy = rs.getString(4),
                        lastUpdatedOn =
                            rs.getTimestamp(5, BookmarkDB.utcCalendar).toInstant(),
                        errorMessage = rs.getString(6),
                        filePath = rs.getString(7),
                        searchTerm = rs.getString(8),
                        format = ExportFormat.valueOf(rs.getString(9)),
                    )
                },
                params = { stmt ->
                    stmt.setString(1, id.toString())
                },
            ).firstOrNull()

    suspend fun getJobsOlderThan(
        instant: Instant,
        isBatchJob: Boolean = false,
    ): List<ExportJob> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            tracer = tracer,
            spanName = "getJobsOlderThan",
            sql = STATEMENT_GET_ALL_IDS,
            params = { stmt ->
                stmt.setTimestamp(1, Timestamp.from(instant))
            },
            mapper = { rs: ResultSet ->
                ExportJob(
                    id = UUID.fromString(rs.getString(1)),
                    status = ExportJobStatus.valueOf(rs.getString(2)),
                    createdOn =
                        rs.getTimestamp(3, BookmarkDB.utcCalendar).toInstant(),
                    createdBy = rs.getString(4),
                    lastUpdatedOn =
                        rs.getTimestamp(5, BookmarkDB.utcCalendar).toInstant(),
                    errorMessage = rs.getString(6),
                    filePath = rs.getString(7),
                    searchTerm = rs.getString(8),
                    format = ExportFormat.valueOf(rs.getString(9)),
                )
            },
        )

    suspend fun deleteJobsByIds(
        ids: List<UUID>,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            sql = STATEMENT_DELETE_EXPORT_JOBS_BY_IDS,
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            tracer = tracer,
            spanName = "deleteExportJobsByIds",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", ids.map { it.toString() }.toTypedArray()))
            },
        )

    suspend fun updateJobStatusById(
        exportJob: ExportJob,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_UPDATE_EXPORT_JOB,
            tracer = tracer,
            spanName = "updateExportJobStatusById",
            params = { stmt ->
                val now = Instant.now()
                stmt.setTimestamp(1, Timestamp.from(now), RightDB.utcCalendar) // last_applied_on
                stmt.setString(2, exportJob.status.toString())
                stmt.setString(3, exportJob.errorMessage)
                stmt.setString(4, exportJob.filePath)
                stmt.setString(5, exportJob.id.toString())
            },
        )

    companion object {
        const val COLUMN_EXPORT_JOB_ID = "id"
        const val COLUMN_EXPORT_JOB_STATUS = "status"
        const val COLUMN_EXPORT_JOB_CREATED_ON = "created_on"
        const val COLUMN_EXPORT_JOB_CREATED_BY = "created_by"
        const val COLUMN_EXPORT_JOB_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_EXPORT_JOB_ERROR_MESSAGE = "error_message"
        const val COLUMN_EXPORT_JOB_FILE_PATH = "file_path"
        const val COLUMN_EXPORT_JOB_SEARCH_TERM = "search_term"
        const val COLUMN_EXPORT_JOB_EXPORT_FORMAT = "export_format"

        const val STATEMENT_INSERT_EXPORT_JOB =
            "INSERT INTO $TABLE_NAME_EXPORT_JOBS" +
                " ($COLUMN_EXPORT_JOB_ID,$COLUMN_EXPORT_JOB_STATUS,$COLUMN_EXPORT_JOB_CREATED_ON," +
                "$COLUMN_EXPORT_JOB_CREATED_BY,$COLUMN_EXPORT_JOB_LAST_UPDATED_ON,$COLUMN_EXPORT_JOB_FILE_PATH," +
                "$COLUMN_EXPORT_JOB_SEARCH_TERM,$COLUMN_EXPORT_JOB_EXPORT_FORMAT)" +
                " VALUES (?,?,?," +
                "?,?,?," +
                "?,?);"

        const val STATEMENT_GET_EXPORT_JOB_BY_ID =
            "SELECT $COLUMN_EXPORT_JOB_ID,$COLUMN_EXPORT_JOB_STATUS,$COLUMN_EXPORT_JOB_CREATED_ON," +
                "$COLUMN_EXPORT_JOB_CREATED_BY,$COLUMN_EXPORT_JOB_LAST_UPDATED_ON,$COLUMN_EXPORT_JOB_ERROR_MESSAGE," +
                "$COLUMN_EXPORT_JOB_FILE_PATH,$COLUMN_EXPORT_JOB_SEARCH_TERM,$COLUMN_EXPORT_JOB_EXPORT_FORMAT" +
                " FROM $TABLE_NAME_EXPORT_JOBS" +
                " WHERE $COLUMN_EXPORT_JOB_ID=?;"

        const val STATEMENT_UPDATE_EXPORT_JOB =
            "UPDATE $TABLE_NAME_EXPORT_JOBS" +
                " SET $COLUMN_EXPORT_JOB_LAST_UPDATED_ON=?," +
                " $COLUMN_EXPORT_JOB_STATUS=?," +
                " $COLUMN_EXPORT_JOB_ERROR_MESSAGE=?," +
                " $COLUMN_EXPORT_JOB_FILE_PATH=?" +
                " WHERE $COLUMN_EXPORT_JOB_ID=?;"

        const val STATEMENT_DELETE_EXPORT_JOBS_BY_IDS =
            "DELETE " +
                "FROM $TABLE_NAME_EXPORT_JOBS r " +
                "WHERE r.$COLUMN_EXPORT_JOB_ID = ANY(?)"

        const val STATEMENT_GET_ALL_IDS =
            "SELECT $COLUMN_EXPORT_JOB_ID,$COLUMN_EXPORT_JOB_STATUS,$COLUMN_EXPORT_JOB_CREATED_ON," +
                "$COLUMN_EXPORT_JOB_CREATED_BY,$COLUMN_EXPORT_JOB_LAST_UPDATED_ON,$COLUMN_EXPORT_JOB_ERROR_MESSAGE," +
                "$COLUMN_EXPORT_JOB_FILE_PATH,$COLUMN_EXPORT_JOB_SEARCH_TERM,$COLUMN_EXPORT_JOB_EXPORT_FORMAT" +
                " FROM $TABLE_NAME_EXPORT_JOBS" +
                " WHERE $COLUMN_EXPORT_JOB_LAST_UPDATED_ON < ?"
    }
}
