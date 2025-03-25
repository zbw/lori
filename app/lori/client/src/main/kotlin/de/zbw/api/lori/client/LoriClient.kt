package de.zbw.api.lori.client

import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.CheckForRightErrorsRequest
import de.zbw.lori.api.CheckForRightErrorsResponse
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.LoriServiceGrpcKt
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * A client for the Lori-Service.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriClient(
    val configuration: LoriClientConfiguration,
    val channel: Channel =
        ManagedChannelBuilder
            .forTarget("${configuration.address}:${configuration.port}")
            .defaultLoadBalancingPolicy("round_robin")
            .usePlaintext()
            .build(),
    val stub: LoriServiceGrpcKt.LoriServiceCoroutineStub = LoriServiceGrpcKt.LoriServiceCoroutineStub(channel),
    private val openTelemetry: OpenTelemetry = OpenTelemetry.noop(),
    private val tracer: Tracer = openTelemetry.getTracer("de.zbw.api.lori.client.LoriClient"),
) {
    suspend fun applyTemplates(request: ApplyTemplatesRequest): ApplyTemplatesResponse =
        runWithTracing("client_applyTemplates") { s: LoriServiceGrpcKt.LoriServiceCoroutineStub ->
            s.applyTemplates(request)
        }

    suspend fun fullImport(request: FullImportRequest): FullImportResponse =
        runWithTracing("client_fullImport") { s: LoriServiceGrpcKt.LoriServiceCoroutineStub ->
            s.fullImport(request)
        }

    suspend fun checkForErrors(request: CheckForRightErrorsRequest): CheckForRightErrorsResponse =
        runWithTracing("client_checkForErrors") { s: LoriServiceGrpcKt.LoriServiceCoroutineStub ->
            s.checkForRightErrors(request)
        }

    private suspend fun <T> runWithTracing(
        spanName: String,
        op: suspend (LoriServiceGrpcKt.LoriServiceCoroutineStub) -> T,
    ): T {
        val span =
            tracer
                .spanBuilder(spanName)
                .setSpanKind(SpanKind.CLIENT)
                .startSpan()
        return withContext(span.asContextElement()) {
            try {
                op.invoke(stub.withDeadlineAfter(configuration.deadlineInMilli, TimeUnit.MILLISECONDS))
            } catch (t: Throwable) {
                span.setStatus(StatusCode.ERROR, t.message ?: t.cause.toString())
                throw t
            } finally {
                span.end()
            }
        }
    }
}
