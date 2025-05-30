package de.zbw.api.lori.server.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration
import de.gfelbing.konfig.core.definition.KonfigDeclaration.default
import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.definition.KonfigDeclaration.required
import de.gfelbing.konfig.core.definition.KonfigDeclaration.secret
import de.gfelbing.konfig.core.source.KonfigurationSource
import java.util.Properties

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
    val digitalArchiveAddress: String,
    val digitalArchiveBasicAuth: String,
    val digitalArchiveUsername: String,
    val digitalArchivePassword: String,
    val jwtSecret: String,
    val jwtAudience: String,
    val jwtIssuer: String,
    val jwtRealm: String,
    val duoUrlMetadata: String,
    val duoUrlSLO: String,
    val duoUrlSSO: String,
    val sessionSignKey: String,
    val sessionEncryptKey: String,
    val stage: String,
    val handleURL: String,
    val commitHash: String,
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
            val digitalArchiveAddress =
                KonfigDeclaration.string(prefix, "connection", "digitalarchive", "address").required()
            val digitalArchiveBasicAuth =
                KonfigDeclaration.string(prefix, "connection", "digitalarchive", "basicauth").required()
            val digitalArchiveUsername =
                KonfigDeclaration
                    .string(
                        prefix,
                        "connection",
                        "digitalarchive",
                        "credentials",
                        "username",
                    ).required()
            val digitalArchivePassword =
                KonfigDeclaration
                    .string(prefix, "connection", "digitalarchive", "credentials", "password")
                    .secret()
                    .required()
            val jwtAudience = KonfigDeclaration.string(prefix, "jwt", "audience").required()
            val jwtIssuer = KonfigDeclaration.string(prefix, "jwt", "issuer").required()
            val jwtRealm = KonfigDeclaration.string(prefix, "jwt", "realm").required()
            val jwtSecret = KonfigDeclaration.string(prefix, "jwt", "secret").secret().required()
            val duoUrlMetadata = KonfigDeclaration.string(prefix, "duo", "metadata").required()
            val duoUrlSLO = KonfigDeclaration.string(prefix, "duo", "slo").required()
            val duoUrlSSO = KonfigDeclaration.string(prefix, "duo", "sso").required()
            val sessionSignKey = KonfigDeclaration.string(prefix, "session", "sign").secret().required()
            val sessionEncryptKey = KonfigDeclaration.string(prefix, "session", "encrypt").secret().required()
            val stage = KonfigDeclaration.string(prefix, "stage").required()
            val handleURL = KonfigDeclaration.string(prefix, "connection", "digitalarchive", "handleurl").required()
            val props = Thread.currentThread().contextClassLoader.getResourceAsStream("git.properties")

            return LoriConfiguration(
                httpPort = source[httpPort],
                grpcPort = source[grpcPort],
                sqlUrl = source[sqlUrl],
                sqlUser = source[sqlUser],
                sqlPassword = source[sqlPassword],
                digitalArchiveAddress = source[digitalArchiveAddress],
                digitalArchiveUsername = source[digitalArchiveUsername],
                digitalArchivePassword = source[digitalArchivePassword],
                digitalArchiveBasicAuth = source[digitalArchiveBasicAuth],
                jwtAudience = source[jwtAudience],
                jwtIssuer = source[jwtIssuer],
                jwtRealm = source[jwtRealm],
                jwtSecret = source[jwtSecret],
                duoUrlMetadata = source[duoUrlMetadata],
                sessionSignKey = source[sessionSignKey],
                sessionEncryptKey = source[sessionEncryptKey],
                stage = source[stage],
                handleURL = source[handleURL],
                duoUrlSLO = source[duoUrlSLO],
                duoUrlSSO = source[duoUrlSSO],
                commitHash = loadGitHash(),
            )
        }

        fun loadGitHash(): String {
            val props = Properties()
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("git.properties")
            if (stream != null) {
                props.load(stream)
                return props.getProperty("git.hash") ?: "unknown"
            }
            return "unknown"
        }
    }
}
