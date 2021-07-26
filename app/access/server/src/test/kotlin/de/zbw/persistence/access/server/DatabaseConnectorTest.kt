package de.zbw.persistence.access.server

import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Header
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import kotlin.test.assertTrue

/**
 * Testing [DatabaseConnector].
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnectorTest : DatabaseTest() {

    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
    )

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testInsertHeaderException() {

        // given
        val testHeaderId = "double_entry"
        val testHeader = TEST_HEADER.copy(id = testHeaderId)

        // when
        dbConnector.insertHeader(testHeader)

        // exception
        dbConnector.insertHeader(testHeader)
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testInsertHeaderNoInsertError() {
        // given
        val stmntAccIns = "INSERT INTO ${DatabaseConnector.TABLE_NAME_HEADER}" +
            "(header_id,tenant,usage_guide,template,mention,sharealike,commercial_use,copyright) " +
            "VALUES(?,?,?,?,?,?,?,?)"

        val prepStmt = spyk(dbConnector.connection.prepareStatement(stmntAccIns)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection> {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            }
        )
        // when
        dbConnectorMockked.insertHeader(TEST_HEADER)
        // then exception
    }

    @Test
    fun testInsertAndReceiveHeader() {

        // given
        val testHeaderId = "header_test"
        val testHeader = TEST_HEADER.copy(id = testHeaderId, template = "foo")

        // when
        val headerResponse = dbConnector.insertHeader(testHeader)

        // then
        assertThat(headerResponse, `is`(testHeaderId))

        // when
        val receivedHeader: List<Header> = dbConnector.getHeaders(listOf(testHeaderId))

        // then
        assertThat(
            receivedHeader.first(), `is`(testHeader)
        )

        // when
        assertThat(
            dbConnector.getHeaders(listOf("not_in_db")), `is`(listOf())
        )
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testInsertActionError() {
        // when
        dbConnector.insertAction(TEST_ACTION, "invalid_foreign_key")
        // then
        // exception
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testInsertActionNoInsertError() {
        // given
        val stmntActIns = "INSERT INTO ${DatabaseConnector.TABLE_NAME_ACTION}" +
            "(type, permission, header_id) " +
            "VALUES(?,?,?)"
        val prepStmt = spyk(dbConnector.connection.prepareStatement(stmntActIns)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            }
        )
        // when
        dbConnectorMockked.insertAction(TEST_ACTION, "foo")
        // then exception
    }

    @Test
    fun testInsertActionWithoutRestriction() {
        // given
        val givenHeaderId = "action_test"
        val givenHeader = TEST_HEADER.copy(id = givenHeaderId)
        val givenAction = Action(
            type = ActionType.READ,
            permission = true,
            restrictions = listOf(),
        )

        // when
        dbConnector.insertHeader(givenHeader)
        val actionResponse = dbConnector.insertAction(givenAction, givenHeaderId)
        // then
        assertTrue(actionResponse > 0, "Inserting an action with a header was not successful")

        // when
        val receivedActions: Map<String, List<Action>> = dbConnector.getActions(listOf(givenHeaderId))

        // then
        assertThat(receivedActions[givenHeaderId]!!.first(), `is`(givenAction))
    }

    @Test
    fun testInsertWithRestriction() {
        // given
        val givenHeaderId = "action_test_with_restriction"
        val givenHeader = TEST_HEADER.copy(id = givenHeaderId)
        val givenAction = TEST_ACTION

        // when
        dbConnector.insertHeader(givenHeader)
        val actionResponse = dbConnector.insertAction(givenAction, givenHeaderId)
        // then
        assertTrue(actionResponse > 0, "Inserting an action with a header was not successful")

        // when
        dbConnector.insertRestriction(TEST_ACTION.restrictions.first(), actionResponse)
        val receivedActions: Map<String, List<Action>> = dbConnector.getActions(listOf(givenHeaderId))

        // then
        assertThat(receivedActions[givenHeaderId]!!.first(), `is`(givenAction))
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testInsertRestrictionError() {
        // when
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } throws SQLException()
            }
        )
        // then
        dbConnector.insertRestriction(TEST_ACTION.restrictions.first(), 1)
        // exception
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testInsertRestrictionNoInsertError() {
        // given
        val stmntRestIns = "INSERT INTO ${DatabaseConnector.TABLE_NAME_RESTRICTION}" +
            "(type, attribute_type, attribute_values, action_id) " +
            "VALUES(?,?,?,?)"

        val prepStmt = spyk(dbConnector.connection.prepareStatement(stmntRestIns)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            }
        )
        // when
        dbConnectorMockked.insertRestriction(TEST_ACTION.restrictions.first(), 1)
        // then exception
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testGetHeadersException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any()) } throws SQLException()
            }
        )
        dbConnector.getHeaders(listOf("foo"))
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testGetActionException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any()) } throws SQLException()
            }
        )
        dbConnector.getActions(listOf("foo"))
    }

    companion object {

        val TEST_HEADER = Header(
            id = "that-test",
            tenant = "www.zbw.eu",
            usageGuide = "www.zbw.eu/license",
            template = null,
            copyright = true,
            mention = true,
            commercialUse = false,
            shareAlike = true,
        )

        private val TEST_ACTION = Action(
            type = ActionType.READ,
            permission = true,
            restrictions = listOf(
                Restriction(
                    type = RestrictionType.DATE,
                    attribute = Attribute(
                        type = AttributeType.FROM_DATE,
                        values = listOf("2022-01-01")
                    )
                )
            )
        )
    }
}
