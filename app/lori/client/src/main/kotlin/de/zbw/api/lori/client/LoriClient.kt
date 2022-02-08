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
import org.slf4j.LoggerFactory
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
    val stub: LoriServiceGrpcKt.LoriServiceCoroutineStub = LoriServiceGrpcKt.LoriServiceCoroutineStub(channel)
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
        msg: String,
        op: suspend (LoriServiceGrpcKt.LoriServiceCoroutineStub) -> T
    ): T {
        // Add some reasonable tracing framework here...
        LOG.info(msg)
        return op.invoke(stub.withDeadlineAfter(configuration.deadlineInMilli, TimeUnit.MILLISECONDS))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(LoriClient::class.java)
    }
}
