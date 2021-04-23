package de.zbw.api.helloworld.server

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * A pool for services.
 *
 * Created on 04-20-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ServicePoolWithProbes(private val services: List<ServiceLifecycle>) : ServiceLifecycle() {

    val server: NettyApplicationEngine = embeddedServer(Netty, port = 8080) {
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
        server.start(wait = true)
    }

    override fun stop() {
        services.forEach {
            it.stop()
        }
    }
}
