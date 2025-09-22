package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.api.lori.server.type.toUpdateRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.export.ExportJobService
import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.utils.enumOrNull
import de.zbw.lori.model.ErrorRest
import de.zbw.lori.model.ExportFormatRest
import de.zbw.lori.model.ItemSearch
import de.zbw.lori.model.JobCreatedRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * REST-API routes for jobs.
 *
 * Created on 10-09-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.jobRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
    exportJobService: ExportJobService,
) {
    route("/api/v1/export/jobs") {
        /**
         * Create a new Job.
         */
        post {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.POST/api/v1/export/jobs")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val searchTerm: String =
                        call
                            .receive(ItemSearch::class)
                            .takeIf { it.searchTerm != null }
                            ?.searchTerm
                            ?: throw BadRequestException("Invalid Json has been provided")

                    val format: ExportFormat =
                        call.request.queryParameters
                            .enumOrNull<ExportFormatRest>("format")
                            ?.toBusiness()
                            ?: ExportFormat.DEFAULT

                    val exportJob =
                        exportJobService.createJob(
                            createdBy = "unkown",
                            searchTerm = searchTerm,
                            format = format,
                        )
                    span.setStatus(StatusCode.OK)
                    call.respond(
                        HttpStatusCode.Created,
                        JobCreatedRest(
                            jobId = exportJob.id.toString(),
                            status = exportJob.status.toRest(),
                            statusUrl = "/api/v1/export/jobs/${exportJob.id}",
                        ),
                    )
                } catch (bre: BadRequestException) {
                    span.recordException(bre)
                    span.setStatus(StatusCode.ERROR, "Exception: ${bre.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError("Invalide Suchanfrage aufgrund von korrupten Request Body"),
                    )
                } catch (e: Exception) {
                    span.recordException(e)
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorRest(
                            type = "/errors/internalservererror",
                            title = "Unerwarteter Fehler.",
                            detail = "Ein interner Fehler ist aufgetreten.",
                            status = "500",
                        ),
                    )
                } finally {
                    span.end()
                }
            }
        }
        /**
         * Receive status of a job.
         */
        get("{jobId}") {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/export/jobs")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val jobId = call.parameters["jobId"]?.let { UUID.fromString(it) }
                    span.setAttribute("jobId", jobId ?.toString() ?: "null")
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
                    val exportJobStatus: ExportJob =
                        backend.getJobById(jobId) ?: return@withContext call.respond(
                            HttpStatusCode.NotFound,
                            ApiError.notFoundError(ApiError.NO_RESOURCE_FOR_ID),
                        )
                    return@withContext call.respond(
                        HttpStatusCode.OK,
                        exportJobStatus.toUpdateRest(),
                    )
                } catch (e: Exception) {
                    span.recordException(e)
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                } finally {
                    span.end()
                }
            }
        }
    }
}
