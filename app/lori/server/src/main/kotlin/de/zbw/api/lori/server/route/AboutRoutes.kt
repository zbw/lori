package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.AboutRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes for information about the system.
 *
 * Created on 06-17-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.aboutRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/about") {
        get {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/about")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    span.setStatus(StatusCode.OK)
                    call.respond(
                        AboutRest(
                            stage = backend.config.stage,
                            handleURL = backend.config.handleURL,
                            duoSLO = backend.config.duoUrlSLO,
                            duoSSO = backend.config.duoUrlSSO,
                        ),
                    )
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                } finally {
                    span.end()
                }
            }
        }
    }
}
