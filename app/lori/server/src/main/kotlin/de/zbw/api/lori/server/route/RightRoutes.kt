package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.UserSession
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.lori.model.RightIdCreated
import de.zbw.lori.model.RightRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes for rights.
 *
 * Created on 04-01-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.rightRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/right") {
        get("{id}") {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/right/{id}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val rightId = call.parameters["id"]
                    span.setAttribute("rightId", rightId ?: "null")
                    if (rightId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                detail = ApiError.NO_VALID_ID,
                            ),
                        )
                    } else {
                        val right: ItemRight? = backend.getRightsByIds(listOf(rightId)).firstOrNull()
                        right?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(it.toRest())
                        } ?: let {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError.notFoundError(ApiError.NO_RESOURCE_FOR_ID),
                            )
                        }
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(),
                    )
                } finally {
                    span.end()
                }
            }
        }

        authenticate("auth-login") {
            post {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.POST/api/v1/right")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        @Suppress("SENSELESS_COMPARISON")
                        val right: RightRest =
                            call
                                .receive(RightRest::class)
                                .takeIf { it.startDate != null && it.accessState != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        span.setAttribute("right", right.toString())
                        val userSession: UserSession =
                            call.principal<UserSession>()
                                ?: return@withContext call.respond(
                                    HttpStatusCode.Unauthorized,
                                    ApiError.unauthorizedError("User is not authorized"),
                                ) // This should never happen
                        if (right.endDate != null && right.endDate!! <= right.startDate) {
                            return@withContext call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError("Enddatum muss nach dem Startdatum liegen."),
                            )
                        }
                        val pk =
                            backend.insertRight(
                                right.toBusiness().copy(
                                    createdBy = userSession.email,
                                    lastUpdatedBy = userSession.email,
                                ),
                            )
                        span.setStatus(StatusCode.OK)
                        call.respond(RightIdCreated(pk))
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                detail = ApiError.INVALID_JSON,
                            ),
                        )
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(),
                        )
                    } finally {
                        span.end()
                    }
                }
            }

            put {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.PUT/api/v1/right")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        @Suppress("SENSELESS_COMPARISON")
                        val right: RightRest =
                            call
                                .receive(RightRest::class)
                                .takeIf { it.rightId != null && it.startDate != null && it.accessState != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        span.setAttribute("right", right.toString())
                        val userSession: UserSession =
                            call.principal<UserSession>()
                                ?: return@withContext call.respond(
                                    HttpStatusCode.Unauthorized,
                                    ApiError.unauthorizedError("User is not authorized"),
                                ) // This should never happen
                        if (right.endDate != null && right.endDate!! <= right.startDate) {
                            return@withContext call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError("Enddatum muss nach dem Startdatum liegen."),
                            )
                        }
                        if (backend.rightContainsId(right.rightId!!)) {
                            backend.upsertRight(
                                right.toBusiness().copy(
                                    lastUpdatedBy = userSession.email,
                                ),
                            )
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            backend.insertRight(
                                right.toBusiness().copy(
                                    lastUpdatedBy = userSession.email,
                                    createdBy = userSession.email,
                                ),
                            )
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.Created)
                        }
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                ApiError.INVALID_JSON,
                            ),
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
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.DELETE/api/v1/right")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["id"]
                        span.setAttribute("rightId", rightId ?: "null")
                        if (rightId == null) {
                            span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                            call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError(ApiError.NO_VALID_ID))
                        } else {
                            // Delete relations between Metadata and Right to avoid conflicts
                            backend.deleteItemEntriesByRightId(rightId)
                            val entriesDeleted = backend.deleteRight(rightId)
                            if (entriesDeleted == 1) {
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK)
                            } else {
                                span.setStatus(StatusCode.ERROR)
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError.notFoundError(
                                        detail = "Recht mit ID '$rightId' existiert nicht.",
                                    ),
                                )
                            }
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
