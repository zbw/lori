package de.zbw.api.handle.server

import de.zbw.api.handle.server.config.HandleConfiguration
import de.zbw.business.handle.server.HandleCommunicator
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleResponse
import de.zbw.handle.api.HandleServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.handle.hdllib.AbstractMessage
import net.handle.hdllib.CreateHandleResponse
import net.handle.hdllib.HandleException
import net.handle.hdllib.Util

/**
 * The Handle GRPC-server implementing the grpc interfaces.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleGrpcServer(
    private val config: HandleConfiguration,
    private val communicator: HandleCommunicator = HandleCommunicator(config.password)
) : HandleServiceGrpcKt.HandleServiceCoroutineImplBase() {

    override suspend fun addHandle(request: AddHandleRequest): AddHandleResponse {
        try {
            val resp = communicator.addHandle(request)
            when (resp.responseCode) {
                AbstractMessage.RC_SUCCESS -> {
                    return AddHandleResponse
                        .newBuilder()
                        .setCreatedHandle(Util.decodeString((resp as CreateHandleResponse).handle))
                        .build()
                }
                else ->
                    throw StatusRuntimeException(
                        Status.INTERNAL.withDescription(
                            "Request failed with response code ${resp.responseCode}. Error message: $resp"
                        )
                    )
            }
        } catch (hexc: HandleException) {
            throw StatusRuntimeException(Status.INTERNAL.withCause(hexc))
        }
    }
}
