package de.zbw.api.helloworld.server

import de.zbw.api.helloworld.server.config.HelloWorldConfigurations
import org.slf4j.LoggerFactory

/**
 * The HelloWorld-Server.
 *
 * An example implementation of a microservice that shall serve
 * as blueprint for future ones.
 *
 * Created on 04-19-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object HelloWorldServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting the HelloWorldServer :)")

        val configs = HelloWorldConfigurations.serverConfig

        ServicePoolWithProbes(
            port = configs.httpPort,
            services = listOf(
                GrpcServer(
                    port = configs.grpcPort,
                    services = listOf(
                        HelloWorldGrpcServer(),
                    ),
                )
            ),
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(HelloWorldServer::class.java)
}
