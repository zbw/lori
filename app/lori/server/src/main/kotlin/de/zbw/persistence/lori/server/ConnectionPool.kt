package de.zbw.persistence.lori.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.zbw.api.lori.server.config.LoriConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.LogManager
import java.sql.Connection

class ConnectionPool(
    private val ds: HikariDataSource,
) {
    constructor(
        config: LoriConfiguration,
    ) : this(
        createConnection(config),
    )

    suspend fun <T> useConnection(
        methodName: String = "unkown",
        block: suspend (Connection) -> T,
    ): T =
        jdbcSemaphore.withPermit {
            LOG.debug("Init: $methodName; Available ${jdbcSemaphore.availablePermits}")
            val connection: Connection = ds.connection
            connection.use { block(it) }.also {
                LOG.debug("End: $methodName: Available: ${jdbcSemaphore.availablePermits}")
            }
        }

    companion object {
        private const val JDBC_PARALLELISM = 5
        private const val IDLE_TIMEOUT = 60000L
        private const val CONNECTION_TIMEOUT = 30000L
        private val LOG = LogManager.getLogger(ConnectionPool::class.java)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val jdbcSemaphore = Semaphore(JDBC_PARALLELISM)

        fun createConnection(config: LoriConfiguration): HikariDataSource {
            val hiConfig = HikariConfig()
            hiConfig.jdbcUrl = config.sqlUrl
            hiConfig.username = config.sqlUser
            hiConfig.password = config.sqlPassword
            hiConfig.addDataSourceProperty("cachePrepStmts", "true")
            hiConfig.addDataSourceProperty("prepStmtCacheSize", "250")
            hiConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            hiConfig.isAutoCommit = false
            hiConfig.maximumPoolSize = JDBC_PARALLELISM
            hiConfig.idleTimeout = IDLE_TIMEOUT
            hiConfig.connectionTimeout = CONNECTION_TIMEOUT
            hiConfig.minimumIdle = JDBC_PARALLELISM
            return HikariDataSource(hiConfig)
        }
    }
}
