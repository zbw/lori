package de.zbw.job.loriiimport

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
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
        val tracer: Tracer =
            OpenTelemetry.noop().getTracer("foo")

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
                        // Wait for four hour max. Anything above that is at least worth investigating.
                        4 * 3600000,
                    ),
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
