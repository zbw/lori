package de.zbw.persistence.lori.server

import com.mchange.v2.c3p0.ComboPooledDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

/**
 * Interface for embedded database tests.
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
abstract class DatabaseTest {

    private val embeddedPostgres: EmbeddedPostgres = EmbeddedPostgres.start()
    protected val dataSource: ComboPooledDataSource = ComboPooledDataSource().apply {
        jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
        driverClass = "org.postgresql.Driver"
        user = "postgres"
        dataSourceName = "foo"
        maxAdministrativeTaskTime = 300
        unreturnedConnectionTimeout = 0
        acquireIncrement = 10
        maxIdleTime = 1800
        idleConnectionTestPeriod = 0
        isTestConnectionOnCheckin = false
        isTestConnectionOnCheckout = true
        numHelperThreads = 32
        maxPoolSize = 10
        minPoolSize = 1
    }

    init {
        dataSource.connection
            .prepareStatement("create EXTENSION IF NOT EXISTS \"pg_trgm\"")
            .execute()
        FlywayMigrator(dataSource).migrate()
    }
}
