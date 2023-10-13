package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.UserRole
import de.zbw.lori.model.AuthTokenRest
import de.zbw.lori.model.RoleRest
import de.zbw.lori.model.UserRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes for users.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.usersRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/users") {
        post("/register") {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/users/register")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val user: UserRest =
                        call.receive(UserRest::class)
                            .takeIf { it.username != null && it.password != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("name", user.username)
                    if (backend.userContainsName(user.username)) {
                        span.setStatus(StatusCode.ERROR, "Conflict: An user with this name already exists.")
                        call.respond(
                            HttpStatusCode.Conflict,
                            ApiError.conflictError(
                                "Username existiert bereits."
                            ),
                        )
                    } else {
                        backend.insertNewUser(user)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError(ApiError.INVALID_JSON)
                    )
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                } finally {
                    span.end()
                }
            }
        }
        post("/login") {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/users/login")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val user: UserRest =
                        call.receive(UserRest::class)
                            .takeIf { it.username != null && it.password != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("name", user.username)
                    val isValidUser = backend.checkCredentials(user)
                    if (isValidUser) {
                        call.respond(AuthTokenRest(backend.generateJWT(user.username)))
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiError.unauthorizedError("Credentials sind ungültig.")
                        )
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                } finally {
                    span.end()
                }
            }
        }

        authenticate("auth-jwt") {
            put("{id}") {
                val span = tracer
                    .spanBuilder("lori.LoriService.PUT/api/v1/users/{id}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val username = call.parameters["id"]
                            ?: throw BadRequestException("User parameter is invalid")

                        @Suppress("SENSELESS_COMPARISON")
                        val user: UserRest =
                            call.receive(UserRest::class)
                                .takeIf { it.username != null && it.password != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        if (user.username != username) {
                            throw BadRequestException("Username in URL is different to username in JSON")
                        }
                        span.setAttribute("username", username)
                        val principal = call.principal<JWTPrincipal>()
                        val usernameToken = principal!!.payload.getClaim("username").asString()
                        if (LoriServerBackend.isJWTExpired(principal)) {
                            call.respond(HttpStatusCode.Unauthorized, ApiError.unauthorizedError(ApiError.EXPIRED_JWT))
                            return@withContext
                        }
                        val isAuthorized =
                            username == usernameToken || backend.getCurrentUserRole(usernameToken) == UserRole.ADMIN
                        if (!isAuthorized) {
                            call.respond(HttpStatusCode.Unauthorized, ApiError.unauthorizedError())
                            return@withContext
                        }
                        backend.updateUserNonRoleProperties(user)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(ApiError.INVALID_JSON)
                        )
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }
            delete("{id}") {
                val span = tracer
                    .spanBuilder("lori.LoriService.DELETE/api/v1/users/{id}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val username = call.parameters["id"]
                            ?: throw BadRequestException("User parameter is invalid")

                        span.setAttribute("username", username)
                        val principal = call.principal<JWTPrincipal>()
                        val usernameToken = principal!!.payload.getClaim("username").asString()
                        if (LoriServerBackend.isJWTExpired(principal)) {
                            call.respond(HttpStatusCode.Unauthorized, ApiError.unauthorizedError(ApiError.EXPIRED_JWT))
                            return@withContext
                        }
                        val isAuthorized =
                            username == usernameToken || backend.getCurrentUserRole(usernameToken) == UserRole.ADMIN
                        if (!isAuthorized) {
                            call.respond(HttpStatusCode.Unauthorized, ApiError.unauthorizedError())
                            return@withContext
                        }
                        backend.deleteUser(username)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError("User Id fehlt."))
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }
            put("/admin/role") {
                val span = tracer
                    .spanBuilder("lori.LoriService.PUT/api/v1/users/admin/role")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val principal = call.principal<JWTPrincipal>()
                        val usernameToken = principal!!.payload.getClaim("username").asString()
                        if (LoriServerBackend.isJWTExpired(principal)) {
                            call.respond(HttpStatusCode.Unauthorized, ApiError.unauthorizedError(ApiError.EXPIRED_JWT))
                            return@withContext
                        }
                        if (backend.getCurrentUserRole(usernameToken) != UserRole.ADMIN) {
                            call.respond(HttpStatusCode.Unauthorized, ApiError.unauthorizedError())
                            return@withContext
                        }
                        @Suppress("SENSELESS_COMPARISON")
                        val roleRest: RoleRest =
                            call.receive(RoleRest::class)
                                .takeIf { it.username != null && it.role != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        span.setAttribute("username", roleRest.username)
                        span.setAttribute("role", roleRest.role.toString())
                        backend.updateUserRoleProperty(roleRest.username, roleRest.role.toBusiness())
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError(ApiError.INVALID_JSON))
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }
        }
    }
}
