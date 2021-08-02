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
                    val receive: AccessInformation = call.receive(AccessInformation::class)
                    // TODO: check if id already exist.
                    backend.insertAccessRightEntry(receive.toBusiness())
                } catch (e: ContentTransformationException) {
                    call.respond(HttpStatusCode.BadRequest, "Payload had an unexpected format: ${e.message}")
                    throw(e)
                } catch (e: BadRequestException) {
                    call.respond(HttpStatusCode.BadRequest, "Bad request: ${e.message}")
                    throw(e)
                } catch (e: SQLException) {
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occured.")
                    throw(e)
                }
                call.respond(HttpStatusCode.Created)
            }
            get("{ids}") {
                val headerIds = call.parameters["ids"]
                if (headerIds == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val accessRights = backend.getAccessRightEntries(headerIds.split(","))
                    call.respond(accessRights.map { it.toRest() })
                }
            }
        }
    }
}
