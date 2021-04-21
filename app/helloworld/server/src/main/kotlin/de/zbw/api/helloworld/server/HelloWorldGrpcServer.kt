package de.zbw.api.helloworld.server

import de.zbw.helloworld.api.HelloWorldServiceGrpcKt
import de.zbw.helloworld.api.SayHelloRequest
import de.zbw.helloworld.api.SayHelloResponse

/**
 * // TODO
 *
 * Created on 04-20-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HelloWorldGrpcServer : HelloWorldServiceGrpcKt.HelloWorldServiceCoroutineImplBase() {
    override suspend fun sayHello(request: SayHelloRequest): SayHelloResponse = SayHelloResponse
                 .newBuilder()
                 .setMessage("Hello ${request.name}")
                 .build()

}