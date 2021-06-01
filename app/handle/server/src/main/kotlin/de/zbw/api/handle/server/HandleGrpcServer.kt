package de.zbw.api.handle.server

import de.zbw.api.handle.server.config.HandleConfiguration
import de.zbw.business.handle.server.HandleCommunicator
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleResponse
import de.zbw.handle.api.AddHandleValuesRequest
import de.zbw.handle.api.AddHandleValuesResponse
import de.zbw.handle.api.DeleteHandleRequest
import de.zbw.handle.api.DeleteHandleResponse
import de.zbw.handle.api.HandleServiceGrpcKt
import de.zbw.handle.api.ModifyHandleValuesRequest
import de.zbw.handle.api.ModifyHandleValuesResponse
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.handle.hdllib.AbstractMessage
import net.handle.hdllib.AbstractResponse
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
    private val communicator: HandleCommunicator = HandleCommunicator(config),
) : HandleServiceGrpcKt.HandleServiceCoroutineImplBase() {

    override suspend fun addHandle(request: AddHandleRequest): AddHandleResponse =
        processRequestWithErrorHandling(
            communicator::addHandle,
            request,
        ) { resp ->
            AddHandleResponse
                .newBuilder()
                .setCreatedHandle(Util.decodeString((resp as CreateHandleResponse).handle))
                .build()
        }

    override suspend fun deleteHandle(request: DeleteHandleRequest): DeleteHandleResponse =
        processRequestWithErrorHandling(communicator::deleteHandle, request) {
            DeleteHandleResponse.getDefaultInstance()
        }

    override suspend fun addHandleValues(request: AddHandleValuesRequest): AddHandleValuesResponse =
        processRequestWithErrorHandling(communicator::addHandleValues, request) {
            AddHandleValuesResponse.getDefaultInstance()
        }

    override suspend fun modifyHandleValues(request: ModifyHandleValuesRequest): ModifyHandleValuesResponse =
        processRequestWithErrorHandling(communicator::modifyHandleValues, request) {
            ModifyHandleValuesResponse.getDefaultInstance()
        }

    companion object {
        private fun <T, U> processRequestWithErrorHandling(
            op: (T) -> AbstractResponse,
            request: T,
            successCase: (AbstractResponse) -> U
        ): U {
            try {
                val resp = op(request)

                when (resp.responseCode) {
                    AbstractMessage.RC_SUCCESS -> {
                        return successCase(resp)
                    }
                    else ->
                        throw StatusRuntimeException(
                            Status.INTERNAL.withDescription(
                                "Request failed with response code ${resp.responseCode}. Error message: $resp"
                            )
                        )
                }
            } catch (hexc: HandleException) {
                throw StatusRuntimeException(
                    Status
                        .INTERNAL
                        .withCause(hexc)
                        .withDescription(hexc.message)
                )
            }
        }
    }
}
