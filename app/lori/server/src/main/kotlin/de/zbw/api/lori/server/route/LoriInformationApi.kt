package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.Item
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.ItemRest
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.accessInformationRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/item") {
        post {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/item")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    // receive() may return an object where non-null fields are null.
                    @Suppress("SENSELESS_COMPARISON")
                    val item: ItemRest =
                        call.receive(ItemRest::class)
                            .takeIf { it.metadata != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("item", item.toString())
                    if (backend.containsMetadataId(item.metadata.metadataId)) {
                        span.setStatus(StatusCode.ERROR, "Conflict: Resource with this id already exists.")
                        call.respond(HttpStatusCode.Conflict, "Resource with this id already exists.")
                    } else {
                        backend.insertMetadataElement(item.metadata.toBusiness())
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
        get("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.GET/api/v1/item/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val itemId = call.parameters["id"]
                    span.setAttribute("idemId", itemId ?: "null")
                    if (itemId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        val item: Item? = backend.getRightsByMetadataId(itemId)
                        item?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(item.toRest())
                        } ?: let {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(HttpStatusCode.NotFound, "No item found for given id.")
                        }
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        put {
            val span = tracer
                .spanBuilder("lori.LoriService.PUT/api/v1/item")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    // receive() may return an object where non-null fields are null.
                    @Suppress("SENSELESS_COMPARISON")
                    val item: ItemRest =
                        call.receive(ItemRest::class).takeIf { it.metadata != null }
                            ?: throw BadRequestException("Invalid Json has been provided")
                    if (backend.containsMetadataId(item.metadata.metadataId)) {
                        backend.upsertMetadataElements(listOf(item.metadata.toBusiness()))
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        backend.insertMetadataElement(item.metadata.toBusiness())
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

        get("/list") {
            val span = tracer
                .spanBuilder("lori.LoriService.GET/api/v1/item/list")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            try {
                val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                if (limit < 1 || limit > 100) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: Limit parameter is expected to be between (0,100]")
                    call.respond(HttpStatusCode.BadRequest, "Limit parameter is expected to be between (0,100]")
                    return@get
                } else if (offset < 0) {
                    span.setStatus(
                        StatusCode.ERROR,
                        "BadRequest: Offset parameter is expected to be larger or equal zero"
                    )
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Offset parameter is expected to be larger or equal zero"
                    )
                    return@get
                } else {
                    val items = backend.getItemList(limit, offset)
                    span.setStatus(StatusCode.OK)
                    call.respond(items.map { it.toRest() })
                }
            } catch (e: NumberFormatException) {
                span.setStatus(StatusCode.ERROR, "NumberFormatException: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Parameters have a bad format")
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
            } finally {
                span.end()
            }
        }
    }
}
