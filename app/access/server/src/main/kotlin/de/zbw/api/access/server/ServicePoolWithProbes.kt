package de.zbw.api.access.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine

/**
 * A pool for services.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ServicePoolWithProbes(
    private val services: List<ServiceLifecycle>,
    private val port: Int,
) : ServiceLifecycle() {

    private var server: NettyApplicationEngine = embeddedServer(
        Netty,
        port = port,
        module = application()
    )

    // This method is a hack due to ktors extension based design. It makes
    // testing a lot easier here.
    internal fun getHttpServer(): NettyApplicationEngine = server

    internal fun application(): Application.() -> Unit = {
        install(ContentNegotiation) { gson { } }
        install(CallLogging)
        routing {
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
