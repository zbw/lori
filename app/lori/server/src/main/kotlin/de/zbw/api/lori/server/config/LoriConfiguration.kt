package de.zbw.api.lori.server.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration
import de.gfelbing.konfig.core.definition.KonfigDeclaration.default
import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.definition.KonfigDeclaration.required
import de.gfelbing.konfig.core.definition.KonfigDeclaration.secret
import de.gfelbing.konfig.core.source.KonfigurationSource

/**
 * Configurations for the Microservice.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class LoriConfiguration(
    val grpcPort: Int,
    val httpPort: Int,
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
        ): LoriConfiguration {
            val grpcPort = int(prefix, "grpc", "port").default(DEFAULT_GRPC_PORT)
            val httpPort = int(prefix, "http", "port").default(DEFAULT_HTTP_PORT)
            val sqlUrl = KonfigDeclaration.string(prefix, "sql", "url").required()
            val sqlUser = KonfigDeclaration.string(prefix, "sql", "user").required()
            val sqlPassword = KonfigDeclaration.string(prefix, "sql", "password").secret().required()
            return LoriConfiguration(
                httpPort = source[httpPort],
                grpcPort = source[grpcPort],
                sqlUrl = source[sqlUrl],
                sqlUser = source[sqlUser],
                sqlPassword = source[sqlPassword],
            )
        }
    }
}
