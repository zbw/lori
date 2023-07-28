package de.zbw.job.templateapply

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Apply all templates.
 *
 * Created on 07-28-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {

        val openTelemetry = AutoConfiguredOpenTelemetrySdk
            .initialize()
            .openTelemetrySdk
        val tracer = openTelemetry.getTracer("de.zbw.job.templateapply.Main")

        val span = tracer
            .spanBuilder("main")
            .setSpanKind(SpanKind.CLIENT)
            .startSpan()

        val loriClient = LoriClient(
            configuration = LoriClientConfiguration(
                9092,
                "lori",
                3600000 // Wait for one hour max. Anything above that is at least worth investigating.
            ),
            openTelemetry = openTelemetry,
        )

        runBlocking {
            try {
                withContext(span.asContextElement()) {
                    val response: ApplyTemplatesResponse = loriClient.applyTemplates(
                        ApplyTemplatesRequest
                            .newBuilder()
                            .setAll(true)
                            .build()
                    )
                    span.setAttribute("Templates Applied", response.templateApplicationsList.toString())
                }
            } finally {
                span.end()
            }
        }
    }

    private val LOG = LoggerFactory.getLogger(Main::class.java)
}
