package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfigurations
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.persistence.lori.server.FlywayMigrator
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
object LoriServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting the AccessServer :)")

        val config = LoriConfigurations.serverConfig
        val backend = LoriServerBackend(config)

        // Migrate DB
        FlywayMigrator(config).migrate()

        ServicePoolWithProbes(
            config = config,
            services = listOf(
                GrpcServer(
                    port = config.grpcPort,
                    services = listOf(
                        LoriGrpcServer(config, backend),
                    ),
                )
            ),
            backend = backend,
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(LoriServer::class.java)
}
