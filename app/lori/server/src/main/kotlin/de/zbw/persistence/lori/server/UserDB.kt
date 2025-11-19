package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserPermission
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_SESSIONS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * Execute SQL queries strongly related to a user.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class UserDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    suspend fun deleteSessionById(sessionID: String): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_DELETE_SESSION_BY_ID,
            tracer = tracer,
            spanName = "deleteSessionById",
            params = { stmt ->
                stmt.setString(1, sessionID)
            },
        )

    suspend fun insertSession(session: Session): String =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool = connectionPool,
                sql = STATEMENT_INSERT_SESSION,
                tracer = tracer,
                spanName = "insertSession",
                params = { stmt ->
                    val now = Instant.now()
                    stmt.setString(1, UUID.randomUUID().toString())
                    stmt.setBoolean(2, session.authenticated)
                    stmt.setIfNotNull(3, session.firstName) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    stmt.setIfNotNull(4, session.lastName) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    stmt.setIfNotNull(5, session.permissions) { value, idx, prepStmt ->
                        prepStmt.setArray(idx, stmt.connection.createArrayOf("permission_enum", value.toTypedArray()))
                    }
                    stmt.setTimestamp(6, Timestamp.from(session.validUntil), utcCalendar)
                    stmt.setTimestamp(7, Timestamp.from(now), utcCalendar)
                },
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
            ).first()

    suspend fun getSessionById(sessionId: String): Session? =
        DatabaseConnector
            .select(
                connectionPool = connectionPool,
                sql = STATEMENT_GET_SESSION_BY_ID,
                tracer = tracer,
                spanName = "getSessionById",
                params = { stmt ->
                    stmt.setString(1, sessionId)
                },
                mapper = { rs ->
                    Session(
                        sessionID = rs.getString(1),
                        authenticated = rs.getBoolean(2),
                        firstName = rs.getString(3),
                        lastName = rs.getString(4),
                        permissions =
                            (rs.getArray(5)?.array as? Array<out Any?>)
                                ?.filterIsInstance<String>()
                                ?.map { UserPermission.valueOf(it) }
                                ?: emptyList(),
                        validUntil = rs.getTimestamp(6, utcCalendar).toInstant(),
                        createdOn = rs.getTimestamp(7, utcCalendar).toInstant(),
                    )
                },
            ).firstOrNull()

    companion object {
        val utcCalendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone(TimezoneUtil.TIME_ZONE_UTC))

        const val STATEMENT_INSERT_SESSION =
            "INSERT INTO $TABLE_NAME_SESSIONS" +
                "(session_id,authenticated,first_name," +
                "last_name,permissions,valid_until, created_on)" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?)"

        const val STATEMENT_GET_SESSION_BY_ID =
            "SELECT session_id,authenticated,first_name," +
                "last_name,permissions,valid_until,created_on" +
                " FROM $TABLE_NAME_SESSIONS" +
                " WHERE session_id=?"

        const val STATEMENT_DELETE_SESSION_BY_ID =
            "DELETE" +
                " FROM $TABLE_NAME_SESSIONS i" +
                " WHERE i.session_id = ?"
    }
}
