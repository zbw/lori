package de.zbw.persistence.auth.server

import de.zbw.api.auth.server.config.AuthConfiguration
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.internal.jdbc.DriverDataSource
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Execute flyway migrations.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class FlywayMigrator(
    dataSource: DataSource,
    private val flyway: Flyway = Flyway
        .configure()
        .validateMigrationNaming(true)
        .locations("db/migration")
        .dataSource(dataSource)
        .load()
) {
    constructor(
        config: AuthConfiguration,
    ) : this(
        createDriverDataSource(
            config.sqlUrl,
            config.sqlUser,
            config.sqlPassword,
        )
    )

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
