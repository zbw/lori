package de.zbw.persistence.auth.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.flywaydb.core.api.ErrorCode
import org.flywaydb.core.api.ErrorDetails
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.exception.FlywayValidateException
import org.testng.annotations.Test

class FlywayMigratorTest{
    @Test(expectedExceptions = [FlywayException::class])
    fun flywayMigrationException() {

        // given
        val flyway: FlywayMigrator = spyk(
            FlywayMigrator(
                mockk(),
                mockk() {
                    every { migrate() } throws FlywayValidateException(
                        ErrorDetails(
                            ErrorCode.VALIDATE_ERROR, "error"
                        ),
                        "foo"
                    )
                }
            )
        )

        // when
        flyway.migrate()

        // then exception
    }
}
