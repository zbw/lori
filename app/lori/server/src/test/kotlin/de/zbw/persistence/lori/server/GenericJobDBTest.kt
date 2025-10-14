package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.GenericJob
import de.zbw.business.lori.server.type.JobKind
import de.zbw.business.lori.server.type.JobStatus
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
 * Testing [GenericJobDB].
 *
 * Created on 09-23-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GenericJobDBTest : DatabaseTest() {
    private val jobDB =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).genericJobDB

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

            val jobUUID = jobDB.insertJob(TEST_Generic_JOB).let { UUID.fromString(it) }
            val jobReceived = jobDB.getJobById(jobUUID)
            assertThat(
                jobReceived.toString(),
                `is`(
                    TEST_Generic_JOB
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
                    status = JobStatus.FAILED,
                    errorMessage = "error",
                    summary = "No summary",
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
                    TEST_Generic_JOB
                        .copy(
                            id = jobUUID,
                            createdOn = NOW.toInstant(),
                            lastUpdatedOn = updatedNOW.toInstant(),
                            status = updatedJob.status,
                            errorMessage = updatedJob.errorMessage,
                            summary = updatedJob.summary,
                        ).toString(),
                ),
            )

            val ids =
                jobDB.getJobsOlderThan(NOW.plusDays(2L).toInstant())
            assertThat(
                ids.size,
                `is`(1),
            )
            assertThat(
                ids[0].id,
                `is`(jobUUID),
            )

            val deletions =
                jobDB.deleteJobsByIds(
                    listOf(jobUUID),
                )
            assertThat(
                deletions,
                `is`(1),
            )
            assertThat(
                jobDB
                    .getJobsOlderThan(
                        NOW.plusDays(2L).toInstant(),
                    ).size,
                `is`(0),
            )
        }

    @Test
    fun testJobSuccessful() =
        runBlocking {
            val createTime = NOW.toInstant()
            mockkCurrentTime(createTime)
            val jobUUID =
                jobDB
                    .insertJob(TEST_Generic_JOB.copy(status = JobStatus.SUCCESSFUL))
                    .let { UUID.fromString(it) }
            val jobReceived = jobDB.getJobById(jobUUID)
            assertThat(
                jobReceived.toString(),
                `is`(
                    TEST_Generic_JOB
                        .copy(
                            id = jobUUID,
                            createdOn = NOW.toInstant(),
                            lastUpdatedOn = NOW.toInstant(),
                            status = JobStatus.SUCCESSFUL,
                        ).toString(),
                ),
            )
            val successfulJobs = jobDB.getSuccessfulJobsSince(NOW.minusDays(1L).toInstant())

            assertThat(
                successfulJobs.size,
                `is`(1),
            )
            val deleted = jobDB.deleteJobsByIds(successfulJobs.map { it.id })
            assertThat(
                deleted,
                `is`(1),
            )
        }

    companion object {
        val TEST_Generic_JOB =
            GenericJob(
                id = UUID.randomUUID(),
                status = JobStatus.QUEUED,
                createdBy = "user",
                errorMessage = null,
                kind = JobKind.FULL_IMPORT,
                summary = "some summary",
            )
    }
}
