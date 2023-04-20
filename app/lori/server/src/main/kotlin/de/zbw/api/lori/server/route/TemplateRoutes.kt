package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Template
import de.zbw.lori.model.TemplateIdCreated
import de.zbw.lori.model.TemplateRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.postgresql.util.PSQLException

/**
 * REST-API routes for templates.
 *
 * Created on 04-18-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.templateRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/template") {
        post {
            val span: Span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/template")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val template: TemplateRest = call.receive(TemplateRest::class)
                    span.setAttribute("template", template.toString())
                    val pk = backend.insertTemplate(template.toBusiness())
                    span.setStatus(StatusCode.OK)
                    call.respond(
                        HttpStatusCode.Created,
                        TemplateIdCreated(templateId = pk.templateId, rightId = pk.rightId)
                    )
                } catch (pe: PSQLException) {
                    if (pe.sqlState == ApiError.PSQL_CONFLICT_ERR_CODE) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(
                            HttpStatusCode.Conflict,
                            ApiError.conflictError(
                                detail = "Ein Template mit diesem Namen existiert bereits.",
                            )
                        )
                    } else {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(
                                detail = "Ein interner Datenbankfehler ist aufgetreten.",
                            )
                        )
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(
                            detail = "Ein interner Fehler ist aufgetreten.",
                        ),
                    )
                } finally {
                    span.end()
                }
            }
        }
        /**
         * Update an existing Template.
         */
        put {
            val span = tracer
                .spanBuilder("lori.LoriService.PUT/api/v1/template")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val template: TemplateRest = call.receive(TemplateRest::class)
                        .takeIf { it.templateName != null && it.templateId != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("template", template.toString())
                    val insertedRows = backend.updateTemplate(template.templateId!!, template.toBusiness())
                    if (insertedRows == 1) {
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        span.setStatus(StatusCode.ERROR)
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiError.notFoundError(
                                detail = "Für das Template mit Id ${template.templateId} existiert kein Eintrag.",
                            )
                        )
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError(
                            detail = "Das JSON Format ist ungültig und konnte nicht gelesen werden.",
                        ),
                    )
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(),
                    )
                } finally {
                    span.end()
                }
            }
        }
    }
    route("/api/v1/template") {
        /**
         * Return Template for a given id.
         */
        get("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.GET/api/v1/template/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val templateId = call.parameters["id"]?.toInt()
                    span.setAttribute("templateId", templateId?.toString() ?: "null")
                    if (templateId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        val template: Template? = backend.getTemplateById(templateId)
                        template?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(template.toRest())
                        } ?: let {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError.notFoundError(
                                    detail = "Für das Template mit Id: $templateId existiert kein Eintrag.",
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(),
                    )
                } finally {
                    span.end()
                }
            }
        }
        post {
            val span: Span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/template")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val template: TemplateRest = call.receive(TemplateRest::class)
                    span.setAttribute("template", template.toString())
                    val pk = backend.insertTemplate(template.toBusiness())
                    span.setStatus(StatusCode.OK)
                    call.respond(
                        HttpStatusCode.Created,
                        TemplateIdCreated(templateId = pk.templateId, rightId = pk.rightId)
                    )
                } catch (pe: PSQLException) {
                    if (pe.sqlState == ApiError.PSQL_CONFLICT_ERR_CODE) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(
                            HttpStatusCode.Conflict,
                            ApiError.conflictError(
                                detail = "Ein Template mit diesem Namen existiert bereits.",
                            )
                        )
                    } else {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError.internalServerError(
                                detail = "Ein interner Datenbankfehler ist aufgetreten.",
                            )
                        )
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(
                            detail = "Ein interner Fehler ist aufgetreten.",
                        ),
                    )
                } finally {
                    span.end()
                }
            }
        }
        /**
         * Update an existing Template.
         */
        put {
            val span = tracer
                .spanBuilder("lori.LoriService.PUT/api/v1/template")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val template: TemplateRest = call.receive(TemplateRest::class)
                        .takeIf { it.templateName != null && it.templateId != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("template", template.toString())
                    val insertedRows = backend.updateTemplate(template.templateId!!, template.toBusiness())
                    if (insertedRows == 1) {
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        span.setStatus(StatusCode.ERROR)
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiError.notFoundError(
                                detail = "Für das Template mit Id ${template.templateId} existiert kein Eintrag.",
                            )
                        )
                    }
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError(
                            detail = "Das JSON Format ist ungültig und konnte nicht gelesen werden.",
                        ),
                    )
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(),
                    )
                } finally {
                    span.end()
                }
            }
        }

        /**
         * Delete Template by Id.
         */
        delete("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.DELETE/api/v1/template/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val templateId = call.parameters["id"]?.toInt()
                    span.setAttribute("templateId", templateId?.toString() ?: "null")
                    if (templateId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                detail = "Keine valide numerische Id wurde übergeben",
                            ),
                        )
                    } else {
                        val entriesDeleted = backend.deleteTemplate(templateId)
                        if (entriesDeleted == 1) {
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError.notFoundError(
                                    detail = "Für die TemplateId $templateId existiert kein Eintrag.",
                                ),
                            )
                        }
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError.internalServerError(
                            detail = "Ein interner Datenbankfehler ist aufgetreten.",
                        ),
                    )
                } finally {
                    span.end()
                }
            }
        }
    }
}
