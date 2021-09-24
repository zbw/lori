package de.zbw.api.auth.server

import de.zbw.api.auth.server.config.AuthConfigurations
import de.zbw.persistence.auth.server.FlywayMigrator
import org.slf4j.LoggerFactory

/**
 * The Auth-Server.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object AuthServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting the AuthServer :)")

        val config = AuthConfigurations.serverConfig
        FlywayMigrator(config).migrate()

        ServicePoolWithProbes(
            config = config,
            services = listOf(
                GrpcServer(
                    port = config.grpcPort,
                    services = listOf(
                        AuthGrpcServer(),
                    ),
                )
            ),
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(AuthServer::class.java)
}
