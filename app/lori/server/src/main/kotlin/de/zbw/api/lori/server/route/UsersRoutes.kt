package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.UserSession
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
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
        route("/sessions") {
            authenticate("auth-login") {
                get {
                    val span = tracer
                        .spanBuilder("lori.LoriService.GET/api/v1/sessions")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val userSession: UserSession? = call.principal<UserSession>()
                            if (userSession != null) {
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK, userSession.toRest())
                            } else {
                                call.respond(
                                    HttpStatusCode.Unauthorized,
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
                delete {
                    val span = tracer
                        .spanBuilder("lori.LoriService.DELETE/api/v1/sessions")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val userSession: UserSession? = call.principal<UserSession>()
                            if (userSession == null) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiError.badRequestError("No cookie does exist"),
                                )
                            } else {
                                backend.deleteSessionById(userSession.sessionId)
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK, userSession.toRest())
                            }
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
}
