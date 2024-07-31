package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.SamlUtils
import de.zbw.api.lori.server.type.UserSession
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserPermission
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
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
import net.shibboleth.utilities.java.support.resolver.ResolverException
import org.opensaml.core.xml.schema.XSString
import org.opensaml.saml.saml2.core.Response
import org.opensaml.xmlsec.signature.support.SignatureException
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Routes covering UI Paths.
 *
 * Created on 11-15-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.guiRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
    samlUtils: SamlUtils = SamlUtils(backend.config.duoSenderEntityId),
) {
    route("/ui/callback-sso") {
        post {
            val span: Span =
                tracer
                    .spanBuilder("lori.LoriService.POST/ui/callback-sso")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val text = SamlUtils.decodeSAMLResponse(call.receiveText())

                    val response: Response =
                        samlUtils.unmarshallSAMLObject(Response::class.java, text)
                            ?: throw BadRequestException("Input is invalid. Expected SAML2.0 response in XML format.")
                    val now = Instant.now()
                    if (
                        response.assertions.size == 0 ||
                        response.assertions[0].conditions.notBefore > now ||
                        response.assertions[0].conditions.notOnOrAfter <= now
                    ) {
                        throw SecurityException("Message is not valid anymore")
                    }
                    samlUtils.verifySignatureUsingSignatureValidator(
                        response.signature ?: throw SecurityException("No signature in response."),
                    )
                    val email =
                        response.assertions[0]
                            .subject.nameID
                            ?.value
                            ?: throw BadRequestException("Response lacks name ID")
                    val permissions =
                        SamlUtils
                            .getAttributeValuesByName(response.assertions[0], "permissions")
                            .filterIsInstance<XSString>()
                            .mapNotNull { xss -> xss.value?.uppercase()?.let { UserPermission.valueOf(it) } }
                    call.sessions.set(
                        "JSESSIONID",
                        UserSession(
                            email = email,
                            permissions = permissions,
                            sessionId =
                                backend.insertSession(
                                    Session(
                                        authenticated = true,
                                        permissions = permissions,
                                        firstName = email,
                                        lastName = null,
                                        sessionID = null,
                                        validUntil = Instant.now().plus(1, ChronoUnit.DAYS),
                                    ),
                                ),
                        ),
                    )
                    call.respondRedirect(
                        "/ui?login=success",
                    )
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    when (e) {
                        is SecurityException,
                        is SignatureException,
                        is BadRequestException,
                        is ResolverException,
                        ->
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                ApiError.unauthorizedError(e.message),
                            )

                        else ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(),
                            )
                    }
                }
            }
        }
    }
}
