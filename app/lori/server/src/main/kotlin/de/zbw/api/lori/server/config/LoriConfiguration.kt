package de.zbw.api.lori.server.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration.default
import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.definition.KonfigDeclaration.required
import de.gfelbing.konfig.core.definition.KonfigDeclaration.secret
import de.gfelbing.konfig.core.definition.KonfigDeclaration.string
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
    val mailHost: String,
    val mailPort: Int,
    val mailToError: String,
    val mailToWarning: String?,
    val mailFrom: String,
    val sessionSignKey: String,
    val sessionEncryptKey: String,
    val stage: String,
    val handleURL: String,
    val commitHash: String,
    val downloadDir: String,
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
            val sqlUrl = string(prefix, "sql", "url").required()
            val sqlUser = string(prefix, "sql", "user").required()
            val sqlPassword = string(prefix, "sql", "password").secret().required()
            val digitalArchiveAddress =
                string(prefix, "connection", "digitalarchive", "address").required()
            val digitalArchiveBasicAuth =
                string(prefix, "connection", "digitalarchive", "basicauth").required()
            val digitalArchiveUsername =
                string(
                    prefix,
                    "connection",
                    "digitalarchive",
                    "credentials",
                    "username",
                ).required()
            val digitalArchivePassword =
                string(prefix, "connection", "digitalarchive", "credentials", "password")
                    .secret()
                    .required()
            val jwtAudience = string(prefix, "jwt", "audience").required()
            val jwtIssuer = string(prefix, "jwt", "issuer").required()
            val jwtRealm = string(prefix, "jwt", "realm").required()
            val jwtSecret = string(prefix, "jwt", "secret").secret().required()
            val duoUrlMetadata = string(prefix, "duo", "metadata").required()
            val duoUrlSLO = string(prefix, "duo", "slo").required()
            val duoUrlSSO = string(prefix, "duo", "sso").required()
            val sessionSignKey = string(prefix, "session", "sign").secret().required()
            val sessionEncryptKey = string(prefix, "session", "encrypt").secret().required()
            val stage = string(prefix, "stage").required()
            val handleURL = string(prefix, "connection", "digitalarchive", "handleurl").required()
            val downloadDir = string(prefix, "download", "directory").required()
            val mailHost = string(prefix, "mail", "host").required()
            val mailPort = int(prefix, "mail", "port").required()
            val mailToError = string(prefix, "mail", "to", "error").required()
            val mailFrom = string(prefix, "mail", "from").required()
            val mailToWarning = string(prefix, "mail", "to", "warning")

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
                downloadDir = source[downloadDir],
                mailHost = source[mailHost],
                mailPort = source[mailPort],
                mailToError = source[mailToError],
                mailToWarning = source[mailToWarning],
                mailFrom = source[mailFrom],
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
