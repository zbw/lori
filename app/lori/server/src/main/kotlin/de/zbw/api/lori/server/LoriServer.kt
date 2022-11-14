package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfigurations
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.persistence.lori.server.FlywayMigrator
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import org.apache.logging.log4j.LogManager

/**
 * Lori-Server.
 *
 * Manages access rights of bibliographic entities.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object LoriServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting LoriServer :)")

        val tracer: Tracer = AutoConfiguredOpenTelemetrySdk
            .initialize()
            .openTelemetrySdk
            .getTracer("de.zbw.api.lori.server.LoriServer")

        val config = LoriConfigurations.serverConfig
        val backend = LoriServerBackend(config, tracer)

        // Migrate DB
        FlywayMigrator(config).migrate()

        // TODO: Add Service for DB connection test
        ServicePoolWithProbes(
            config = config,
            services = listOf(
                GrpcServer(
                    port = config.grpcPort,
                    services = listOf(
                        LoriGrpcServer(
                            config = config,
                            backend = backend,
                            tracer = tracer
                        ),
                    ),
                ),
            ),
            backend = backend,
            tracer = tracer,
        ).apply {
            start()
        }
    }

    private val LOG = LogManager.getLogger(LoriServer::class.java)
}
