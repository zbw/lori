package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.User
import de.zbw.business.lori.server.type.UserRole
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testing [UserDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class UserDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    )

    @Test
    fun testUsernameExistsRoundtrip() {
        // given
        val expectedUser = TEST_USER

        assertFalse(dbConnector.userDB.userTableContainsName(expectedUser.name))
        // when
        val userName = dbConnector.userDB.insertUser(expectedUser)

        // then
        assertThat(userName, `is`(TEST_USER.name))
        assertTrue(dbConnector.userDB.userTableContainsName(expectedUser.name))

        // when
        val deletedUsers = dbConnector.userDB.deleteUser(expectedUser.name)
        assertThat(deletedUsers, `is`(1))
        assertFalse(dbConnector.userDB.userTableContainsName(expectedUser.name))
    }

    @Test
    fun testUserExistsByNameAndPassword() {
        // given
        val expectedUser = TEST_USER.copy(
            name = "testUserExists",
        )

        assertFalse(dbConnector.userDB.userTableContainsName(expectedUser.name))
        // when
        val userName = dbConnector.userDB.insertUser(expectedUser)

        // then
        assertThat(userName, `is`(expectedUser.name))
        assertTrue(dbConnector.userDB.userTableContainsName(expectedUser.name))

        // when
        assertTrue(
            dbConnector.userDB.userExistsByNameAndPassword(
                expectedUser.name,
                expectedUser.passwordHash
            )
        )
    }

    @Test
    fun testUserDoesNotExistsByNameAndPassword() {
        // given
        val expectedUser = TEST_USER.copy(
            name = notExistingUsername,
        )

        assertFalse(dbConnector.userDB.userTableContainsName(expectedUser.name))
        // when
        val userName = dbConnector.userDB.insertUser(expectedUser)

        // then
        assertThat(userName, `is`(expectedUser.name))
        assertTrue(dbConnector.userDB.userTableContainsName(expectedUser.name))

        // when
        assertFalse(
            dbConnector.userDB.userExistsByNameAndPassword(
                expectedUser.name,
                expectedUser.passwordHash + "$",
            )
        )
    }

    @Test
    fun testGetRoleByUsername() {
        // given
        val expectedUser = TEST_USER.copy(
            name = "testGetRoleByExistingUsername",
            role = UserRole.READWRITE,
        )

        // when
        val userName = dbConnector.userDB.insertUser(expectedUser)
        // then
        assertThat(userName, `is`(expectedUser.name))

        // when
        val receivedRole = dbConnector.userDB.getRoleByUsername(expectedUser.name)
        // then
        assertThat(receivedRole, `is`(expectedUser.role))

        // when
        val receivedRoleNonExistingUser = dbConnector.userDB.getRoleByUsername(notExistingUsername)
        // then
        assertNull(receivedRoleNonExistingUser)
    }

    @Test
    fun testUpdateUserNonRoleProperties() {
        // given
        val beforeUpdateUser = TEST_USER.copy(
            name = "testUpdateUserNonRoleProp",
            role = UserRole.READONLY,
        )
        dbConnector.userDB.insertUser(beforeUpdateUser)

        val afterUpdateUser = beforeUpdateUser.copy(
            passwordHash = "foobar23456"
        )

        dbConnector.userDB.updateUserNonRoleProperties(afterUpdateUser)
        assertThat(
            dbConnector.userDB.getUserByName(afterUpdateUser.name),
            `is`(afterUpdateUser),
        )
    }

    @Test
    fun testUpdateUserRoleProperty() {
        // given
        val beforeUpdateUser = TEST_USER.copy(
            name = "testUpdateUserRoleProp",
            role = UserRole.READONLY,
        )
        dbConnector.userDB.insertUser(beforeUpdateUser)

        val afterUpdateUser = beforeUpdateUser.copy(
            role = UserRole.READWRITE
        )

        // when
        dbConnector.userDB.updateUserRoleProperty(afterUpdateUser.name, afterUpdateUser.role!!)

        // then
        assertThat(
            dbConnector.userDB.getUserByName(afterUpdateUser.name),
            `is`(afterUpdateUser),
        )
    }

    @Test
    fun testRoundtripSessions() {
        val sessionId: String = dbConnector.userDB.insertSession(TEST_SESSION)
        assertThat(
            dbConnector.userDB.getSessionById(sessionId),
            `is`(TEST_SESSION.copy(sessionID = sessionId))
        )
        dbConnector.userDB.deleteSessionById(sessionId)
        assertNull(
            dbConnector.userDB.getSessionById(sessionId),
        )
    }

    companion object {
        const val notExistingUsername = "notExistentUser"
        private val TEST_USER = User(
            name = "Bob",
            passwordHash = "122345",
            role = UserRole.ADMIN,
        )
        private val TEST_SESSION = Session(
            sessionID = null,
            authenticated = true,
            firstName = "some",
            lastName = "name",
            role = UserRole.ADMIN,
            validUntil = OffsetDateTime.of(
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
