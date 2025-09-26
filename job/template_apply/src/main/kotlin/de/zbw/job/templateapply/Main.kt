package de.zbw.job.templateapply

import com.google.protobuf.Timestamp
import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.CheckForRightErrorsRequest
import de.zbw.lori.api.CheckForRightErrorsResponse
import de.zbw.lori.api.CleanDownloadsRequest
import de.zbw.lori.api.CleanDownloadsResponse
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Apply all templates.
 *
 * Created on 07-28-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val openTelemetry = OpenTelemetry.noop()
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
                        "localhost",
                        // Wait for one hour max. Anything above that is at least worth investigating.
                        3600000,
                    ),
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
                                .setSkipDraft(true)
                                .build(),
                        )
                    span.setAttribute("Templates Applied", response.templateApplicationsList.toString())
                    LOG.info("Application procedure was successful")
                    response.templateApplicationsList.forEach {
                        LOG.info("-------------------------------")
                        LOG.info("Applied template ${it.templateName} (ID: ${it.rightId}): ${it.numberAppliedEntries}")
                        LOG.info("Errors found: ${it.numberOfErrors}")
                    }
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
                    LOG.info("-------------------------------")
                    LOG.info("Start checking for errors:")
                    val response: CheckForRightErrorsResponse =
                        loriClient.checkForErrors(
                            CheckForRightErrorsRequest.getDefaultInstance(),
                        )
                    span.setAttribute("Number of errors found", response.numberOfErrors.toLong())
                    LOG.info("Checking for errors procedure was successful; Found ${response.numberOfErrors} errors.")
                }
            } catch (e: Exception) {
                LOG.error("An error occurred on error checking procedure: ${e.message}")
                LOG.error("Stacktrace: ${e.printStackTrace()}")
                throw e
            } finally {
                span.end()
            }

            runBlocking {
                try {
                    withContext(span.asContextElement()) {
                        LOG.info("-------------------------------")
                        LOG.info("Start cleaning download older than 1 day")
                        val cutoffInstant = Instant.now().minus(1, ChronoUnit.DAYS)
                        val response: CleanDownloadsResponse =
                            loriClient.cleanDownloads(
                                CleanDownloadsRequest
                                    .newBuilder()
                                    .setOlderThan(
                                        Timestamp
                                            .newBuilder()
                                            .setNanos(cutoffInstant.nano)
                                            .build(),
                                    ).build(),
                            )
                        span.setAttribute("Number of deletions", response.deletedCount.toLong())
                        LOG.info("Deleting downloads was successful; Deleted ${response.deletedCount} downloads.")
                    }
                } catch (e: Exception) {
                    LOG.error("An error occurred on when deleting downloads: ${e.message}")
                    LOG.error("Stacktrace: ${e.printStackTrace()}")
                    throw e
                } finally {
                    span.end()
                }
            }
        }
    }

    private val LOG = LogManager.getLogger(Main::class.java)
}
