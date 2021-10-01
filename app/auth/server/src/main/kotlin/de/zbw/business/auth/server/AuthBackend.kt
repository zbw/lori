package de.zbw.business.auth.server

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.SignInUserData
import de.zbw.auth.model.SignUpUserData
import de.zbw.auth.model.UserRole
import de.zbw.persistence.auth.server.DatabaseConnector
import de.zbw.persistence.auth.server.transient.UserTableEntry

/**
 * Backend implementation of REST-API.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthBackend(
    private val dbConnector: DatabaseConnector,
) {

    constructor(config: AuthConfiguration) : this(
        DatabaseConnector(config),
    )
    init {
        UserRole.Role.values().forEach { role ->
            val roleId = dbConnector.getRoleIdByName(role)
            if (roleId == null) {
                dbConnector.insertRole(role)
            }
        }
    }

    fun isUsernameAvailable(
        name: String
    ) = !dbConnector.usernameExists(name)

    fun registerNewUser(
        userData: SignUpUserData,
    ): Int? {
        val userId = dbConnector.insertUser(
            userData.name,
            hashPassword(userData.password),
            userData.email,
        )
        return userId
            ?.let { dbConnector.getRoleIdByName(UserRole.Role.userRead) }
            ?.let { roleId ->
                dbConnector.insertUserRole(userId, roleId)
            }
    }

    fun getUserEntry(signInUserData: SignInUserData): UserTableEntry? = dbConnector.getUserByName(signInUserData.name)

    fun getUserRolesById(userId: Int): List<UserRole.Role> = dbConnector.getRolesByUserId(userId)

    companion object {
        private const val ARGON2_ITERATIONS: Int = 10
        private const val ARGON2_MEMORY: Int = 65536 // 2^16
        private const val ARGON2_PARALLELISM: Int = 1

        private val argon: Argon2 = Argon2Factory.create()

        fun hashPassword(password: String): String =
            argon.hash(ARGON2_ITERATIONS, ARGON2_MEMORY, ARGON2_PARALLELISM, password.toCharArray())

        fun verifyPassword(password: String, hash: String) =
            argon.verify(hash, password.toCharArray())
    }
}
