package de.zbw.api.helloworld.client

import de.zbw.api.helloworld.client.config.HelloWorldClientConfiguration
import de.zbw.helloworld.api.HelloWorldServiceGrpcKt.HelloWorldServiceCoroutineStub
import de.zbw.helloworld.api.SayHelloRequest
import de.zbw.helloworld.api.SayHelloResponse
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * A client for the HelloWorld-Service.
 *
 * Created on 04-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HelloWorldClient(
    val configuration: HelloWorldClientConfiguration,
    val channel: Channel = ManagedChannelBuilder.forAddress(configuration.address, configuration.port).build(),
    val stub: HelloWorldServiceCoroutineStub = HelloWorldServiceCoroutineStub(channel)
) {

    suspend fun sayHello(request: SayHelloRequest): SayHelloResponse =
        runWithTracing("client_sayHello") { s: HelloWorldServiceCoroutineStub ->
            s.sayHello(request)
        }

    private suspend fun runWithTracing(msg: String, op: suspend (HelloWorldServiceCoroutineStub) -> SayHelloResponse): SayHelloResponse {
        // Add some reasonable tracing framework here...
        LOG.info(msg)
        return op.invoke(stub.withDeadlineAfter(configuration.deadlineInMilli, TimeUnit.MILLISECONDS))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HelloWorldClient::class.java)
    }
}
