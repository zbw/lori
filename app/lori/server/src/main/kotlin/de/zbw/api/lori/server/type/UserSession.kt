package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.type.UserRole
import io.ktor.server.auth.Principal

data class UserSession(
    val email: String,
    val sessionId: String,
    val role: UserRole,
) : Principal
