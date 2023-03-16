package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.BookmarkIdCreated
import de.zbw.lori.model.BookmarkRest
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
 * REST-API routes for search bookmarks.
 *
 * Created on 03-15-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.bookmarkRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/bookmark") {
        /**
         * Return Bookmark for a given id.
         */
        get("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.GET/api/v1/bookmark/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val bookmarkId = call.parameters["id"]?.toInt()
                    span.setAttribute("bookmarkId", bookmarkId?.toString() ?: "null")
                    if (bookmarkId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        val bookmark: BookmarkRest? = backend.getBookmarkById(bookmarkId)
                        bookmark?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(bookmark)
                        } ?: let {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError.notFoundError(
                                    detail = "Für das Bookmark mit Id: $bookmarkId existiert kein Eintrag.",
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
                .spanBuilder("lori.LoriService.POST/api/v1/bookmark")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val bookmark: BookmarkRest = call.receive(BookmarkRest::class)
                    span.setAttribute("bookmark", bookmark.toString())
                    val pk = backend.insertBookmark(bookmark)
                    span.setStatus(StatusCode.OK)
                    call.respond(HttpStatusCode.Created, BookmarkIdCreated(pk))
                } catch (pe: PSQLException) {
                    if (pe.sqlState == ApiError.PSQL_CONFLICT_ERR_CODE) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(
                            HttpStatusCode.Conflict,
                            ApiError.conflictError(
                                detail = "Ein Bookmark mit diesem Namen existiert bereits.",
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
         * Update an existing Bookmark.
         */
        put {
            val span = tracer
                .spanBuilder("lori.LoriService.PUT/api/v1/bookmark")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val bookmark: BookmarkRest = call.receive(BookmarkRest::class)
                        .takeIf { it.bookmarkName != null && it.bookmarkId != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("bookmark", bookmark.toString())
                    val insertedRows = backend.updateBookmark(bookmark.bookmarkId!!, bookmark)
                    if (insertedRows == 1) {
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        span.setStatus(StatusCode.ERROR)
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiError.notFoundError(
                                detail = "Für das Bookmark mit Id ${bookmark.bookmarkId} existiert kein Eintrag.",
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
         * Delete Bookmark by Id.
         */
        delete("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.DELETE/api/v1/bookmark/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val bookmarkId = call.parameters["id"]?.toInt()
                    span.setAttribute("bookmarkId", bookmarkId?.toString() ?: "null")
                    if (bookmarkId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError.badRequestError(
                                detail = "Keine valide numerische Id wurde übergeben",
                            ),
                        )
                    } else {
                        val entriesDeleted = backend.deleteBookmark(bookmarkId)
                        if (entriesDeleted == 1) {
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiError.notFoundError(
                                    detail = "Für die BookmarkId $bookmarkId existiert kein Eintrag.",
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
