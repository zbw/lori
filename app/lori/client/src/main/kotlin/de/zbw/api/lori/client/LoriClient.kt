package de.zbw.api.lori.client

import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.AddItemRequest
import de.zbw.lori.api.AddItemResponse
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.GetItemRequest
import de.zbw.lori.api.GetItemResponse
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
 * A client for the HelloWorld-Service.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriClient(
    val configuration: LoriClientConfiguration,
    val channel: Channel = ManagedChannelBuilder.forTarget("${configuration.address}:${configuration.port}")
        .defaultLoadBalancingPolicy("round_robin")
        .usePlaintext()
        .build(),
    val stub: LoriServiceGrpcKt.LoriServiceCoroutineStub = LoriServiceGrpcKt.LoriServiceCoroutineStub(channel),
    private val openTelemetry: OpenTelemetry,
    private val tracer: Tracer = openTelemetry.getTracer("de.zbw.api.lori.client.LoriClient")
) {

    suspend fun addItem(request: AddItemRequest): AddItemResponse =
        runWithTracing("client_addAccessInformation") { s: LoriServiceGrpcKt.LoriServiceCoroutineStub ->
            s.addItem(request)
        }

    suspend fun getItem(request: GetItemRequest): GetItemResponse =
        runWithTracing("client_getAccessInformation") { s: LoriServiceGrpcKt.LoriServiceCoroutineStub ->
            s.getItem(request)
        }

    suspend fun fullImport(request: FullImportRequest): FullImportResponse =
        runWithTracing("client_fullImport") { s: LoriServiceGrpcKt.LoriServiceCoroutineStub ->
            s.fullImport(request)
        }

    private suspend fun <T> runWithTracing(
        spanName: String,
        op: suspend (LoriServiceGrpcKt.LoriServiceCoroutineStub) -> T
    ): T {

        val span = tracer
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
