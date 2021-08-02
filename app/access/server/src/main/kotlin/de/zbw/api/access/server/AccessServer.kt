package de.zbw.api.access.server

import de.zbw.api.access.server.config.AccessConfigurations
import de.zbw.business.access.server.AccessServerBackend
import de.zbw.persistence.access.server.FlywayMigrator
import org.slf4j.LoggerFactory

/**
 * The Access-Server.
 *
 * Manages access rights. Provides an REST-API
 * for managing access rights.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object AccessServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting the AccessServer :)")

        val config = AccessConfigurations.serverConfig
        val backend = AccessServerBackend(config)

        // Migrate DB
        FlywayMigrator(config).migrate()

        ServicePoolWithProbes(
            config = config,
            services = listOf(
                GrpcServer(
                    port = config.grpcPort,
                    services = listOf(
                        AccessGrpcServer(config, backend),
                    ),
                )
            ),
            backend = backend,
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(AccessServer::class.java)
}
