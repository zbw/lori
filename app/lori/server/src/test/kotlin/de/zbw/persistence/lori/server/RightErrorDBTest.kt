package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.RightError
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterTest
import org.testng.annotations.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing [RightErrorDB].
 *
 * Created on 01-17-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightErrorDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    ).rightErrorDB

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun testRightErrorRoundtrip() {
        // Mock time
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
        assertThat(
            dbConnector.getErrorList(10, 0).size,
            `is`(0),
        )
        val errorId = dbConnector.insertError(TEST_RIGHT_ERROR)
        val receivedError: RightError? = dbConnector.getErrorList(10, 0).firstOrNull()
        assertThat(
            receivedError?.toString() ?: "",
            `is`(TEST_RIGHT_ERROR.copy(errorId = errorId, createdOn = NOW).toString()),
        )

        // Delete by AGE
        dbConnector.deleteErrorByAge(NOW.plusDays(1).toInstant())
        assertThat(
            dbConnector.getErrorList(10, 0).size,
            `is`(0),
        )
        // Delete by Id
        val errorId2 = dbConnector.insertError(TEST_RIGHT_ERROR)
        assertThat(
            dbConnector.getErrorList(10, 0).size,
            `is`(1),
        )
        dbConnector.deleteErrorById(errorId2)
        assertThat(
            dbConnector.getErrorList(10, 0).size,
            `is`(0),
        )
    }

    companion object {
        val TEST_RIGHT_ERROR = RightError(
            errorId = null,
            message = "Timing conflict",
            rightIdSource = "sourceRightId",
            conflictingRightId = "conflictingRightId",
            handleId = "somehandle",
            createdOn = null,
            metadataId = "metadataId",
            conflictType = ConflictType.DATE_OVERLAP,
            templateIdSource = 5,
        )

        val NOW: OffsetDateTime = OffsetDateTime.of(
            2022,
            3,
            1,
            1,
            1,
            0,
            0,
            ZoneOffset.UTC,
        )!!
    }
}
