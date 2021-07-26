package de.zbw.api.access.client

import de.zbw.access.api.AccessServiceGrpcKt
import de.zbw.access.api.AddAccessInformationRequest
import de.zbw.access.api.AddAccessInformationResponse
import de.zbw.access.api.GetAccessInformationRequest
import de.zbw.access.api.GetAccessInformationResponse
import de.zbw.api.access.client.config.AccessClientConfiguration
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
class AccessClient(
    val configuration: AccessClientConfiguration,
    val channel: Channel = ManagedChannelBuilder.forAddress(configuration.address, configuration.port).build(),
    val stub: AccessServiceGrpcKt.AccessServiceCoroutineStub = AccessServiceGrpcKt.AccessServiceCoroutineStub(channel)
) {

    suspend fun addAccessInformation(request: AddAccessInformationRequest): AddAccessInformationResponse =
        runWithTracing("client_addAccessInformation") { s: AccessServiceGrpcKt.AccessServiceCoroutineStub ->
            s.addAccessInformation(request)
        }

    suspend fun getAccessInformation(request: GetAccessInformationRequest): GetAccessInformationResponse =
        runWithTracing("client_getAccessInformation") { s: AccessServiceGrpcKt.AccessServiceCoroutineStub ->
            s.getAccessInformation(request)
        }

    private suspend fun <T> runWithTracing(
        msg: String,
        op: suspend (AccessServiceGrpcKt.AccessServiceCoroutineStub) -> T
    ): T {
        // Add some reasonable tracing framework here...
        LOG.info(msg)
        return op.invoke(stub.withDeadlineAfter(configuration.deadlineInMilli, TimeUnit.MILLISECONDS))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AccessClient::class.java)
    }
}
