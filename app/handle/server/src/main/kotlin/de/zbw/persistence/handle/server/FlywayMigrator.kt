package de.zbw.persistence.handle.server

import de.zbw.api.handle.server.config.HandleConfiguration
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.internal.jdbc.DriverDataSource
import org.slf4j.LoggerFactory

/**
 * Execute flyway migrations.
 *
 * Created on 06-03-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class FlywayMigrator(
    config: HandleConfiguration,
    dataSource: DriverDataSource = createDriverDataSource(
        config.sqlUrl,
        config.sqlUser,
        config.sqlPassword,
    ),
    private val flyway: Flyway = Flyway.configure().dataSource(dataSource).load()
) {

    fun migrate() {
        try {
            flyway.migrate()
        } catch (flywayException: FlywayException) {
            LOG.error("Migration via Flyway failed.", flywayException)
            throw flywayException
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FlywayMigrator::class.java)
        private const val DRIVER_CLASS = "org.postgresql.Driver"

        fun createDriverDataSource(
            sqlUrl: String,
            sqlUser: String,
            sqlPassword: String,
        ): DriverDataSource =
            DriverDataSource(
                this::class.java.classLoader,
                DRIVER_CLASS,
                sqlUrl,
                sqlUser,
                sqlPassword
            )
    }
}
