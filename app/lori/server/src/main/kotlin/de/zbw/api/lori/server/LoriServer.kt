package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfigurations
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.persistence.lori.server.FlywayMigrator
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
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

        val tracer: Tracer = AutoConfiguredOpenTelemetrySdk
            .initialize()
            .openTelemetrySdk
            .getTracer("de.zbw.api.lori.server.LoriServer")

        val config = LoriConfigurations.serverConfig
        val backend = LoriServerBackend(config, tracer)
        // Migrate DB
        FlywayMigrator(config).migrate()

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
                )
            ),
            backend = backend,
            tracer = tracer,
        ).apply {
            start()
        }
    }

    private val LOG = LoggerFactory.getLogger(LoriServer::class.java)
}
