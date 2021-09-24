package de.zbw.api.auth.server

import de.zbw.auth.api.AuthServiceGrpcKt
import de.zbw.auth.api.SayHelloRequest
import de.zbw.auth.api.SayHelloResponse

/**
 * Auth GRPC-server.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthGrpcServer : AuthServiceGrpcKt.AuthServiceCoroutineImplBase() {
    override suspend fun sayHello(request: SayHelloRequest): SayHelloResponse = SayHelloResponse
        .newBuilder()
        .setMessage("Hello ${request.name}")
        .build()
}
