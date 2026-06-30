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
        isBatch: Boolean,
    ) : this(
        if (isBatch) {
            createBatchConnection(config)
        } else {
            createConnection(config)
        },
    )

    suspend fun <T> useConnection(
        methodName: String = "unknown",
        block: suspend (Connection) -> T,
    ): T =
        jdbcSemaphore.withPermit {
            LOG.debug("Init: $methodName; Available ${jdbcSemaphore.availablePermits}")
            val connection = ds.connection
            try {
                block(connection)
            } finally {
                LOG.debug("End: $methodName; Available: ${jdbcSemaphore.availablePermits}")
                connection.close()
            }
        }

    companion object {
        private const val CONNECTION_TIMEOUT = 30000L
        private const val CONNECTION_TIMEOUT_BATCH = 60000L
        private const val IDLE_TIMEOUT = 80000L
        private const val JDBC_PARALLELISM = 5
        private const val LEAK_DETECTION_THRESHOLD = 2000L
        private const val MAXIMUM_POOL_SIZE = 5
        private const val MAXIMUM_POOL_SIZE_BATCH = 2
        private const val MINIMUM_IDLE = 2
        private const val MINIMUM_IDLE_BATCH = 1
        private val LOG = LogManager.getLogger(ConnectionPool::class.java)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val jdbcSemaphore = Semaphore(JDBC_PARALLELISM)

        // Optimized for batch jobs
        fun createBatchConnection(config: LoriConfiguration): HikariDataSource {
            val hiConfig =
                HikariConfig().apply {
                    jdbcUrl = config.sqlUrl
                    username = config.sqlUserBatch
                    password = config.sqlPasswordBatch
                    maximumPoolSize = MAXIMUM_POOL_SIZE_BATCH // Reduced for batch
                    minimumIdle = MINIMUM_IDLE_BATCH // Lower idle connections
                    connectionTimeout = CONNECTION_TIMEOUT_BATCH // Longer timeout
                    addDataSourceProperty("ApplicationName", "batch_processor")
                    addDataSourceProperty("prepareThreshold", "5")
                    addDataSourceProperty("prepStmtCacheSize", "50")
                }
            return HikariDataSource(hiConfig)
        }

        fun createConnection(config: LoriConfiguration): HikariDataSource {
            val hiConfig =
                HikariConfig().apply {
                    jdbcUrl = config.sqlUrl
                    username = config.sqlUser
                    password = config.sqlPassword
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                    leakDetectionThreshold = LEAK_DETECTION_THRESHOLD
                    isAutoCommit = false
                    maximumPoolSize = MAXIMUM_POOL_SIZE
                    idleTimeout = IDLE_TIMEOUT
                    connectionTimeout = CONNECTION_TIMEOUT
                    minimumIdle = MINIMUM_IDLE
                }
            return HikariDataSource(hiConfig)
        }
    }
}
