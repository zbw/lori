package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.exception.ResourceStillInUseException
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.Group
import de.zbw.lori.model.ErrorRest
import de.zbw.lori.model.GroupIdCreated
import de.zbw.lori.model.GroupRest
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
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import org.postgresql.util.PSQLException

/**
 * REST-API routes for groups.
 *
 * Created on 11-09-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Routing.groupRoutes(
    backend: LoriServerBackend,
    tracer: Tracer,
) {
    route("/api/v1/group") {
        /**
         * Insert a new Group.
         */
        post {
            val span = tracer
                .spanBuilder("lori.LoriService.POST/api/v1/group")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val group: GroupRest = call.receive(GroupRest::class)
                        .takeIf { it.name != null && it.ipAddresses != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("group", group.toString())
                    val pk = backend.insertGroup(group.toBusiness())
                    span.setStatus(StatusCode.OK)
                    call.respond(HttpStatusCode.Created, GroupIdCreated(pk))
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                } catch (iae: IllegalArgumentException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${iae.message}")
                    call.respond(HttpStatusCode.BadRequest, "CSV has the wrong number of columns.")
                } catch (pe: PSQLException) {
                    if (pe.sqlState == "23505") {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(HttpStatusCode.Conflict, "A group with this name already exists.")
                    } else {
                        span.setStatus(StatusCode.ERROR, "Exception: ${pe.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        /**
         * Update an existing Group.
         */
        put {
            val span = tracer
                .spanBuilder("lori.LoriService.PUT/api/v1/group")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    @Suppress("SENSELESS_COMPARISON")
                    val group: GroupRest = call.receive(GroupRest::class)
                        .takeIf { it.name != null && it.ipAddresses != null }
                        ?: throw BadRequestException("Invalid Json has been provided")
                    span.setAttribute("group", group.toString())
                    val insertedRows = backend.updateGroup(group.toBusiness())
                    if (insertedRows == 1) {
                        span.setStatus(StatusCode.OK)
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        span.setStatus(StatusCode.ERROR)
                        call.respond(HttpStatusCode.NotFound)
                    }
                } catch (iae: IllegalArgumentException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${iae.message}")
                    call.respond(HttpStatusCode.BadRequest, "CSV has the wrong number of columns.")
                } catch (e: BadRequestException) {
                    span.setStatus(StatusCode.ERROR, "BadRequest: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Invalid input")
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred.")
                } finally {
                    span.end()
                }
            }
        }

        /**
         * Return Group for a given id.
         */
        get("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.GET/api/v1/group/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val groupId = call.parameters["id"]
                    span.setAttribute("groupId", groupId ?: "null")
                    if (groupId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        val group: Group? = backend.getGroupById(groupId)
                        group?.let {
                            span.setStatus(StatusCode.OK)
                            call.respond(group.toRest())
                        } ?: let {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(HttpStatusCode.NotFound, "No item found for given id.")
                        }
                    }
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred: ${e.message}")
                } finally {
                    span.end()
                }
            }
        }

        /**
         * Delete Group by Id.
         */
        delete("{id}") {
            val span = tracer
                .spanBuilder("lori.LoriService.DELETE/api/v1/group/{id}")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
            withContext(span.asContextElement()) {
                try {
                    val groupId = call.parameters["id"]
                    span.setAttribute("groupId", groupId ?: "null")
                    if (groupId == null) {
                        span.setStatus(StatusCode.ERROR, "BadRequest: No valid id has been provided in the url.")
                        call.respond(HttpStatusCode.BadRequest, "No valid id has been provided in the url.")
                    } else {
                        val entriesDeleted = backend.deleteGroup(groupId)
                        if (entriesDeleted == 1) {
                            span.setStatus(StatusCode.OK)
                            call.respond(HttpStatusCode.OK)
                        } else {
                            span.setStatus(StatusCode.ERROR)
                            call.respond(HttpStatusCode.NotFound, "No item found for given id.")
                        }
                    }
                } catch (re: ResourceStillInUseException) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${re.message}")
                    call.respond(
                        HttpStatusCode.Conflict,
                        ErrorRest(
                            type = "/errors/resourcestillinuse",
                            title = "Gruppe konnte nicht gelöscht werden.",
                            detail = re.message,
                            status = "409",
                        ),
                    )
                } catch (e: Exception) {
                    span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "An internal error occurred: ${e.message}.")
                } finally {
                    span.end()
                }
            }
        }

        route("/list") {
            /**
             * Receive a list of groups.
             */
            get {
                val span = tracer
                    .spanBuilder("lori.LoriService.GET/api/v1/group/list")
                    .setSpanKind(SpanKind.SERVER)
                    .startSpan()
                withContext(span.asContextElement()) {
                    try {
                        val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: 100
                        val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: 0
                        if (limit < 1 || limit > 200) {
                            span.setStatus(
                                StatusCode.ERROR,
                                "BadRequest: Limit parameter is expected to be between 1 and 200."
                            )
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "Limit parameter is expected to be between 1 and 200."
                            )
                            return@withContext
                        }
                        val receivedGroups: List<Group> = backend.getGroupList(limit, offset)
                        span.setStatus(StatusCode.OK)
                        call.respond(receivedGroups.map { it.toRest() })
                    } catch (e: Exception) {
                        span.setStatus(StatusCode.ERROR, "Exception: ${e.message}")
                        call.respond(HttpStatusCode.InternalServerError, "An internal error occurred: ${e.message}")
                    } finally {
                        span.end()
                    }
                }
            }
        }
    }
}
