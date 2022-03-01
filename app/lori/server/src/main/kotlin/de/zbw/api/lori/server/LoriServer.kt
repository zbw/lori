package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfigurations
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.persistence.lori.server.FlywayMigrator
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
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

        val sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter
                        .builder()
                        .setEndpoint("http://localhost:4317")
                        .build()
                )
                    .build()
            )
            .build()

        val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal()

        val tracer: Tracer = openTelemetry.getTracer("de.zbw.api.lori.server.LoriServer")

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
