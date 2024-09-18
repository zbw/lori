package de.zbw.persistence.lori.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

/**
 * Interface for embedded database tests.
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
abstract class DatabaseTest {
    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.start()
    protected val testDataSource: HikariDataSource = initDataSource(embeddedPostgres)

    init {
        val conn = testDataSource.connection
        conn.use { c ->
            c
                .prepareStatement("create EXTENSION IF NOT EXISTS \"pg_trgm\"")
                .execute()
            c.commit()
        }

        FlywayMigrator(testDataSource).migrate()
    }

    private fun initDataSource(embeddedPostgres: EmbeddedPostgres): HikariDataSource {
        val hiConfig = HikariConfig()
        hiConfig.jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
        hiConfig.username = "postgres"
        hiConfig.password = "postgres"
        hiConfig.addDataSourceProperty("cachePrepStmts", "true")
        hiConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hiConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        hiConfig.isAutoCommit = false
        hiConfig.maximumPoolSize = 5
        hiConfig.driverClassName = "org.postgresql.Driver"
        return HikariDataSource(hiConfig)
    }
}
