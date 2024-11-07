package de.zbw.job.templateapply

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.CheckForRightErrorsRequest
import de.zbw.lori.api.CheckForRightErrorsResponse
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager

/**
 * Apply all templates.
 *
 * Created on 07-28-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val openTelemetry =
            AutoConfiguredOpenTelemetrySdk
                .initialize()
                .openTelemetrySdk
        val tracer = openTelemetry.getTracer("de.zbw.job.templateapply.Main")

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
                    LOG.info("Start applying templates:")
                    val response: ApplyTemplatesResponse =
                        loriClient.applyTemplates(
                            ApplyTemplatesRequest
                                .newBuilder()
                                .setAll(true)
                                .build(),
                        )
                    span.setAttribute("Templates Applied", response.templateApplicationsList.toString())
                    LOG.info("Application procedure was successful")
                }
            } catch (e: Exception) {
                LOG.error("An error occurred on template application procedure: ${e.message}")
                LOG.error("Stacktrace: ${e.printStackTrace()}")
                throw e
            } finally {
                span.end()
            }
        }

        runBlocking {
            try {
                withContext(span.asContextElement()) {
                    LOG.info("Start checking for errors:")
                    val response: CheckForRightErrorsResponse =
                        loriClient.checkForErrors(
                            CheckForRightErrorsRequest.getDefaultInstance(),
                        )
                    span.setAttribute("Number of errors found", response.errorsCount.toString())
                    LOG.info("Checking for errors procedure was successful; Found ${response.errorsCount} errors.")
                }
            } catch (e: Exception) {
                LOG.error("An error occurred on error checking procedure: ${e.message}")
                LOG.error("Stacktrace: ${e.printStackTrace()}")
                throw e
            } finally {
                span.end()
            }
        }
    }

    private val LOG = LogManager.getLogger(Main::class.java)
}
