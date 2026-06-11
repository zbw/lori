package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserPermission
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import io.mockk.every
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.assertNotNull
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertNull

/**
 * Testing [UserDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class UserDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            batchConnectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @Test
    fun testRoundTripSessions() =
        runBlocking {
            val sessionId: String = dbConnector.userDB.insertSession(TEST_SESSION)
            assertThat(
                dbConnector.userDB.getSessionById(sessionId),
                `is`(
                    TEST_SESSION.copy(
                        sessionID = sessionId,
                        createdOn = NOW.toInstant(),
                    ),
                ),
            )
            dbConnector.userDB.deleteSessionById(sessionId)
            assertNull(
                dbConnector.userDB.getSessionById(sessionId),
            )
        }

    @Test
    fun testDeleteOldSessions() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.minusDays(5).toInstant()
            val sessionId: String = dbConnector.userDB.insertSession(TEST_SESSION)
            assertThat(
                dbConnector.userDB.getSessionById(sessionId),
                `is`(
                    TEST_SESSION.copy(
                        sessionID = sessionId,
                        createdOn = NOW.minusDays(5).toInstant(),
                    ),
                ),
            )

            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            assertThat(
                dbConnector.userDB.deleteSessionOlderThan(14),
                `is`(0),
            )
            assertNotNull(
                dbConnector.userDB.getSessionById(sessionId),
            )

            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.plusDays(15L).toInstant()
            assertThat(
                dbConnector.userDB.deleteSessionOlderThan(14),
                `is`(1),
            )
            assertNull(
                dbConnector.userDB.getSessionById(sessionId),
            )
        }

    companion object {
        private val TEST_SESSION =
            Session(
                sessionID = null,
                authenticated = true,
                firstName = "some",
                lastName = "name",
                permissions = listOf(UserPermission.WRITE, UserPermission.READ),
                validUntil =
                    OffsetDateTime
                        .of(
                            2022,
                            3,
                            2,
                            1,
                            1,
                            0,
                            0,
                            ZoneOffset.UTC,
                        ).toInstant(),
                createdOn = NOW.toInstant(),
            )
    }
}
