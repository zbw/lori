package de.zbw.business.handle.server

import com.benasher44.uuid.uuid4
import de.zbw.api.handle.server.config.HandleConfiguration
import de.zbw.handle.api.AddHandleRequest
import de.zbw.handle.api.AddHandleValuesRequest
import de.zbw.handle.api.DeleteHandleRequest
import de.zbw.handle.api.HandleType
import io.grpc.Status
import io.grpc.StatusRuntimeException
import net.handle.hdllib.AbstractResponse
import net.handle.hdllib.AddValueRequest
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
    private val config: HandleConfiguration,
    val resolver: HandleResolver = createResolver(),
    private val prefix: String = config.handlePrefix,
    private val adminHandle: String = "$prefix/ADMIN",
    private val authenticationInfo: SecretKeyAuthenticationInfo = createAuthInfo(config.password, adminHandle)
) {

    fun addHandle(
        request: AddHandleRequest,
    ): AbstractResponse {
        val handle = if (request.generateHandleSuffix) {
            "$prefix/${uuid4()}"
        } else {
            "$prefix/${request.customHandleSuffix}"
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

    fun addHandleValues(request: AddHandleValuesRequest): AbstractResponse {
        val preparedRequest = AddValueRequest(
            Util.encodeString("$prefix/${request.handleSuffix}"),
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
                "$prefix/${request.handleSuffix}"
            ),
            authenticationInfo,
        )
        return resolver.processRequest(preparedRequest)
    }

    companion object {

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

        fun createAuthInfo(
            password: String,
            adminHandle: String,
        ): SecretKeyAuthenticationInfo =
            SecretKeyAuthenticationInfo(
                Util.encodeString(adminHandle),
                SECRET_KEY_IDX,
                Util.encodeString(password),
            )
    }
}

fun HandleType.convertToHandleValue(): String =
    when (this) {
        HandleType.HANDLE_TYPE_ADMIN -> "HS_ADMIN"
        HandleType.HANDLE_TYPE_ALIAS -> "HS_ALIAS"
        HandleType.HANDLE_TYPE_EMAIL -> "EMAIL"
        HandleType.HANDLE_TYPE_PUBKEY -> "HS_PUBKEY"
        HandleType.HANDLE_TYPE_SECKEY -> "HS_SECKEY"
        HandleType.HANDLE_TYPE_URN -> "URN"
        HandleType.HANDLE_TYPE_URL -> "URL"
        HandleType.HANDLE_TYPE_VLIST -> "HS_VLIST"
        else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("Type for handle value hasn't been specified"))
    }
