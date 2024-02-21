package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.type.UserPermission
import io.ktor.server.auth.Principal

data class UserSession(
    val email: String,
    val sessionId: String,
    val permissions: List<UserPermission>,
) : Principal
