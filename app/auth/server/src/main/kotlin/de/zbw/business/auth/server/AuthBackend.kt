package de.zbw.business.auth.server

import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Factory
import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.SignUp
import de.zbw.persistence.auth.server.DatabaseConnector
import org.apache.commons.lang3.CharSet

/**
 * Backend implementation of REST-API.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthBackend (
    private val dbConnector: DatabaseConnector,
    private val argon: Argon2 = Argon2Factory.create(),
    ) {

    constructor(
        config: AuthConfiguration,
    ) : this(
        DatabaseConnector(config),
    )

    fun registerNewUser(
        userData: SignUp,
    ): String {
        // TODO: Check if Username already exists
        return dbConnector.insertUser(
            userData.name,
            hashPassword(userData.password),
            userData.email,
        )
    }

    fun hashPassword(password: String): String = argon.hash(ARGON2_ITERATIONS, ARGON2_MEMORY, ARGON2_PARALLELISM, password.toCharArray())

    companion object {
        const val ARGON2_ITERATIONS: Int = 10
        const val ARGON2_MEMORY: Int = 65536 // 2^16
        const val ARGON2_PARALLELISM: Int = 1
    }
}
