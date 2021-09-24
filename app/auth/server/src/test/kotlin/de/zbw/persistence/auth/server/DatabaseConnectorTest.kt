package de.zbw.persistence.auth.server

import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import kotlin.test.assertNotNull

/**
 * Testing [DatabaseConnector].
 *
 * Created on 09-23-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnectorTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
    )

    @DataProvider(name = DATA_FOR_INSERT_USER)
    fun createInsertUserData() = arrayOf(
        arrayOf(
            UserData(
                "testUserInsert1",
                "garfield",
                "mymail@domain.com",
            ),
        ),
        arrayOf(
            UserData(
                "testUserInsert2",
                "garfield",
                null,
            ),
        )
    )

    @Test(dataProvider = DATA_FOR_INSERT_USER)
    fun testInsertUserRoundtrip(userData: UserData) {
        // given
        assertFalse(dbConnector.findUserByName(userData.name), "This username should not exist")
        val insertUserId = dbConnector.insertUser(
            userData.name,
            userData.password,
            userData.email,
        )
        assertNotNull(insertUserId, "Insertion was not successful")
        assertTrue(dbConnector.findUserByName(userData.name), "This username should not exist")
    }

    companion object {
        const val DATA_FOR_INSERT_USER = "DATA_FOR_INSERT_USER"

        data class UserData(
            val name: String,
            val password: String,
            val email: String?,
        )
    }
}
