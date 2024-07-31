package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.UserPermission
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
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
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("foo"),
        )

    @Test
    fun testRoundtripSessions() {
        val sessionId: String = dbConnector.userDB.insertSession(TEST_SESSION)
        assertThat(
            dbConnector.userDB.getSessionById(sessionId),
            `is`(TEST_SESSION.copy(sessionID = sessionId)),
        )
        dbConnector.userDB.deleteSessionById(sessionId)
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
            )
    }
}
