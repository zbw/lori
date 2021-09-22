package de.zbw.persistence.auth.server

import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.SignUp
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 07-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    internal val connection: Connection,
) {
    constructor(
        config: AuthConfiguration,
    ) : this(DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword))

    fun insertUser(
        name: String,
        password: String,
        email: String,
    ): String {
        val statement =
            "INSERT INTO $TABLE_NAME_USERS" +
                "(name,password,id) " +
                "VALUES(?,?,?)"
        val prepStmt = connection.prepareStatement(statement, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, name)
            this.setString(2, password)
            this.setString(3, email)
        }

        val affectedRows = prepStmt.run { this.executeUpdate() }

        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getString(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    private fun <T> PreparedStatement.setIfNotNull(
        element: T?,
        idx: Int,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        const val TABLE_NAME_USERS = "users"
    }
}
