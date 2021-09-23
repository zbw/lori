package de.zbw.business.auth.server

import de.zbw.auth.model.SignUpUserData
import de.zbw.persistence.auth.server.DatabaseConnector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

class AuthBackendTest {

    @Test
    fun testRegisterNewUser() {
        // given
        val dbConnectorMock: DatabaseConnector = mockk() {
            every { insertUser(any(), any(), any()) } returns "1"
        }
        val authBackend = AuthBackend(dbConnectorMock)

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
            dbConnectorMock.insertUser(
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
}
