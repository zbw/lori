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
    private val port: Int = 9092,
    private val services: List<BindableService>,
    private val server: Server = ServerBuilder
        .forPort(port)
        .addServices(services.map { it.bindService() })
        .addService(ProtoReflectionService.newInstance())
        .build()
) : ServiceLifecycle() {

    private var isReady = false

    override fun isReady(): Boolean = isReady

    override fun isHealthy(): Boolean = true

    override fun start() {
        LOG.info("Start GRPC Server on port $port")
        server.start()
        isReady = true
        Runtime.getRuntime().addShutdownHook(
            Thread {
                server.shutdown()
            }
        )
    }

    override fun stop() {
        isReady = false
        server.shutdown()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GrpcServer::class.java)
    }
}
