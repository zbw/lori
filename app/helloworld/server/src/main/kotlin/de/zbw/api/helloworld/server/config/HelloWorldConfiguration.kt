package de.zbw.api.helloworld.server.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration.default
import de.zbw.api.helloworld.server.ServicePoolWithProbes
import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.source.KonfigurationSource

/**
 * Configurations for the Microservice.
 *
 * Created on 04-27-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class HelloWorldConfiguration(
    val grpcPort: Int,
    val httpPort: Int,
){
    companion object {
        private const val DEFAULT_HTTP_PORT = 8082
        private const val DEFAULT_GRPC_PORT = 9092

        fun load(
            prefix: String,
            source: KonfigurationSource,
        ) : HelloWorldConfiguration {
            val grpcPort = int(prefix, "grpc", "port").default(DEFAULT_GRPC_PORT)
            val httpPort = int(prefix, "http", "port").default(DEFAULT_HTTP_PORT)
            return HelloWorldConfiguration(
                httpPort = source[httpPort],
                grpcPort = source[grpcPort],
            )
        }
    }
}
