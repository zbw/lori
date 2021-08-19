package de.zbw.api.access.server.route

import de.zbw.access.model.AccessInformation
import de.zbw.api.access.server.type.toBusiness
import de.zbw.api.access.server.type.toRest
import de.zbw.business.access.server.AccessServerBackend
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import java.sql.SQLException

/**
 * REST-API routes.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.accessInformationRoutes(backend: AccessServerBackend) {
    route("/api/v1/accessinformation") {
        post {
            try {
                val accessInformation: AccessInformation =
                    call.receive(AccessInformation::class)?.takeIf { it.id != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                if (backend.containsAccessRightId(accessInformation.id)) {
                    call.respond(HttpStatusCode.Conflict, "Resource with this id already exists.")
                } else {
                    backend.insertAccessRightEntry(accessInformation.toBusiness())
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
                    val accessRights = backend.getAccessRightEntries(listOf(headerId))
                    call.respond(accessRights.first().toRest())
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
