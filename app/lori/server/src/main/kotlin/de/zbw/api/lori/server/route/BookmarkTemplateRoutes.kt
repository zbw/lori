package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.lori.model.BookmarkTemplateBatchRest
import de.zbw.lori.model.BookmarkTemplateRest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.postgresql.util.PSQLException

/**
 * REST-API routes for bookmarks template connections.
 *
 * Created on 05-05-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.bookmarkTemplateRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/bookmarktemplates") {
        authenticate("auth-login") {
            post {
                val span: Span =
                    tracer
                        .spanBuilder("lori.LoriService.POST/api/v1/bookmarktemplates")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val bookmarkTemplate: BookmarkTemplateRest = call.receive(BookmarkTemplateRest::class)
                        span.setAttribute("Bookmark-Id", bookmarkTemplate.bookmarkId.toString())
                        span.setAttribute("Right-Id", bookmarkTemplate.rightId)
                        val created =
                            backend.insertBookmarkTemplatePair(
                                bookmarkId = bookmarkTemplate.bookmarkId,
                                rightId = bookmarkTemplate.rightId,
                            )
                        if (created == 1) {
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.Created)
                        } else {
                            span.setStatus(StatusCode.ERROR, "Exception: No row was inserted")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(
                                    detail = "Ein interner Fehler ist aufgetreten.",
                                ),
                            )
                        }
                    } catch (pe: PSQLException) {
                        if (pe.sqlState == ApiError.PSQL_CONFLICT_ERR_CODE) {
                            span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                            call.respond(
                                HttpStatusCode.Conflict,
                                ApiError.conflictError(
                                    detail = "Ein Bookmark mit diesem Namen existiert bereits.",
                                ),
                            )
                        } else {
                            span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(
                                    detail = "Ein interner Datenbankfehler ist aufgetreten.",
                                ),
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
             * Delete Bookmark Template Pair.
             */
            delete {
                val span =
                    tracer
                        .spanBuilder("lori.LoriService.DELETE/api/v1/bookmarktemplates")
                        .setSpanKind(SpanKind.SERVER)
                        .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val bookmarkId: Int? = call.request.queryParameters["bookmarkid"]?.toInt()
                        val rightId: String? = call.request.queryParameters["rightid"]
                        span.setAttribute("bookmarkId", bookmarkId?.toString() ?: "null")
                        if (bookmarkId == null || rightId == null) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: No valid id has been provided in the query parameters.",
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError.badRequestError(
                                    detail = "templateId oder bookmarkId fehlen in Queryparametern oder sind nicht numerisch",
                                ),
                            )
                        } else {
                            val entriesDeleted =
                                backend.deleteBookmarkTemplatePair(
                                    bookmarkId = bookmarkId,
                                    rightId = rightId,
                                )
                            if (entriesDeleted == 1) {
                                span.setStatus(StatusCode.OK)
                                call.respond(HttpStatusCode.OK)
                            } else {
                                span.setStatus(StatusCode.ERROR)
                                call.respond(
                                    HttpStatusCode.NotFound,
                                    ApiError.notFoundError(
                                        detail = "Für die BookmarkId $bookmarkId und TemplateId $rightId existiert kein Eintrag.",
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

            route("/batch") {
                delete {
                    val span: Span =
                        tracer
                            .spanBuilder("lori.LoriService.DELETE/api/v1/bookmarktemplates/batch")
                            .setSpanKind(SpanKind.SERVER)
                            .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val bookmarkTemplatePairs: BookmarkTemplateBatchRest =
                                call.receive(BookmarkTemplateBatchRest::class)
                            span.setAttribute("BookmarkTemplate Pairs", bookmarkTemplatePairs.toString())
                            val deletedItems: Int =
                                backend.deleteBookmarkTemplatePairs(
                                    bookmarkTemplatePairs.batch?.map { it.toBusiness() }
                                        ?: emptyList(),
                                )
                            call.respond(HttpStatusCode.OK, deletedItems)
                        } catch (e: Exception) {
                            span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(
                                    detail = "Ein interner Datenbankfehler ist aufgetreten.",
                                ),
                            )
                        }
                    }
                }

                post {
                    val span: Span =
                        tracer
                            .spanBuilder("lori.LoriService.POST/api/v1/bookmarktemplates/batch")
                            .setSpanKind(SpanKind.SERVER)
                            .startSpan()
                    withContext(span.asContextElement()) {
                        try {
                            val bookmarkTemplatePairs: BookmarkTemplateBatchRest =
                                call.receive(BookmarkTemplateBatchRest::class)
                            span.setAttribute("BookmarkTemplate Pairs", bookmarkTemplatePairs.toString())
                            val createdEntries: List<BookmarkTemplate> =
                                backend.upsertBookmarkTemplatePairs(
                                    bookmarkTemplatePairs.batch?.map { it.toBusiness() }
                                        ?: emptyList(),
                                )
                            call.respond(HttpStatusCode.Created, createdEntries.map { it.toRest() })
                        } catch (e: Exception) {
                            span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError.internalServerError(
                                    detail = "Ein interner Datenbankfehler ist aufgetreten.",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
