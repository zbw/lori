package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.DashboardConflictTypeFilter
import de.zbw.business.lori.server.DashboardTemplateNameFilter
import de.zbw.business.lori.server.DashboardTimeIntervalEndFilter
import de.zbw.business.lori.server.DashboardTimeIntervalStartFilter
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.lori.model.ErrorRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * REST-API routes for errors.
 *
 * Created on 01-18-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.errorRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/errors") {
        route("/rights") {
            get("/list") {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.GET/api/v1/errors/right/list")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 100
                        val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                        val pageSize: Int = call.request.queryParameters["pageSize"]?.toInt() ?: 25
                        if (limit < 1 || limit > 200) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: Limit parameter is expected to be between 1 and 200.",
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorRest(
                                    type = "/errors/badrequest",
                                    title = "Ungültiger Query Parameter.",
                                    detail = "Der Limit Parameter muss zwischen 1 und 200 sein.",
                                    status = "400",
                                ),
                            )
                            return@withContext
                        }
                        val dashboardTemplateNameFilter: DashboardTemplateNameFilter? =
                            QueryParameterParser.parseDashboardTemplateNameFilter(call.request.queryParameters["filterTemplateName"])
                        val dashboardConflictTypeFilter: DashboardConflictTypeFilter? =
                            QueryParameterParser.parseDashboardConflictTypeFilter(call.request.queryParameters["filterConflictType"])
                        val dashboardTimeIntervalStartFilter: DashboardTimeIntervalStartFilter? =
                            QueryParameterParser.parseDashboardStartDateFilter(call.request.queryParameters["filterTimeIntervalStart"])
                        val dashboardTimeIntervalEndFilter: DashboardTimeIntervalEndFilter? =
                            QueryParameterParser.parseDashboardEndDateFilter(call.request.queryParameters["filterTimeIntervalEnd"])
                        val testId: String? = call.request.queryParameters["testId"]
                        val receivedErrors: ErrorQueryResult =
                            backend.getRightErrorList(
                                limit,
                                offset,
                                listOfNotNull(
                                    dashboardTemplateNameFilter,
                                    dashboardConflictTypeFilter,
                                    dashboardTimeIntervalStartFilter,
                                    dashboardTimeIntervalEndFilter,
                                ),
                                testId,
                            )
                        span.setStatus(StatusCode.OK)
                        call.respond(receivedErrors.toRest(pageSize))
                    } catch (e: Exception) {
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
            authenticate("auth-login") {
                delete("{testId}") {
                    val span =
                        tracer
                            .spanBuilder("lori.LoriService.DELETE/api/v1/rights/{testId}")
                            .setSpanKind(SpanKind.SERVER)
                            .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val testId = call.parameters["testId"]
                            span.setAttribute("testId", testId ?: "null")
                            if (testId == null) {
                                span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                                return@withContext call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiError.badRequestError(
                                        detail = "Keine valide Test-ID wurde übergeben",
                                    ),
                                )
                            } else {
                                val entriesDeleted = backend.deleteErrorsByTestId(testId)
                                if (entriesDeleted == 1) {
                                    span.setStatus(StatusCode.OK)
                                    call.respond(HttpStatusCode.OK)
                                } else {
                                    call.respond(
                                        HttpStatusCode.NotFound,
                                        ApiError.notFoundError(
                                            detail = "Errors mit Test-ID '$testId' existieren nicht.",
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
    }
}
