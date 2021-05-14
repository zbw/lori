package de.zbw.api.handle.server

import de.zbw.api.handle.server.config.HandleConfigurations
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

        val configs = HandleConfigurations.serverConfig

        ServicePoolWithProbes(
            port = configs.httpPort,
            services = listOf(
                GrpcServer(
                    port = configs.grpcPort,
                    services = listOf(
                        HandleGrpcServer(configs),
                    ),
                )
            ),
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(HandleServer::class.java)
}
