package de.zbw.business.auth.server

import de.zbw.auth.model.SignUpUserData
import de.zbw.auth.model.UserRole
import de.zbw.persistence.auth.server.DatabaseConnector
import de.zbw.persistence.auth.server.DatabaseTest
import io.mockk.spyk
import io.mockk.verify
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

class AuthBackendTest : DatabaseTest() {
    private val dbConnector = spyk(DatabaseConnector(
        connection = dataSource.connection,
    ))

    @Test
    fun testRegisterNewUser() {
        // given
        val authBackend = AuthBackend(dbConnector)

        val userData = SignUpUserData(
            name = "user",
            password = "12345",
            email = "foobar@mail.com",
        )

        // when
        authBackend.registerNewUser(
            userData
        )

        // then
        verify(exactly = 1) {
            dbConnector.insertUser(
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun testHashingAlgorithm() {
        // when
        val hashPW = AuthBackend.hashPassword("garfield")
        // then
        assertTrue(AuthBackend.verifyPassword("garfield", hashPW))
        assertFalse(AuthBackend.verifyPassword("foobar", hashPW))
    }

    @Test
    fun checkInit(){
        // This test has not to be executed first!
        // It checks if the roles were created through the init
        // method once.
        AuthBackend(dbConnector)
        verify (exactly = UserRole.Role.values().size){ dbConnector.insertRole(any()) }
    }
}
