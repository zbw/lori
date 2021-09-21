package de.zbw.api.auth.server.route

import de.zbw.business.auth.server.ApiBackend
import io.ktor.application.call
import io.ktor.features.NotFoundException
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route

/**
 * REST-API routes.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.apiRoutes() {
    route("/api/v1") {
        route("/userinfo/users/") {
            get("{id}") {
                val userId = call.parameters["id"]?.toIntOrNull() ?: throw NotFoundException()
                val userInformation = ApiBackend.getUserInformation(userId)
                call.respond(userInformation)
            }
        }
    }
}
