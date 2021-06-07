package de.zbw.api.handle.server

import de.zbw.api.handle.server.config.HandleConfigurations
import de.zbw.business.handle.server.HandleCommunicator
import org.slf4j.LoggerFactory

/**
 * The HelloWorld-Server.
 *
 * An example implementation of a microservice that shall serve
 * as blueprint for future ones.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object HandleServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting the HandleServer :)")

        val config = HandleConfigurations.serverConfig
        val communicator = HandleCommunicator(config)

        ServicePoolWithProbes(
            port = config.httpPort,
            services = listOf(
                GrpcServer(
                    port = config.grpcPort,
                    services = listOf(
                        HandleGrpcServer(
                            communicator = communicator,
                        ),
                    ),
                ),
                HandleServiceLifecycle(
                    config,
                    communicator
                ),
            ),
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(HandleServer::class.java)
}
