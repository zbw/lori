package de.zbw.api.handle.client

import de.zbw.api.handle.client.config.HandleClientConfiguration
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleResponse
import de.zbw.handle.api.AddHandleValuesRequest
import de.zbw.handle.api.AddHandleValuesResponse
import de.zbw.handle.api.DeleteHandleRequest
import de.zbw.handle.api.DeleteHandleResponse
import de.zbw.handle.api.HandleServiceGrpcKt
import de.zbw.handle.api.ModifyHandleValuesRequest
import de.zbw.handle.api.ModifyHandleValuesResponse
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * A client for the Asset-Service.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleClient(
    val configuration: HandleClientConfiguration,
    val channel: Channel = ManagedChannelBuilder.forAddress(configuration.address, configuration.port).build(),
    val stub: HandleServiceGrpcKt.HandleServiceCoroutineStub = HandleServiceGrpcKt.HandleServiceCoroutineStub(channel)
) {

    suspend fun addHandle(request: AddHandleRequest): AddHandleResponse =
        runWithTracing("client_addHandle") { s: HandleServiceGrpcKt.HandleServiceCoroutineStub ->
            s.addHandle(request)
        }

    suspend fun addHandleValues(request: AddHandleValuesRequest): AddHandleValuesResponse =
        runWithTracing("client_addHandle") { s: HandleServiceGrpcKt.HandleServiceCoroutineStub ->
            s.addHandleValues(request)
        }

    suspend fun deleteHandle(request: DeleteHandleRequest): DeleteHandleResponse =
        runWithTracing("client_deleteHandle") { s: HandleServiceGrpcKt.HandleServiceCoroutineStub ->
            s.deleteHandle(request)
        }

    suspend fun modifyHandleValue(request: ModifyHandleValuesRequest): ModifyHandleValuesResponse =
        runWithTracing("client_modifyHandles") { s: HandleServiceGrpcKt.HandleServiceCoroutineStub ->
            s.modifyHandleValues(request)
        }

    private suspend fun <T> runWithTracing(
        msg: String,
        op: suspend (HandleServiceGrpcKt.HandleServiceCoroutineStub) -> T
    ): T {
        // Add some reasonable tracing framework here...
        LOG.info(msg)
        return op.invoke(stub.withDeadlineAfter(configuration.deadlineInMilli, TimeUnit.MILLISECONDS))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HandleClient::class.java)
    }
}
