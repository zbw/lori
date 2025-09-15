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
 * Testing [JobDB].
 *
 * Created on 09-10-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class JobDBTest : DatabaseTest() {
    private val jobDB =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).jobDB

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

            val jobUUID = jobDB.insertJob(TEST_Export_JOB).let { UUID.fromString(it) }
            val jobReceived = jobDB.getJobById(jobUUID)
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
                jobDB.updateJobStatusById(
                    updatedJob,
                )

            assertThat(
                "Number of updated rows has to be 1",
                rowsUpdated,
                `is`(1),
            )

            val jobReceivedAfterUpdate = jobDB.getJobById(jobUUID)
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
