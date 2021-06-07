package de.zbw.api.handle.server.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration.default
import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.definition.KonfigDeclaration.required
import de.gfelbing.konfig.core.definition.KonfigDeclaration.secret
import de.gfelbing.konfig.core.definition.KonfigDeclaration.string
import de.gfelbing.konfig.core.source.KonfigurationSource

/**
 * Configurations for the Microservice.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class HandleConfiguration(
    val grpcPort: Int,
    val httpPort: Int,
    val password: String,
    val handlePrefix: String,
    val sqlUrl: String,
    val sqlUser: String,
    val sqlPassword: String,
) {
    companion object {
        private const val DEFAULT_HTTP_PORT = 8082
        private const val DEFAULT_GRPC_PORT = 9092

        fun load(
            prefix: String,
            source: KonfigurationSource,
        ): HandleConfiguration {
            val grpcPort = int(prefix, "grpc", "port").default(DEFAULT_GRPC_PORT)
            val httpPort = int(prefix, "http", "port").default(DEFAULT_HTTP_PORT)
            val password = string(prefix, "server", "password").secret().required()
            val handlePrefix = string(prefix, "server", "prefix").required()
            val sqlUrl = string(prefix, "sql", "url").required()
            val sqlUser = string(prefix, "sql", "user").required()
            val sqlPassword = string(prefix, "sql", "password").secret().required()
            return HandleConfiguration(
                httpPort = source[httpPort],
                grpcPort = source[grpcPort],
                password = source[password],
                handlePrefix = source[handlePrefix],
                sqlUrl = source[sqlUrl],
                sqlUser = source[sqlUser],
                sqlPassword = source[sqlPassword],
            )
        }
    }
}
