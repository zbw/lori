package de.zbw.persistence.auth.server

import de.zbw.auth.model.UserRole
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeClass
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

    @BeforeClass
    fun fillDB() {
        UserRole.Role.values().forEach {
            dbConnector.insertRole(it)
        }
    }

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
    fun testInsertUser(userData: UserData) {
        // given
        assertFalse(dbConnector.usernameExists(userData.name), "This username should not exist")
        // when
        val userId = dbConnector.insertUser(
            userData.name,
            userData.password,
            userData.email,
        )
        // then
        assertNotNull(userId, "Insertion was not successful")
        assertTrue(dbConnector.usernameExists(userData.name), "This username should not exist")
    }

    @Test
    fun testFindRoleIdByName() {
        // given
        UserRole.Role.values().forEach {
            val roleId = dbConnector.getRoleIdByName(it)
            assertNotNull(roleId)
        }
    }

    @Test
    fun testInsertUserRole() {
        // given
        val userData = UserData(
            name = "TestUserRT",
            password = "testPasswordRT",
            email = "email@domain.com",
        )

        val userRights = listOf(
            UserRole.Role.userWrite,
            UserRole.Role.admin,
        )

        // when
        // Insert new user
        val userId: Int = dbConnector.insertUser(
            userData.name,
            userData.password,
            userData.email,
        )!!

        val roleIds = userRights.map {
            dbConnector.getRoleIdByName(it)
        }

        // Give new user write+admin rights
        roleIds.forEach { roleId ->
            dbConnector.insertUserRole(userId, roleId!!)
        }

        // then
        val receivedRoles = dbConnector.getRolesByUserId(userId)
        assertThat(receivedRoles.toSet(), `is`(userRights.toSet()))
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
