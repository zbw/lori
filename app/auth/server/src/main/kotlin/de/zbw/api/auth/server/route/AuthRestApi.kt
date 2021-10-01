package de.zbw.api.auth.server.route

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import de.zbw.api.auth.server.config.AuthConfiguration
import de.zbw.auth.model.ApiResponse
import de.zbw.auth.model.SignInAnswer
import de.zbw.auth.model.SignInUserData
import de.zbw.auth.model.SignUpUserData
import de.zbw.auth.model.UserRole
import de.zbw.business.auth.server.AuthBackend
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route
import java.util.Date

/**
 * REST-API routes.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.authInformationRoutes(backend: AuthBackend, config: AuthConfiguration) {
    route("/api/v1/auth") {
        post("/signup") {
            try {
                val signUpData: SignUpUserData = call.receive(SignUpUserData::class)
                if (!backend.isUsernameAvailable(signUpData.name)) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@post
                }
                backend.registerNewUser(signUpData)?.let {
                    call.respond(HttpStatusCode.OK)
                } ?: let {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                // Unless everything is traced, throwing this exception is necessary to see the error message somewhere
                throw(e)
            }
        }
        post("/signin") {
            try {
                val signInUserData = call.receive(SignInUserData::class)
                val userEntry = backend.getUserEntry(signInUserData)
                val isValid = userEntry?.let {
                    AuthBackend.verifyPassword(signInUserData.password, it.hash)
                } ?: false
                if (isValid && userEntry != null) {
                    val token: String = JWT.create()
                        .withAudience(config.jwtAudience)
                        .withIssuer(config.jwtIssuer)
                        .withClaim("username", signInUserData.name)
                        .withExpiresAt(Date(System.currentTimeMillis() + 360000))
                        .sign(Algorithm.HMAC256(config.jwtSecret))
                    call.respond(
                        SignInAnswer(
                            username = signInUserData.name,
                            email = userEntry.email,
                            roles = backend.getUserRolesById(userEntry.id).map {
                                UserRole(it)
                            },
                            accessToken = token,
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse(
                            HttpStatusCode.Unauthorized.value,
                            HttpStatusCode.Unauthorized.toString(),
                            "User or password are wrong."
                        )
                    )
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                // Unless everything is traced, throwing this exception is necessary to see the error message somewhere
                throw(e)
            }
        }
    }
}
