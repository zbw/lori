package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.User
import de.zbw.business.lori.server.type.UserRole
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_SESSIONS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_USERS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.util.UUID

/**
 * Execute SQL queries strongly related to user.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class UserDB(
    val connection: Connection,
    private val tracer: Tracer,
) {
    fun userTableContainsName(username: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_USER_CONTAINS_NAME).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("userContainsName").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun deleteSessionById(sessionID: String) {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_SESSION_BY_ID).apply {
            this.setString(1, sessionID)
        }
        val span = tracer.spanBuilder("deleteSessionById").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun insertSession(session: Session): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_SESSION, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, UUID.randomUUID().toString())
            this.setBoolean(2, session.authenticated)
            this.setIfNotNull(3, session.firstName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, session.lastName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(5, session.role.toString()) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setTimestamp(6, Timestamp.from(session.validUntil))
        }

        val span = tracer.spanBuilder("insertSession").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun insertUser(user: User): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_USER, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, user.name)
            this.setString(2, user.passwordHash)
            this.setIfNotNull(3, user.role.toString()) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }

        val span = tracer.spanBuilder("insertUser").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun userExistsByNameAndPassword(
        username: String,
        hashedPassword: String
    ): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_USER_CREDENTIALS_EXIST).apply {
            this.setString(1, username)
            this.setString(2, hashedPassword)
        }
        val span = tracer.spanBuilder("userExistByNameAndPassword").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getRoleByUsername(username: String): UserRole? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_ROLE_BY_USER).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("getRoleByUser").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return if (rs.next()) {
            UserRole.valueOf(rs.getString(1))
        } else null
    }

    fun updateUserNonRoleProperties(user: User): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_UPDATE_USER).apply {
            this.setString(1, user.passwordHash)
            this.setString(2, user.name)
        }
        val span = tracer.spanBuilder("updateUserNonRoleProperties").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun getSessionById(sessionId: String): Session? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_SESSION_BY_ID).apply {
            this.setString(1, sessionId)
        }
        val span = tracer.spanBuilder("getSessionById").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return if (rs.next()) {
            Session(
                sessionID = rs.getString(1),
                authenticated = rs.getBoolean(2),
                firstName = rs.getString(3),
                lastName = rs.getString(4),
                role = UserRole.valueOf(
                    rs.getString(5),
                ),
                validUntil = rs.getTimestamp(6).toInstant(),
            )
        } else null
    }

    fun getUserByName(username: String): User? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_USER_BY_NAME).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("getUserByName").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return if (rs.next()) {
            User(
                name = rs.getString(1),
                passwordHash = rs.getString(2),
                role = UserRole.valueOf(
                    rs.getString(3),
                ),
            )
        } else null
    }

    fun deleteUser(username: String): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_USER).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("deleteUser").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun updateUserRoleProperty(username: String, role: UserRole): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_UPDATE_USER_ROLE).apply {
            this.setString(1, role.toString())
            this.setString(2, username)
        }
        val span = tracer.spanBuilder("updateUserRole").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    companion object {
        const val STATEMENT_INSERT_SESSION = "INSERT INTO $TABLE_NAME_SESSIONS" +
            "(session_id,authenticated,first_name," +
            "last_name,role,valid_until) " +
            "VALUES(?,?,?," +
            "?,?::role_enum,?)"
        const val STATEMENT_INSERT_USER = "INSERT INTO $TABLE_NAME_USERS" +
            "(username, password, role) " +
            "VALUES(?,?,?::role_enum)"

        const val STATEMENT_USER_CONTAINS_NAME =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_USERS WHERE username=?)"

        const val STATEMENT_USER_CREDENTIALS_EXIST =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_USERS WHERE username=? AND password=?)"

        const val STATEMENT_GET_ROLE_BY_USER =
            "SELECT role FROM $TABLE_NAME_USERS WHERE username=?"

        const val STATEMENT_UPDATE_USER =
            "UPDATE $TABLE_NAME_USERS SET password=? WHERE username=?"

        const val STATEMENT_UPDATE_USER_ROLE =
            "UPDATE $TABLE_NAME_USERS SET role=?::role_enum WHERE username=?"

        const val STATEMENT_GET_USER_BY_NAME =
            "SELECT username, password, role " +
                "FROM $TABLE_NAME_USERS " +
                "WHERE username=?"

        const val STATEMENT_GET_SESSION_BY_ID =
            "SELECT session_id,authenticated,first_name," +
                "last_name,role,valid_until " +
                "FROM $TABLE_NAME_SESSIONS " +
                "WHERE session_id=?"

        const val STATEMENT_DELETE_USER = "DELETE " +
            "FROM $TABLE_NAME_USERS i " +
            "WHERE i.username = ?"

        const val STATEMENT_DELETE_SESSION_BY_ID = "DELETE " +
            "FROM $TABLE_NAME_SESSIONS i " +
            "WHERE i.session_id = ?"
    }
}
