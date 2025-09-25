package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.ExportJobStatus
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test
import java.time.Instant
import java.util.UUID

/**
 * Testing [ExportJobDB].
 *
 * Created on 09-10-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ExportJobDBTest : DatabaseTest() {
    private val exportJobDB =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).exportJobDB

    private fun mockkCurrentTime(instant: Instant) {
        mockkStatic(Instant::class)
        every { Instant.now() } returns instant
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun testJobRoundTrip() =
        runBlocking {
            // Case: Create and Read
            val createTime = NOW.toInstant()
            mockkCurrentTime(createTime)

            val jobUUID = exportJobDB.insertJob(TEST_Export_JOB).let { UUID.fromString(it) }
            val jobReceived = exportJobDB.getJobById(jobUUID)
            assertThat(
                jobReceived.toString(),
                `is`(
                    TEST_Export_JOB
                        .copy(
                            id = jobUUID,
                            createdOn = NOW.toInstant(),
                            lastUpdatedOn = NOW.toInstant(),
                        ).toString(),
                ),
            )
            val updatedNOW = NOW.plusDays(1)
            mockkCurrentTime(updatedNOW.toInstant())

            val updatedJob =
                jobReceived!!.copy(
                    status = ExportJobStatus.FAILED,
                    errorMessage = "error",
                    filePath = "path/to/file.csv",
                )
            val rowsUpdated =
                exportJobDB.updateJobStatusById(
                    updatedJob,
                )

            assertThat(
                "Number of updated rows has to be 1",
                rowsUpdated,
                `is`(1),
            )

            val jobReceivedAfterUpdate = exportJobDB.getJobById(jobUUID)
            assertThat(
                jobReceivedAfterUpdate.toString(),
                `is`(
                    TEST_Export_JOB
                        .copy(
                            id = jobUUID,
                            createdOn = NOW.toInstant(),
                            lastUpdatedOn = updatedNOW.toInstant(),
                            status = updatedJob.status,
                            errorMessage = updatedJob.errorMessage,
                            filePath = updatedJob.filePath,
                        ).toString(),
                ),
            )

            val ids =
                exportJobDB.getJobsOlderThan(NOW.plusDays(2L).toInstant())
            assertThat(
                ids.size,
                `is`(1),
            )
            assertThat(
                ids[0].id,
                `is`(jobUUID),
            )

            val deletions =
                exportJobDB.deleteJobsByIds(
                    listOf(jobUUID),
                )
            assertThat(
                deletions,
                `is`(1),
            )
            assertThat(
                exportJobDB
                    .getJobsOlderThan(
                        NOW.plusDays(2L).toInstant(),
                    ).size,
                `is`(0),
            )
        }

    companion object {
        val TEST_Export_JOB =
            ExportJob(
                id = UUID.randomUUID(),
                status = ExportJobStatus.QUEUED,
                searchTerm = "del:on",
                createdBy = "user",
                filePath = null,
                errorMessage = null,
                format = ExportFormat.CSV,
            )
    }
}
