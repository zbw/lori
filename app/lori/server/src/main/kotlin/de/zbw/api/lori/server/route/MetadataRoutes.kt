package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.lori.model.MetadataRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
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
 * REST-API routes for metadata.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.metadataRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/metadata") {
        authenticate("auth-login") {
            post {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.POST/api/v1/metadata")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        // receive() may return an object where non-null fields are null.
                        @Suppress("SENSELESS_COMPARISON")
                        val metadata: MetadataRest =
                            call
                                .receive<MetadataRest>()
                                .takeIf { it.handle != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        span.setAttribute("metadata", metadata.toString())
                        if (backend.metadataContainsHandle(metadata.handle)) {
                            span.setStatus(StatusCode.ERROR, "Conflict: Resource with this id already exists.")
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiError.conflictError(ApiError.RESOURCE_STILL_IN_USE),
                            )
                        } else {
                            backend.insertMetadataElement(metadata.toBusiness())
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.Created)
                        }
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(ApiError.INVALID_JSON),
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

            delete("{handle}") {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.DELETE/api/v1/metadata")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val handle = call.parameters["handle"]
                        span.setAttribute("handle", handle ?: "null")
                        if (handle == null) {
                            span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError(ApiError.NO_VALID_ID),
                            )
                        } else if (backend.itemContainsMetadata(handle)) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "Conflict: Metadata-Id $handle is still in use. Can't be deleted.",
                            )
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiError.conflictError(
                                    ApiError.RESOURCE_STILL_IN_USE,
                                ),
                            )
                        } else {
                            backend.deleteMetadataByHandle(handle)
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }

            put {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.PUT/api/v1/metadata")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        // receive() may return an object where non-null fields are null.
                        @Suppress("SENSELESS_COMPARISON")
                        val metadata: MetadataRest =
                            call.receive(MetadataRest::class).takeIf { it.handle != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        if (backend.metadataContainsHandle(metadata.handle)) {
                            backend.upsertMetadataElements(listOf(metadata.toBusiness()))
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            backend.insertMetadataElement(metadata.toBusiness())
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.Created)
                        }
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

        get("{handle}") {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/metadata/{handle}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val handle = call.parameters["handle"]
                    span.setAttribute("handle", handle ?: "null")
                    if (handle == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(ApiError.NO_VALID_ID),
                        )
                    } else {
                        val metadataElements: List<ItemMetadata> = backend.getMetadataElementsByIds(listOf(handle))
                        metadataElements.takeIf { it.isNotEmpty() }?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(metadataElements.first().toRest())
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
                    call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                } finally {
                    span.end()
                }
            }
        }

        get("/list") {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/metadata/list")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            try {
                val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                if (limit < 1 || limit > 100) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: Limit parameter is expected to be between (0,100]")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError("Limit parameter is expected to be between 1 and 100"),
                    )
                    return@get
                } else if (offset < 0) {
                    span.setStatus(
                        StatusCode.ERROR,
                        "BadRequest: Offset parameter is expected to be larger or equal zero",
                    )
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError("Offset parameter is expected to be larger or equal zero"),
                    )
                    return@get
                } else {
                    val metadataElements: List<ItemMetadata> = backend.getMetadataList(limit, offset)
                    span.setStatus(StatusCode.OK)
                    call.respond(metadataElements.map { it.toRest() })
                }
            } catch (e: NumberFormatException) {
                span.setStatus(StatusCode.ERROR, "NumberFormatException: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError("Parameters have a bad format"))
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
            } finally {
                span.end()
            }
        }
    }
}
