package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.ExportJobStatus
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_JOBS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.UserDB.Companion.utcCalendar
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * SQL queries regarding jobs.
 *
 * Created on 09-10-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class JobDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    suspend fun insertJob(exportJob: ExportJob): String =
        connectionPool.useConnection("createJob") { connection ->
            val span = tracer.spanBuilder("createJob").startSpan()
            val now = Instant.now()
            val prepStmt =
                connection.prepareStatement(STATEMENT_INSERT_JOB, Statement.RETURN_GENERATED_KEYS).apply {
                    this.setString(1, exportJob.id.toString())
                    this.setString(2, exportJob.status.toString())
                    this.setTimestamp(3, Timestamp.from(now), utcCalendar)
                    this.setString(4, exportJob.createdBy)
                    this.setTimestamp(5, Timestamp.from(now), utcCalendar)
                    this.setString(6, exportJob.filePath)
                    this.setString(7, exportJob.searchTerm)
                    this.setString(8, exportJob.format.toString())
                }
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

    suspend fun getJobById(id: UUID): ExportJob? =
        connectionPool.useConnection("getJobById") { connection ->
            val span = tracer.spanBuilder("getJobById").startSpan()
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_JOB_BY_ID).apply {
                    this.setString(1, id.toString())
                }
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            return@useConnection if (rs.next()) {
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
            } else {
                null
            }
        }

    suspend fun getJobsOlderThan(instant: Instant): List<ExportJob> =
        connectionPool.useConnection("getAllJobIds") { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_ALL_IDS).apply {
                    this.setTimestamp(1, Timestamp.from(instant))
                }
            val span = tracer.spanBuilder("getAllJobIds").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            return@useConnection generateSequence {
                if (rs.next()) {
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
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun deleteJobsByIds(ids: List<UUID>): Int =
        connectionPool.useConnection("deleteJobsByIds") { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_JOBS_BY_IDS).apply {
                    this.setArray(1, connection.createArrayOf("text", ids.map { it.toString() }.toTypedArray()))
                }
            val span = tracer.spanBuilder("deleteJobsByIds").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun updateJobStatusById(exportJob: ExportJob): Int =
        connectionPool.useConnection("updateJobStatusById") { connection ->
            val now = Instant.now()
            val prepStmt =
                connection.prepareStatement(STATEMENT_UPDATE_JOB).apply {
                    this.setTimestamp(1, Timestamp.from(now), RightDB.utcCalendar) // last_applied_on
                    this.setString(2, exportJob.status.toString())
                    this.setString(3, exportJob.errorMessage)
                    this.setString(4, exportJob.filePath)
                    this.setString(5, exportJob.id.toString())
                }
            val span = tracer.spanBuilder("updateJobStatusById").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    companion object {
        const val COLUMN_JOB_ID = "id"
        const val COLUMN_JOB_STATUS = "status"
        const val COLUMN_JOB_CREATED_ON = "created_on"
        const val COLUMN_JOB_CREATED_BY = "created_by"
        const val COLUMN_JOB_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_JOB_ERROR_MESSAGE = "error_message"
        const val COLUMN_JOB_FILE_PATH = "file_path"
        const val COLUMN_JOB_SEARCH_TERM = "search_term"
        const val COLUMN_JOB_EXPORT_FORMAT = "export_format"

        const val STATEMENT_INSERT_JOB =
            "INSERT INTO $TABLE_NAME_JOBS" +
                " ($COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_FILE_PATH," +
                "$COLUMN_JOB_SEARCH_TERM,$COLUMN_JOB_EXPORT_FORMAT)" +
                " VALUES (?,?,?," +
                "?,?,?," +
                "?,?);"

        const val STATEMENT_GET_JOB_BY_ID =
            "SELECT $COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_ERROR_MESSAGE," +
                "$COLUMN_JOB_FILE_PATH,$COLUMN_JOB_SEARCH_TERM,$COLUMN_JOB_EXPORT_FORMAT" +
                " FROM $TABLE_NAME_JOBS" +
                " WHERE $COLUMN_JOB_ID=?;"

        const val STATEMENT_UPDATE_JOB =
            "UPDATE $TABLE_NAME_JOBS" +
                " SET $COLUMN_JOB_LAST_UPDATED_ON=?," +
                " $COLUMN_JOB_STATUS=?," +
                " $COLUMN_JOB_ERROR_MESSAGE=?," +
                " $COLUMN_JOB_FILE_PATH=?" +
                " WHERE $COLUMN_JOB_ID=?;"

        const val STATEMENT_DELETE_JOBS_BY_IDS =
            "DELETE " +
                "FROM $TABLE_NAME_JOBS r " +
                "WHERE r.$COLUMN_JOB_ID = ANY(?)"

        const val STATEMENT_GET_ALL_IDS =
            "SELECT $COLUMN_JOB_ID,$COLUMN_JOB_STATUS,$COLUMN_JOB_CREATED_ON," +
                "$COLUMN_JOB_CREATED_BY,$COLUMN_JOB_LAST_UPDATED_ON,$COLUMN_JOB_ERROR_MESSAGE," +
                "$COLUMN_JOB_FILE_PATH,$COLUMN_JOB_SEARCH_TERM,$COLUMN_JOB_EXPORT_FORMAT" +
                " FROM $TABLE_NAME_JOBS" +
                " WHERE $COLUMN_JOB_LAST_UPDATED_ON < ?"
    }
}
