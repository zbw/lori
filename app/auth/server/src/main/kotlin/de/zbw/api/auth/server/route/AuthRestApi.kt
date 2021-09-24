package de.zbw.api.auth.server.route

import de.zbw.auth.model.SignUpUserData
import de.zbw.business.auth.server.AuthBackend
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import java.sql.SQLException

/**
 * REST-API routes.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.authInformationRoutes(backend: AuthBackend) {
    route("/api/v1/auth") {
        post("/signup") {
            try {
                val signUpData: SignUpUserData = call.receive(SignUpUserData::class)
                if (!backend.isUsernameAvailable(signUpData.name)) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    backend.registerNewUser(signUpData)
                    call.respond(HttpStatusCode.OK)
                }
            } catch (e: SQLException) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                // Unless everything is traced, throwing this exception is necessary to see the error message somewhere
                throw(e)
            }
        }
        post("signin") {
        }
    }
}
