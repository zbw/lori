package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * REST-API routes for downloads.
 *
 * Created on 16-09-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.downloadRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/download") {
        get("{jobId}") {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/download/{jobId}")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                val jobId = call.parameters["jobId"]?.let { UUID.fromString(it) }
                span.setAttribute("jobId", jobId?.toString() ?: "null")
                if (jobId == null) {
                    span.setStatus(
                        StatusCode.ERROR,
                        "BadRequest: No valid id has been provided in the url.",
                    )
                    return@withContext call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError(ApiError.NO_VALID_ID),
                    )
                }
                val exportJob: ExportJob =
                    backend.getJobById(jobId) ?: return@withContext call.respond(
                        HttpStatusCode.NotFound,
                        ApiError.notFoundError(ApiError.NO_RESOURCE_FOR_ID),
                    )
                val file =
                    exportJob.getFile() ?: return@withContext call.respond(
                        HttpStatusCode.NotFound,
                        ApiError.notFoundError("Datei konnte nicht gefunden werden"),
                    )
                val contentType =
                    when (exportJob.format) {
                        ExportFormat.CSV -> ContentType.Text.CSV
                        ExportFormat.JSON -> ContentType.Application.Json
                    }

                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter(
                            ContentDisposition.Parameters.FileName,
                            file.name,
                        ).toString(),
                )

                call.respondOutputStream(contentType = contentType) {
                    file.inputStream().use { input ->
                        input.copyTo(this)
                    }
                }
            }
        }
    }
}
