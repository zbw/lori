package de.zbw.job.loriiimport

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Trigger a full import.
 *
 * Created on 02-03-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
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

        val loriClient = LoriClient(
            configuration = LoriClientConfiguration(9092, "lori", 5000),
            openTelemetry = openTelemetry,
        )

        val response: FullImportResponse = runBlocking {
            loriClient.fullImport(FullImportRequest.getDefaultInstance())
        }
        LOG.info("Lori Server imported: ${response.itemsImported}")
    }

    private val LOG = LoggerFactory.getLogger(Main::class.java)
}
