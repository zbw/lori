package de.zbw.job.materialized

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.job.templateapply.config.LoriConfigurations
import de.zbw.lori.api.RefreshMaterializedViewsRequest
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager

/**
 * Refresh materialized views
 *
 * Created on 01-12-2026.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val openTelemetry = OpenTelemetry.noop()
        val tracer = openTelemetry.getTracer("de.zbw.job.materialized.Main")
        val config = LoriConfigurations.serverConfig

        val span =
            tracer
                .spanBuilder("main")
                .setSpanKind(SpanKind.CLIENT)
                .startSpan()

        val loriClient =
            LoriClient(
                configuration =
                    LoriClientConfiguration(
                        config.loriGrpcPort,
                        config.loriAddress,
                        config.loriClientDeadline,
                    ),
            )

        runBlocking {
            try {
                withContext(span.asContextElement()) {
                    LOG.info("Start refresh materialized views:")
                    val response =
                        loriClient.refreshMaterializedViews(
                            RefreshMaterializedViewsRequest.getDefaultInstance(),
                        )
                    LOG.info("-------------------------------")
                    if (response.isSuccessful) {
                        LOG.info("Refreshing was successful")
                        LOG.info("Time in ms: ${response.durationInMs}")
                    } else {
                        LOG.info("Refreshing was not successful")
                        LOG.info("Errors message: ${response.errorMessage}")
                    }
                }
            } catch (e: Exception) {
                LOG.error("An error was thrown while refreshing: ${e.message}")
                LOG.error("Stacktrace: ${e.printStackTrace()}")
                throw e
            } finally {
                span.end()
            }
        }
    }

    private val LOG = LogManager.getLogger(Main::class.java)
}
