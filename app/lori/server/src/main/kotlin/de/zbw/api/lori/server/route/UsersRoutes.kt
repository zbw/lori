package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.AuthTokenRest
import de.zbw.lori.model.UserRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
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
                .spanBuilder("lori.LoriService.POST/api/v1/users")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val user: UserRest =
                        call.receive(UserRest::class)
                            .takeIf { it.name != null && it.password != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("name", user.name)
                    if (backend.userContainsName(user.name)) {
                        span.setStatus(StatusCode.ERROR, "Conflict: An user with this name already exists.")
                        call.respond(HttpStatusCode.Conflict, "Username already exists.")
                    } else {
                        backend.insertNewUser(user)
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError)
                } finally {
                    span.end()
                }
            }
        }
        post("/login") {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/users")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val user: UserRest =
                        call.receive(UserRest::class)
                            .takeIf { it.name != null && it.password != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    val isValidUser = backend.checkCredentials(user)
                    if (isValidUser) {
                        call.respond(AuthTokenRest(backend.generateJWT(user.name)))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError)
                } finally {
                    span.end()
                }
            }
        }
    }
}
