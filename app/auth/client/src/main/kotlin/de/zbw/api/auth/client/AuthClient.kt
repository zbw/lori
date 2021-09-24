package de.zbw.api.auth.client

import de.zbw.api.auth.client.config.AuthClientConfiguration
import de.zbw.auth.api.AuthServiceGrpcKt.AuthServiceCoroutineStub
import de.zbw.auth.api.SayHelloRequest
import de.zbw.auth.api.SayHelloResponse
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * A client for the Auth-Service.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthClient(
    val configuration: AuthClientConfiguration,
    val channel: Channel = ManagedChannelBuilder.forAddress(configuration.address, configuration.port).build(),
    val stub: AuthServiceCoroutineStub = AuthServiceCoroutineStub(channel)
) {

    suspend fun sayHello(request: SayHelloRequest): SayHelloResponse =
        runWithTracing("client_sayHello") { s: AuthServiceCoroutineStub ->
            s.sayHello(request)
        }

    private suspend fun runWithTracing(msg: String, op: suspend (AuthServiceCoroutineStub) -> SayHelloResponse): SayHelloResponse {
        // Add some reasonable tracing framework here...
        LOG.info(msg)
        return op.invoke(stub.withDeadlineAfter(configuration.deadlineInMilli, TimeUnit.MILLISECONDS))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AuthClient::class.java)
    }
}
