package de.zbw.api.access.server.route

import de.zbw.access.model.AccessInformation
import de.zbw.api.access.server.type.toBusiness
import de.zbw.api.access.server.type.toRest
import de.zbw.business.access.server.AccessServerBackend
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
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
fun Routing.apiRoutes(backend: AccessServerBackend) {
    route("/api/v1") {
        route("/accessinformation") {
            post {
                try {
                    val newAccessInformation: AccessInformation = call.receive(AccessInformation::class)
                    if (backend.containsAccessRightId(newAccessInformation.id)) {
                        call.respond(HttpStatusCode.Conflict, "ResoulistOf(testId)rce with this id already exists.")
                    } else {
                        backend.insertAccessRightEntry(newAccessInformation.toBusiness())
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: ContentTransformationException) {
                    call.respond(HttpStatusCode.BadRequest, "Payload had an unexpected format: ${e.message}")
                } catch (e: BadRequestException) {
                    call.respond(HttpStatusCode.BadRequest, "Bad request: ${e.message}")
                } catch (e: SQLException) {
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                    throw(e)
                }
            }
            get("{ids}") {
                val headerIds = call.parameters["ids"]
                if (headerIds == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    // TODO: Invalid Id should result in 400
                    val accessRights = backend.getAccessRightEntries(headerIds.split(","))
                    call.respond(accessRights.map { it.toRest() })
                }
            }
            get("/list") {
                val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0

                // TODO: Check for 100 >= limit >= 1 and offset >= 0 -> otherwise return 400

                val accessRights = backend.getAccessRightList(limit, offset)
                call.respond(accessRights.map { it.toRest() })
            }
        }
    }
}
