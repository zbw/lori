package de.zbw.job.loriiimport

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager

/**
 * Trigger a full import.
 *
 * Created on 02-03-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val openTelemetry =
            AutoConfiguredOpenTelemetrySdk
                .initialize()
                .openTelemetrySdk
        val tracer = openTelemetry.getTracer("de.zbw.job.loriiimport.Main")

        val span =
            tracer
                .spanBuilder("main")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan()

        val loriClient =
            LoriClient(
                configuration =
                    LoriClientConfiguration(
                        9092,
                        "lori",
                        // Wait for one hour max. Anything above that is at least worth investigating.
                        3600000,
                    ),
                openTelemetry = openTelemetry,
            )

        runBlocking {
            try {
                withContext(span.asContextElement()) {
                    LOG.info("Start full import:")
                    val response: FullImportResponse = loriClient.fullImport(FullImportRequest.getDefaultInstance())
                    span.setAttribute("Items Imported", response.itemsImported.toLong())
                }
            } catch (e: Exception) {
                LOG.error("An error occurred on full import procedure: ${e.message}")
                LOG.error("Stacktrace: ${e.printStackTrace()}")
                throw e
            } finally {
                span.end()
            }
        }
    }

    private val LOG = LogManager.getLogger(Main::class.java)
}
