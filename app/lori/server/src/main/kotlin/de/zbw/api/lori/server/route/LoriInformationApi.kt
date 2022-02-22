package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.ItemRest
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import java.sql.SQLException

/**
 * REST-API routes.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.accessInformationRoutes(backend: LoriServerBackend) {
    route("/api/v1/item") {
        post {
            try {
                // receive() may return an object where non-null fields are null.
                @Suppress("SENSELESS_COMPARISON")
                val item: ItemRest =
                    call.receive(ItemRest::class).takeIf { it.id != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                if (backend.containsAccessRightId(item.id)) {
                    call.respond(HttpStatusCode.Conflict, "Resource with this id already exists.")
                } else {
                    backend.insertItem(item.toBusiness())
                    call.respond(HttpStatusCode.Created)
                }
            } catch (e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                // Unless everything is traced, throwing this exception is necessary to see the error message somewhere
                throw(e)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
                throw(e)
            }
        }
        get("{id}") {
            try {
                val headerId = call.parameters["id"]
                if (headerId == null) {
                    call.respond(HttpStatusCode.BadRequest, "No id has been provided in the url.")
                } else {
                    val item = backend.getItems(listOf(headerId))
                    call.respond(item.first().toRest())
                }
            } catch (e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                throw(e)
            }
        }

        put {
            try {
                // receive() may return an object where non-null fields are null.
                @Suppress("SENSELESS_COMPARISON")
                val item: ItemRest =
                    call.receive(ItemRest::class).takeIf { it.id != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                if (backend.containsAccessRightId(item.id)) {
                    backend.upsertItems(listOf(item.toBusiness()))
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    backend.insertItem(item.toBusiness())
                    call.respond(HttpStatusCode.Created)
                }
            } catch (e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                throw(e)
            }
        }

        delete("{id}") {
            try {
                val headerId = call.parameters["id"]
                if (headerId == null) {
                    call.respond(HttpStatusCode.NotFound, "No id has been provided in the url.")
                } else {
                    backend.containsAccessRightId(headerId)
                        .takeIf { it }?.let {
                            backend.deleteAccessRightEntries(listOf(headerId))
                            call.respond(HttpStatusCode.OK)
                        } ?: let {
                        call.respond(HttpStatusCode.NotFound, "Resource with this id does not exist.")
                    }
                }
            } catch (e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                throw(e)
            }
        }
        get("/list") {
            try {
                val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                if (limit < 1 || limit > 100) {
                    call.respond(HttpStatusCode.BadRequest, "Limit parameter is expected to be between (0,100]")
                    return@get
                } else if (offset < 0) {
                    call.respond(HttpStatusCode.BadRequest, "Offset parameter is expected to be larger or equal zero")
                    return@get
                } else {
                    val accessRights = backend.getAccessRightList(limit, offset)
                    call.respond(accessRights.map { it.toRest() })
                }
            } catch (e: NumberFormatException) {
                call.respond(HttpStatusCode.BadRequest, "Parameters had a bad format")
            }
        }
    }
}
