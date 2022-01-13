package de.zbw.api.access.client

import de.zbw.access.api.AddItemRequest
import de.zbw.access.api.AddItemResponse
import de.zbw.access.api.GetItemRequest
import de.zbw.access.api.GetItemResponse
import de.zbw.access.api.LoriServiceGrpcKt
import de.zbw.api.access.client.config.LoriClientConfiguration
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
    val channel: Channel = ManagedChannelBuilder.forAddress(configuration.address, configuration.port).build(),
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
