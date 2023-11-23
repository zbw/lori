package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.SamlUtils
import de.zbw.api.lori.server.type.UserSession
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.sessions
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.opensaml.saml.saml2.core.Response
import java.net.URLDecoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

/**
 * Routes covering UI Paths.
 *
 * Created on 11-15-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.guiRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
    samlUtils: SamlUtils = SamlUtils(),
) {
    route("/ui/callback-sso") {
        post {
            val span: Span = tracer
                .spanBuilder("lori.LoriService.POST/ui/callback-sso")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val text = call.receiveText()
                        .substringAfter("SAMLResponse=")
                        .let { URLDecoder.decode(it, "UTF-8") }
                        .let { String(Base64.getDecoder().decode(it)) }
                    val response: Response? = samlUtils.unmarshallSAMLObject(Response::class.java, text)
                    val now = Instant.now()
                    if (
                        response == null ||
                        response.assertions.size == 0 ||
                        response.assertions[0].conditions.notBefore > now ||
                        response.assertions[0].conditions.notOnOrAfter <= now
                    ) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiError.unauthorizedError(backend.config.signInURL)
                        )
                        return@withContext
                    } else {
                        val email = response.assertions[0].subject.nameID?.value ?: ""
                        call.sessions.set(
                            "JSESSIONID",
                            UserSession(
                                email = email,
                                role = UserRole.READONLY,
                                sessionId = backend.insertSession(
                                    Session(
                                        authenticated = true,
                                        role = UserRole.READONLY,
                                        firstName = email,
                                        lastName = null,
                                        sessionID = null,
                                        validUntil = Instant.now().plus(1, ChronoUnit.DAYS)
                                    )
                                )
                            )
                        )
                        call.respondRedirect(
                            "/ui?login=success"
                        )
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(),
                    )
                }
            }
        }
    }
}
