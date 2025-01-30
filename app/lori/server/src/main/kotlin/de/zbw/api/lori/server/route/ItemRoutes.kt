package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.Either
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.ManualRightFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SeriesFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemplateNameFilter
import de.zbw.business.lori.server.TemporalValidityFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.lori.model.ItemCountByRight
import de.zbw.lori.model.ItemEntry
import de.zbw.lori.model.ItemInformation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import kotlin.math.ceil

/**
 * REST-API routes for items.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.itemRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/item") {
        authenticate("auth-login") {
            post {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.POST/api/v1/item")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        @Suppress("SENSELESS_COMPARISON")
                        val item: ItemEntry =
                            call
                                .receive(ItemEntry::class)
                                .takeIf { it.handle != null && it.rightId != null }
                                ?: throw BadRequestException("Invalid Json has been provided")
                        span.setAttribute("item", item.toString())
                        if (backend.itemContainsEntry(item.handle, item.rightId)) {
                            span.setStatus(StatusCode.ERROR, "Conflict: Resource with this primary key already exists.")
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiError.conflictError(
                                    ApiError.RESOURCE_STILL_IN_USE,
                                ),
                            )
                        } else {
                            val deleteOnConflict: Boolean =
                                call.request.queryParameters["deleteRightOnConflict"]?.toBoolean() == true
                            when (val ret = backend.insertItemEntry(item.handle, item.rightId, deleteOnConflict)) {
                                is Either.Left -> {
                                    call.respond(ret.value.first, ret.value.second)
                                }

                                is Either.Right -> {
                                    span.setStatus(StatusCode.OK)
                                    call.respond(HttpStatusCode.Created)
                                }
                            }
                        }
                    } catch (e: BadRequestException) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(ApiError.INVALID_JSON),
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

        route("/metadata") {
            authenticate("auth-login") {
                delete {
                    val span =
                        tracer
                            .spanBuilder("lori.LoriService.DELETE/api/v1/item/metadata/{handle}")
                            .setSpanKind(SpanKind.SERVER)
                            .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val handle: String? = call.request.queryParameters["handle"]
                            span.setAttribute("handle", handle ?: "null")
                            if (handle == null) {
                                span.setStatus(
                                    StatusCode.ERROR,
                                    "BadRequest: No valid id has been provided in the url.",
                                )
                                call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                            } else {
                                backend.deleteItemEntriesByHandle(handle)
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK)
                            }
                        } catch (e: Exception) {
                            span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                            call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                        } finally {
                            span.end()
                        }
                    }
                }
            }
            get {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.GET/api/v1/item/handle/{handle}")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val handle: String? = call.request.queryParameters["handle"]
                        span.setAttribute("handle", handle ?: "null")
                        if (handle == null) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: No valid id has been provided in the url.",
                            )
                            call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError(ApiError.NO_VALID_ID))
                        } else {
                            if (!backend.metadataContainsHandle(handle)) {
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError.notFoundError(ApiError.NO_RESOURCE_FOR_ID),
                                )
                            } else {
                                val rights = backend.getRightEntriesByHandle(handle)
                                span.setStatus(StatusCode.OK)
                                call.respond(rights.map { it.toRest() })
                            }
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }
        }

        route("/right") {
            authenticate("auth-login") {
                delete("{rightId}") {
                    val span =
                        tracer
                            .spanBuilder("lori.LoriService.DELETE/api/v1/item/right/{rightId}")
                            .setSpanKind(SpanKind.SERVER)
                            .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val rightId = call.parameters["rightId"]
                            span.setAttribute("rightId", rightId ?: "null")
                            if (rightId == null) {
                                span.setStatus(
                                    StatusCode.ERROR,
                                    "BadRequest: No valid id has been provided in the url.",
                                )
                                call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError(ApiError.NO_VALID_ID))
                            } else {
                                backend.deleteItemEntriesByRightId(rightId)
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK)
                            }
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

        route("/count/right") {
            get("{rightId}") {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.GET/api/v1/item/count/right/{rightId}")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val rightId = call.parameters["rightId"]
                        span.setAttribute("rightId", rightId ?: "null")
                        if (rightId == null) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: No valid id has been provided in the url.",
                            )
                            call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError(ApiError.NO_VALID_ID))
                        } else {
                            val count = backend.countItemByRightId(rightId)
                            span.setStatus(StatusCode.OK)
                            call.respond(
                                ItemCountByRight(
                                    rightId = rightId,
                                    count = count,
                                ),
                            )
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }
        }

        authenticate("auth-login") {
            delete {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.DELETE/api/v1/item?handle={handle}&right-id={rightId}")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val handle: String? = call.request.queryParameters["handle"]
                        val rightId: String? = call.request.queryParameters["right-id"]
                        span.setAttribute("handle", handle ?: "null")
                        span.setAttribute("rightId", rightId ?: "null")
                        if (handle == null || rightId == null) {
                            span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                            call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError(ApiError.NO_VALID_ID))
                        } else {
                            backend.deleteItemEntry(handle, rightId)
                            if (backend.countItemByRightId(rightId) == 0) {
                                // TODO(CB): not sure if this is so smart
                                backend.deleteRight(rightId)
                            }
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        }
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, ApiError.internalServerError())
                    } finally {
                        span.end()
                    }
                }
            }
        }

        route("/list") {
            get {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.GET/api/v1/item/list")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                        val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                        val pageSize: Int = call.request.queryParameters["pageSize"]?.toInt() ?: 1
                        if (limit < 1 || limit > 100) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: Limit parameter is expected to be between (0,100]",
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError("Limit parameter is expected to be between (0,100]"),
                            )
                        } else if (offset < 0) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: Offset parameter is expected to be larger or equal zero",
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError(
                                    "Offset parameter is expected to be larger or equal zero",
                                ),
                            )
                        } else if (pageSize < 0) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: PageSize parameter is expected to be between (0,100]",
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError(
                                    "PageSize parameter is expected to be between (0,100]",
                                ),
                            )
                        } else {
                            val items = backend.getItemList(limit, offset)
                            val entries = backend.countMetadataEntries()
                            val totalPages = ceil(entries.toDouble() / pageSize.toDouble()).toInt()
                            span.setStatus(StatusCode.OK)
                            call.respond(
                                ItemInformation(
                                    itemArray = items.map { it.toRest() },
                                    totalPages = totalPages,
                                    numberOfResults = entries,
                                ),
                            )
                        }
                    } catch (e: NumberFormatException) {
                        span.setStatus(StatusCode.ERROR, "NumberFormatException: ${e.message}")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                "Parameters have a bad format",
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

        get("search") {
            val span =
                tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/item/search")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val searchTerm: String? = call.request.queryParameters["searchTerm"]
                    val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 25
                    val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                    val facetsOnly: Boolean = call.request.queryParameters["facetsOnly"]?.toBoolean() == true
                    val pageSize: Int = call.request.queryParameters["pageSize"]?.toInt() ?: 1
                    val publicationYearFilter: PublicationYearFilter? =
                        QueryParameterParser.parsePublicationYearFilter(call.request.queryParameters["filterPublicationYear"])
                    val publicationTypeFilter: PublicationTypeFilter? =
                        QueryParameterParser.parsePublicationTypeFilter(call.request.queryParameters["filterPublicationType"])
                    val paketSigelFilter: PaketSigelFilter? =
                        QueryParameterParser.parsePaketSigelFilter(call.request.queryParameters["filterPaketSigel"])
                    val zdbIdFilter: ZDBIdFilter? =
                        QueryParameterParser.parseZDBIdFilter(call.request.queryParameters["filterZDBId"])
                    val seriesFilter: SeriesFilter? =
                        QueryParameterParser.parseSeriesFilter(call.request.queryParameters["filterSeries"])
                    val licenceUrlFilter: LicenceUrlFilter? =
                        QueryParameterParser.parseLicenceUrlFilter(call.request.queryParameters["filterLicenceUrl"])
                    val accessStateFilter: AccessStateFilter? =
                        QueryParameterParser.parseAccessStateFilter(call.request.queryParameters["filterAccessState"])
                    val temporalValidityFilter: TemporalValidityFilter? =
                        QueryParameterParser.parseTemporalValidity(call.request.queryParameters["filterTemporalValidity"])
                    val formalRuleFilter: FormalRuleFilter? =
                        QueryParameterParser.parseFormalRuleFilter(
                            call.request.queryParameters["filterFormalRule"],
                        )
                    val startDateFilter: StartDateFilter? =
                        QueryParameterParser.parseStartDateFilter(
                            call.request.queryParameters["filterStartDate"],
                        )
                    val endDateFilter: EndDateFilter? =
                        QueryParameterParser.parseEndDateFilter(
                            call.request.queryParameters["filterEndDate"],
                        )
                    val validOnFilter: RightValidOnFilter? =
                        QueryParameterParser.parseRightValidOnFilter(
                            call.request.queryParameters["filterValidOn"],
                        )
                    val noRightInformationFilter: NoRightInformationFilter? =
                        QueryParameterParser.parseNoRightInformationFilter(
                            call.request.queryParameters["filterNoRightInformation"],
                        )

                    val manualRightFilter: ManualRightFilter? =
                        QueryParameterParser.parseManualRightFilter(
                            call.request.queryParameters["filterManualRight"],
                        )

                    val rightIdsFilter: TemplateNameFilter? =
                        QueryParameterParser.parseTemplateNameFilter(
                            call.request.queryParameters["filterRightId"],
                        )

                    val accessStateOnDateFilter =
                        QueryParameterParser.parseAccessStateOnDate(
                            call.request.queryParameters["filterAccessStateOn"],
                        )

                    span.setAttribute("searchTerm", searchTerm ?: "")
                    span.setAttribute("limit", limit.toString())
                    span.setAttribute("offset", offset.toString())
                    span.setAttribute("pageSize", pageSize.toString())

                    if (limit < 1 || limit > 100) {
                        span.setStatus(
                            StatusCode.ERROR,
                            "BadRequest: Limit parameter is expected to be between (0,100]",
                        )
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                "Limit parameter is expected to be between 1 and 100",
                            ),
                        )
                        return@withContext
                    }
                    if (offset < 0) {
                        span.setStatus(
                            StatusCode.ERROR,
                            "BadRequest: Offset parameter is expected to be larger or equal zero",
                        )
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                "Offset parameter is expected to be larger or equal zero",
                            ),
                        )
                        return@withContext
                    }
                    if (pageSize < 0) {
                        span.setStatus(
                            StatusCode.ERROR,
                            "BadRequest: PageSize parameter is expected to be between (0,100]",
                        )
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                "PageSize parameter is expected to be between 1 and 100",
                            ),
                        )
                        return@withContext
                    }
                    val metadataFilters =
                        listOfNotNull(
                            licenceUrlFilter,
                            paketSigelFilter,
                            publicationYearFilter,
                            publicationTypeFilter,
                            zdbIdFilter,
                            seriesFilter,
                        )
                    val rightFilters =
                        listOfNotNull(
                            accessStateFilter,
                            endDateFilter,
                            formalRuleFilter,
                            startDateFilter,
                            temporalValidityFilter,
                            validOnFilter,
                            rightIdsFilter,
                            manualRightFilter,
                            accessStateOnDateFilter,
                        )

                    val queryResult: SearchQueryResult =
                        backend.searchQuery(
                            searchTerm,
                            limit,
                            offset,
                            metadataFilters,
                            rightFilters,
                            noRightInformationFilter,
                            emptyList(),
                            facetsOnly,
                        )
                    span.setStatus(StatusCode.OK)
                    call.respond(
                        queryResult.toRest(pageSize),
                    )
                } catch (e: NumberFormatException) {
                    span.setStatus(StatusCode.ERROR, "NumberFormatException: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, ApiError.badRequestError("Parameters have a bad format"))
                } catch (pe: ParsingException) {
                    span.setStatus(StatusCode.ERROR, "ParsingException: ${pe.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError.badRequestError("Such-Anfrage ist ungültig. Siehe ?-Icon für mehr Informationen"),
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
