package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ItemMetadata
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
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
        get {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/metadata?handle={handle}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val handle: String? = call.request.queryParameters["handle"]
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
