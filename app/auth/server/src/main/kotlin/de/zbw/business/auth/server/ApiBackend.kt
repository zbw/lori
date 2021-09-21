package de.zbw.business.auth.server

/**
 * Backend implementation of REST-API.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object ApiBackend {
    fun getUserInformation(userId: Int): UserInformation {
        return UserInformation(EXAMPLE_USER, userId.toLong())
    }

    const val EXAMPLE_USER = "Max Mustermann"
}
