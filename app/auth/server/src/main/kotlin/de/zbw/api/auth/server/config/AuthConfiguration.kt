package de.zbw.api.auth.server.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration.default
import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.definition.KonfigDeclaration.required
import de.gfelbing.konfig.core.definition.KonfigDeclaration.secret
import de.gfelbing.konfig.core.definition.KonfigDeclaration.string
import de.gfelbing.konfig.core.source.KonfigurationSource

/**
 * Configurations for the Microservice.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class AuthConfiguration(
    val grpcPort: Int,
    val httpPort: Int,
    val jwtAudience: String,
    val jwtIssuer: String,
    val jwtRealm: String,
    val jwtSecret: String,
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
        ): AuthConfiguration {
            val grpcPort = int(prefix, "grpc", "port").default(DEFAULT_GRPC_PORT)
            val httpPort = int(prefix, "http", "port").default(DEFAULT_HTTP_PORT)
            val jwtAudience = string(prefix, "jwt", "audience").required()
            val jwtIssuer = string(prefix, "jwt", "issuer").required()
            val jwtRealm = string(prefix, "jwt", "realm").required()
            val jwtSecret = string(prefix, "jwt", "secret").secret().required()
            val sqlUrl = string(prefix, "sql", "url").required()
            val sqlUser = string(prefix, "sql", "user").required()
            val sqlPassword = string(prefix, "sql", "password").secret().required()
            return AuthConfiguration(
                httpPort = source[httpPort],
                grpcPort = source[grpcPort],
                jwtAudience = source[jwtAudience],
                jwtIssuer = source[jwtIssuer],
                jwtRealm = source[jwtRealm],
                jwtSecret = source[jwtSecret],
                sqlUrl = source[sqlUrl],
                sqlUser = source[sqlUser],
                sqlPassword = source[sqlPassword],
            )
        }
    }
}
