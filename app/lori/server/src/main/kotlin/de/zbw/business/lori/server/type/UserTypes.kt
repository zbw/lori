package de.zbw.business.lori.server.type

import java.time.Instant

/**
 * Types related to User information.
 *
 * Created on 08-17-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
enum class UserRole {
    READONLY,
    READWRITE,
    ADMIN,
}

data class Session(
    val sessionID: String?,
    val authenticated: Boolean,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole,
    val validUntil: Instant,
)
