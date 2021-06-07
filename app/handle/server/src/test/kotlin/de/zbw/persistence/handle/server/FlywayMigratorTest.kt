package de.zbw.persistence.handle.server

import de.zbw.api.handle.server.config.HandleConfiguration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.output.MigrateResult
import org.flywaydb.core.internal.jdbc.DriverDataSource
import org.testng.annotations.Test

/**
 * Testing [FlywayMigrator].
 *
 * Created on 06-03-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class FlywayMigratorTest {

    @Test
    fun testMigrate() {
        // given
        val flyway = mockk<Flyway>() {
            every { migrate() } returns mockk<MigrateResult>()
        }
        val flywayMigrator = FlywayMigrator(
            config = EXAMPLE_CONFIG,
            flyway = flyway,
            dataSource = mockk<DriverDataSource>(),
        )

        // when
        flywayMigrator.migrate()

        // then
        verify(exactly = 1) { flyway.migrate() }
    }

    @Test(expectedExceptions = [FlywayException::class])
    fun testMigrateFailure() {
        // given
        val flyway = mockk<Flyway>() {
            every { migrate() } throws FlywayException("error")
        }
        val flywayMigrator = FlywayMigrator(
            config = EXAMPLE_CONFIG,
            flyway = flyway,
            dataSource = mockk<DriverDataSource>(),
        )

        // when + exception
        flywayMigrator.migrate()
    }

    companion object {
        val EXAMPLE_CONFIG =
            HandleConfiguration(
                1234,
                8080,
                "password",
                "5678",
                "jdbc",
                "postgres",
                "postgres",
            )
    }
}
