package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.RightError
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_RIGHT_ERROR
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
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
    val connection: Connection,
    private val tracer: Tracer,
) {
    fun deleteErrorById(errorId: Int): Int {
        val prepStmt =
            connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_ID).apply {
                this.setInt(1, errorId)
            }
        val span = tracer.spanBuilder("deleteRightErrorById").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun deleteErrorByAge(isOlderThan: Instant): Int {
        val prepStmt =
            connection.prepareStatement(STATEMENT_DELETE_ERROR_BY_AGE).apply {
                this.setTimestamp(1, Timestamp.from(isOlderThan))
            }
        val span = tracer.spanBuilder("deleteRightErrorByAge").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun getErrorList(
        limit: Int,
        offset: Int,
    ): List<RightError> {
        val prepStmt =
            connection.prepareStatement(STATEMENT_GET_RIGHT_LIST).apply {
                this.setInt(1, limit)
                this.setInt(2, offset)
            }

        val span = tracer.spanBuilder("getRightErrorList").startSpan()
        val rs =
            try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.executeQuery() }
            } finally {
                span.end()
            }

        return generateSequence {
            if (rs.next()) {
                RightError(
                    errorId = rs.getInt(1),
                    metadataId = rs.getString(2),
                    handleId = rs.getString(3),
                    rightIdSource = rs.getString(4),
                    conflictingRightId = rs.getString(5),
                    message = rs.getString(6),
                    createdOn = rs.getTimestamp(7).toOffsetDateTime(),
                    conflictType = ConflictType.valueOf(rs.getString(8)),
                )
            } else {
                null
            }
        }.takeWhile { true }.toList()
    }

    fun insertError(rightError: RightError): Int {
        val prepStmt =
            connection
                .prepareStatement(STATEMENT_INSERT_RIGHT_ERROR, Statement.RETURN_GENERATED_KEYS)
                .apply {
                    this.setString(1, rightError.metadataId)
                    this.setString(2, rightError.handleId)
                    this.setString(3, rightError.rightIdSource)
                    this.setString(4, rightError.conflictingRightId)
                    this.setString(5, rightError.message)
                    this.setTimestamp(6, Timestamp.from(rightError.createdOn?.toInstant() ?: Instant.now()))
                    this.setString(7, rightError.conflictType.toString())
                }
        val span = tracer.spanBuilder("insertRightError").startSpan()
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

    companion object {
        private const val COLUMN_CONFLICTING_WITH = "conflicting_right_id"
        private const val COLUMN_CONFLICTING_TYPE = "conflict_type"
        private const val COLUMN_CREATED_ON = "created_on"
        private const val COLUMN_ERROR_ID = "error_id"
        private const val COLUMN_HANDLE_ID = "handle_id"
        private const val COLUMN_METADATA_ID = "metadata_id"
        private const val COLUMN_RIGHT_ID_SOURCE = "right_id_source"
        private const val COLUMN_MESSAGE = "message"

        const val STATEMENT_GET_RIGHT_LIST =
            "SELECT" +
                " $COLUMN_ERROR_ID,$COLUMN_METADATA_ID,$COLUMN_HANDLE_ID,$COLUMN_RIGHT_ID_SOURCE," +
                "$COLUMN_CONFLICTING_WITH,$COLUMN_MESSAGE,$COLUMN_CREATED_ON,$COLUMN_CONFLICTING_TYPE" +
                " FROM $TABLE_NAME_RIGHT_ERROR" +
                " ORDER BY $COLUMN_ERROR_ID LIMIT ? OFFSET ?;"

        const val STATEMENT_INSERT_RIGHT_ERROR =
            "INSERT INTO $TABLE_NAME_RIGHT_ERROR" +
                "($COLUMN_METADATA_ID,$COLUMN_HANDLE_ID,$COLUMN_RIGHT_ID_SOURCE," +
                "$COLUMN_CONFLICTING_WITH,$COLUMN_MESSAGE,$COLUMN_CREATED_ON," +
                "$COLUMN_CONFLICTING_TYPE)" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?)"

        const val STATEMENT_DELETE_ERROR_BY_ID =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_ERROR_ID = ?"

        const val STATEMENT_DELETE_ERROR_BY_AGE =
            "DELETE " +
                "FROM $TABLE_NAME_RIGHT_ERROR " +
                "WHERE $COLUMN_CREATED_ON < ?"
    }
}
