package de.zbw.api.helloworld.server

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import org.slf4j.LoggerFactory

/**
 * A grpc server.
 *
 * Created on 04-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GrpcServer(
    private val port: Int,
    private val services: List<BindableService>,
    private val server: Server = ServerBuilder
        .forPort(port)
        .addServices(services.map { it.bindService() })
        .addService(ProtoReflectionService.newInstance())
        .build()
) : ServiceLifecycle() {

    private var ready = false
    private var healthy = false

    override fun isReady(): Boolean = ready

    override fun isHealthy(): Boolean = healthy

    override fun start() {
        LOG.info("Start GRPC Server on port $port")
        server.start()
        ready = true
        healthy = true
        Runtime.getRuntime().addShutdownHook(
            Thread {
                server.shutdown()
            }
        )
    }

    override fun stop() {
        ready = false
        healthy = false
        server.shutdown()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GrpcServer::class.java)
    }
}
