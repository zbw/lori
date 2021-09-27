package de.zbw.persistence.auth.server

import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.UserRole
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    internal val connection: Connection,
) {
    constructor(
        config: AuthConfiguration,
    ) : this(DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword))

    fun usernameExists(
        name: String
    ): Boolean {
        val stmt = "SELECT EXISTS(SELECT 1 from $TABLE_NAME_USERS WHERE name=?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setString(1, name)
        }
        val rs = prepStmt.executeQuery()
        rs.next()
        return rs.getBoolean(1)
    }

    fun insertUser(
        name: String,
        password: String,
        email: String?,
    ): Int? {
        val statement =
            "INSERT INTO $TABLE_NAME_USERS" +
                "(name,password,email) " +
                "VALUES(?,?,?)"
        val prepStmt = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, name)
            this.setString(2, password)
            this.setIfNotNull(3, email) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
        return insertElement(prepStmt)
    }

    fun getRoleIdByName(
        role: UserRole.Role,
    ): Int? {
        val statement =
            "SELECT id FROM $TABLE_NAME_ROLES" +
                " WHERE name=?"

        val prepStmt: PreparedStatement = connection.prepareStatement(statement).apply {
            this.setString(1, role.toString())
        }
        val rs = prepStmt.executeQuery()
        return rs.next().takeIf { it }
            ?.let { rs.getInt(1) }
    }

    fun insertRole(
        role: UserRole.Role,
    ): Int? {
        val statement =
            "INSERT INTO $TABLE_NAME_ROLES" +
                "(name) " +
                "VALUES(?)"
        val prepStmt: PreparedStatement =
            connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS).apply {
                this.setString(1, role.toString())
            }
        return insertElement(prepStmt)
    }

    fun insertUserRole(
        userId: Int,
        roleId: Int,
    ): Int? {
        val statement =
            "INSERT INTO $TABLE_NAME_USERROLES" +
                "(role_id, user_id) " +
                "VALUES(?,?)"
        val prepStmt: PreparedStatement =
            connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS).apply {
                this.setInt(1, roleId)
                this.setInt(2, userId)
            }
        return insertElement(prepStmt)
    }

    fun getRolesByUserId(userId: Int): List<UserRole.Role> {
        val statement = "SELECT r.name " +
            "FROM $TABLE_NAME_ROLES r " +
            "LEFT JOIN $TABLE_NAME_USERROLES u ON u.role_id = r.id " +
            "WHERE u.user_id = ?"

        val prepStmt: PreparedStatement = connection.prepareStatement(statement).apply {
            this.setInt(1, userId)
        }
        val rs = prepStmt.executeQuery()
        return generateSequence {
            if (rs.next()) {
                UserRole.Role.valueOf(rs.getString(1))
            } else null
        }.takeWhile { true }.toList()
    }

    private fun insertElement(prepStmt: PreparedStatement): Int? {
        val affectedRows = prepStmt.run { this.executeUpdate() }
        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getInt(1)
        } else {
            null
        }
    }

    private fun <T> PreparedStatement.setIfNotNull(
        idx: Int,
        element: T?,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        const val TABLE_NAME_USERS = "users"
        const val TABLE_NAME_ROLES = "roles"
        const val TABLE_NAME_USERROLES = "user_roles"
    }
}
