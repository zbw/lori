package de.zbw.business.handle.server

import com.benasher44.uuid.uuid4
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.DeleteHandleRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.handle.hdllib.AbstractResponse
import net.handle.hdllib.CreateHandleRequest
import net.handle.hdllib.HandleResolver
import net.handle.hdllib.HandleValue
import net.handle.hdllib.SecretKeyAuthenticationInfo
import net.handle.hdllib.Util
import java.net.URI

/**
 * Class which communicates to the handle server.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleCommunicator(
    private val password: String,
    val resolver: HandleResolver = createResolver(),
    private val authenticationInfo: SecretKeyAuthenticationInfo = createAuthInfo(password)
) {
    fun addHandle(
        request: AddHandleRequest,
    ): AbstractResponse {
        val handle = if (request.generateHandleSuffix) {
            "$PREFIX/${uuid4()}"
        } else {
            "$PREFIX/${request.customHandleSuffix}"
        }
        val preparedRequest = CreateHandleRequest(
            Util.encodeString(handle),
            request.handleValuesList.map {
                HandleValue(
                    it.index,
                    Util.encodeString(it.type.convertToHandleValue()),
                    Util.encodeString(it.value),
                )
            }.toTypedArray(),
            authenticationInfo,
        )
        return resolver.processRequest(preparedRequest)
    }

    fun deleteHandle(request: DeleteHandleRequest): AbstractResponse {
        val preparedRequest = net.handle.hdllib.DeleteHandleRequest(
            Util.encodeString(
                "$PREFIX/${request.handleSuffix}"
            ),
            authenticationInfo,
        )
        return resolver.processRequest(preparedRequest)
    }

    companion object {
        const val PREFIX = "5678"
        private const val ADMIN_HANDLE = "$PREFIX/ADMIN"

        // The index of the secret key at the admin handle.
        private const val SECRET_KEY_IDX = 301

        // The handle client library reads the configurations either from ~/.handle
        // or this property.
        const val HANDLE_CONFIG_DIR_PROP = "net.handle.configDir"

        fun createResolver(): HandleResolver {
            val uri = getConfigURI()?.path ?: ""
            System.setProperty(HANDLE_CONFIG_DIR_PROP, uri)
            return HandleResolver()
        }

        internal fun getConfigURI(): URI? =
            this::class.java.getResource("handler")?.toURI()

        fun createAuthInfo(password: String): SecretKeyAuthenticationInfo =
            SecretKeyAuthenticationInfo(
                Util.encodeString(ADMIN_HANDLE),
                SECRET_KEY_IDX,
                Util.encodeString(password),
            )
    }
}

fun AddHandleRequest.HandleType.convertToHandleValue(): String =
    when (this) {
        AddHandleRequest.HandleType.HANDLE_TYPE_ADMIN -> "HS_ADMIN"
        AddHandleRequest.HandleType.HANDLE_TYPE_ALIAS -> "HS_ALIAS"
        AddHandleRequest.HandleType.HANDLE_TYPE_EMAIL -> "EMAIL"
        AddHandleRequest.HandleType.HANDLE_TYPE_PUBKEY -> "HS_PUBKEY"
        AddHandleRequest.HandleType.HANDLE_TYPE_SECKEY -> "HS_SECKEY"
        AddHandleRequest.HandleType.HANDLE_TYPE_URN -> "URN"
        AddHandleRequest.HandleType.HANDLE_TYPE_URL -> "URL"
        AddHandleRequest.HandleType.HANDLE_TYPE_VLIST -> "HS_VLIST"
        else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Type for handle value hasn't been specified"))
    }
