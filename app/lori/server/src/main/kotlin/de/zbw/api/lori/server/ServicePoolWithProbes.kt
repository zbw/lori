package de.zbw.api.lori.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.route.bookmarkRoutes
import de.zbw.api.lori.server.route.bookmarkTemplateRoutes
import de.zbw.api.lori.server.route.groupRoutes
import de.zbw.api.lori.server.route.guiRoutes
import de.zbw.api.lori.server.route.itemRoutes
import de.zbw.api.lori.server.route.metadataRoutes
import de.zbw.api.lori.server.route.rightRoutes
import de.zbw.api.lori.server.route.staticRoutes
import de.zbw.api.lori.server.route.templateRoutes
import de.zbw.api.lori.server.route.usersRoutes
import de.zbw.api.lori.server.type.UserSession
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.session
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.opentelemetry.api.trace.Tracer
import org.slf4j.event.Level
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A pool for services.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ServicePoolWithProbes(
    private val services: List<ServiceLifecycle>,
    val config: LoriConfiguration,
    private val backend: LoriServerBackend,
    private val tracer: Tracer,
) : ServiceLifecycle() {

    private var server: NettyApplicationEngine = embeddedServer(
        Netty,
        port = config.httpPort,
        module = application(),
    )

    // This method is a hack due to ktors extension based design. It makes
    // testing a lot easier here.
    internal fun getHttpServer(): NettyApplicationEngine = server

    internal fun application(): Application.() -> Unit = {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                registerTypeAdapter(
                    OffsetDateTime::class.java,
                    JsonDeserializer { json, _, _ ->
                        ZonedDateTime.parse(json.asString, DateTimeFormatter.ISO_ZONED_DATE_TIME)
                            .toOffsetDateTime()
                    }
                )
                registerTypeAdapter(
                    OffsetDateTime::class.java,
                    JsonSerializer<OffsetDateTime> { obj, _, _ ->
                        JsonPrimitive(obj.toString())
                    }
                )
                registerTypeAdapter(
                    LocalDate::class.java,
                    JsonDeserializer { json, _, _ ->
                        LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                )
                registerTypeAdapter(
                    LocalDate::class.java,
                    JsonSerializer<LocalDate> { obj, _, _ ->
                        JsonPrimitive(obj.toString())
                    }
                )
            }
        }
        install(CallLogging) {
            level = Level.INFO
        }
        install(Authentication) {
            session<UserSession>("auth-session") {
                validate { session: UserSession ->
                    backend.getSessionById(session.sessionId)
                        ?.takeIf { s ->
                            s.validUntil > Instant.now() && (s.role == UserRole.READWRITE || s.role == UserRole.ADMIN)
                        }?.let { session }
                }
                challenge {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                    )
                }
            }
            session<UserSession>("auth-login") {
                validate { session: UserSession ->
                    backend.getSessionById(session.sessionId)
                        ?.takeIf { s ->
                            s.validUntil > Instant.now()
                        }?.let { session }
                }
                challenge {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                    )
                }
            }
            jwt("auth-jwt") {
                realm = config.jwtRealm
                verifier(
                    JWT
                        .require(Algorithm.HMAC256(config.jwtSecret))
                        .withAudience(config.jwtAudience)
                        .withIssuer(config.jwtIssuer)
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("username").asString() != "") {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
                challenge { _, _ ->
                    call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
                }
            }
        }

        install(Sessions) {
            cookie<UserSession>("JSESSIONID") {
                cookie.path = "/"
                cookie.maxAgeInSeconds = 60 * 60 * 24
                cookie.extensions["SameSite"] = "lax"
            }
        }
        routing {
            singlePageApplication {
                useResources = true
                filesPath = "dist/"
                defaultPage = "index.html"
            }

            get("/ready") {
                if (isReady()) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            get("/healthz") {
                if (isHealthy()) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            bookmarkRoutes(backend, tracer)
            bookmarkTemplateRoutes(backend, tracer)
            groupRoutes(backend, tracer)
            guiRoutes(backend, tracer)
            itemRoutes(backend, tracer)
            metadataRoutes(backend, tracer)
            rightRoutes(backend, tracer)
            usersRoutes(backend, tracer)
            templateRoutes(backend, tracer)
            staticRoutes()
        }
    }

    override fun isReady(): Boolean =
        services.map {
            it.isReady()
        }.all { it }

    override fun isHealthy(): Boolean =
        services.map {
            it.isHealthy()
        }.all { it }

    override fun start() {
        services.forEach {
            it.start()
        }
        getHttpServer().start(wait = true)
    }

    override fun stop() {
        services.forEach {
            it.stop()
        }
        getHttpServer().stop(1000, 2000)
    }
}
