package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.Action
import de.zbw.business.lori.server.ActionType
import de.zbw.business.lori.server.Attribute
import de.zbw.business.lori.server.AttributeType
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.PublicationType
import de.zbw.business.lori.server.Restriction
import de.zbw.business.lori.server.RestrictionType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import kotlin.test.assertFalse
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

    @Test(expectedExceptions = [SQLException::class])
    fun testInsertHeaderException() {

        // given
        val testHeaderId = "double_entry"
        val testHeader = TEST_Metadata.copy(id = testHeaderId)

        // when
        dbConnector.insertMetadata(testHeader)

        // exception
        dbConnector.insertMetadata(testHeader)
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertMetadataNoInsertError() {
        // given
        val stmntAccIns = "INSERT INTO ${DatabaseConnector.TABLE_NAME_ITEM_METADATA}" +
            "(header_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,access_state,publishedYear,band,publicationtype,doi," +
            "serialNumber,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "license_conditions,provenance_license)" +
            "VALUES(?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?)"

        val prepStmt = spyk(dbConnector.connection.prepareStatement(stmntAccIns)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection> {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            }
        )
        // when
        dbConnectorMockked.insertMetadata(TEST_Metadata)
        // then exception
    }

    @Test
    fun testInsertAndReceiveMetadata() {
        // given
        val testId = "id_test"
        val testMetadata = TEST_Metadata.copy(id = testId, title = "foo")

        // when
        val responseInsert = dbConnector.insertMetadata(testMetadata)

        // then
        assertThat(responseInsert, `is`(testId))

        // when
        val receivedMetadata: List<ItemMetadata> = dbConnector.getMetadata(listOf(testId))

        // then
        assertThat(
            receivedMetadata.first(), `is`(testMetadata)
        )

        // when
        assertThat(
            dbConnector.getMetadata(listOf("not_in_db")), `is`(listOf())
        )
    }

    @Test
    fun testBatchUpsert() {
        // given
        val id1 = "upsert1"
        val id2 = "upsert2"
        val m1 = TEST_Metadata.copy(id = id1, title = "foo")
        val m2 = TEST_Metadata.copy(id = id2, title = "bar")

        // when
        val responseUpsert = dbConnector.upsertMetadataBatch(listOf(m1, m2))

        // then
        assertThat(responseUpsert, `is`(IntArray(2) { 1 }))

        // when
        val receivedM1: List<ItemMetadata> = dbConnector.getMetadata(listOf(id1))

        // then
        assertThat(
            receivedM1.first(), `is`(m1)
        )

        val receivedM2: List<ItemMetadata> = dbConnector.getMetadata(listOf(id2))

        // then
        assertThat(
            receivedM2.first(), `is`(m2)
        )

        // when
        val m1Changed = m1.copy(title = "foo2")
        val m2Changed = m2.copy(title = "bar2")

        val responseUpsert2 = dbConnector.upsertMetadataBatch(listOf(m1Changed, m2Changed))

        // then
        assertThat(responseUpsert2, `is`(IntArray(2) { 1 }))

        // when
        val receivedM1Changed: List<ItemMetadata> = dbConnector.getMetadata(listOf(id1))

        // then
        assertThat(
            receivedM1Changed.first(), `is`(m1Changed)
        )

        val receivedM2Changed: List<ItemMetadata> = dbConnector.getMetadata(listOf(id2))

        // then
        assertThat(
            receivedM2Changed.first(), `is`(m2Changed)
        )
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testInsertActionError() {
        // when
        dbConnector.insertAction(TEST_ACTION, "invalid_foreign_key")
        // then
        // exception
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertActionNoInsertError() {
        // given
        val stmntActIns = "INSERT INTO ${DatabaseConnector.TABLE_NAME_ITEM_ACTION}" +
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
        val givenHeader = TEST_Metadata.copy(id = givenHeaderId)
        val givenAction = Action(
            type = ActionType.READ,
            permission = true,
            restrictions = listOf(),
        )

        // when
        dbConnector.insertMetadata(givenHeader)
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
        val givenHeader = TEST_Metadata.copy(id = givenHeaderId)
        val givenAction = TEST_ACTION

        // when
        dbConnector.insertMetadata(givenHeader)
        val actionResponse = dbConnector.insertAction(givenAction, givenHeaderId)

        // then
        assertTrue(actionResponse > 0, "Inserting an action with a header was not successful")

        // when
        dbConnector.insertRestriction(TEST_ACTION.restrictions.first(), actionResponse)
        val receivedActions: Map<String, List<Action>> = dbConnector.getActions(listOf(givenHeaderId))

        // then
        assertThat(receivedActions[givenHeaderId]!!.first(), `is`(givenAction))
    }

    @Test(expectedExceptions = [SQLException::class])
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

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertRestrictionNoInsertError() {
        // given
        val stmntRestIns = "INSERT INTO ${DatabaseConnector.TABLE_NAME_ITEM_RESTRICTION}" +
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

    @Test(expectedExceptions = [SQLException::class])
    fun testGetHeadersException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any()) } throws SQLException()
            }
        )
        dbConnector.getMetadata(listOf("foo"))
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetActionException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any()) } throws SQLException()
            }
        )
        dbConnector.getActions(listOf("foo"))
    }

    @Test
    fun testContainsHeader() {

        // given
        val testHeaderId = "headerIdContainCheck"
        val testHeader = TEST_Metadata.copy(id = testHeaderId)

        // when
        val containedBefore = dbConnector.containsHeader(testHeaderId)
        assertFalse(containedBefore, "Header should not exist yet")

        // when
        dbConnector.insertMetadata(testHeader)
        val containedAfter = dbConnector.containsHeader(testHeaderId)
        assertTrue(containedAfter, "Header should exist now")
    }

    @Test
    fun testAccessRightIds() {

        // when
        dbConnector.insertMetadata(TEST_Metadata.copy(id = "aaaa"))
        dbConnector.insertMetadata(TEST_Metadata.copy(id = "aaaab"))
        dbConnector.insertMetadata(TEST_Metadata.copy(id = "aaaac"))

        // then
        assertThat(
            dbConnector.getAccessRightIds(limit = 3, offset = 0),
            `is`(listOf("aaaa", "aaaab", "aaaac"))
        )
        assertThat(
            dbConnector.getAccessRightIds(limit = 2, offset = 1),
            `is`(listOf("aaaab", "aaaac"))
        )
    }

    @Test
    fun testGetPrimaryKeys() {
        // given
        val givenHeaderId = "test_primkey"
        val givenHeader = TEST_Metadata.copy(id = givenHeaderId)
        val givenAction = TEST_ACTION

        // when
        dbConnector.insertMetadata(givenHeader)
        val actionId = dbConnector.insertAction(givenAction, givenHeaderId)
        val restrictionId = dbConnector.insertRestriction(TEST_ACTION.restrictions.first(), actionId)
        val receivedAccessInformationKeys = dbConnector.getAccessInformationKeys(listOf(givenHeaderId))

        // then
        assertThat(
            "The primary keys do not match",
            listOf(
                JoinHeaderActionRestrictionIdTransient(
                    headerId = givenHeaderId,
                    actionId = actionId.toInt(),
                    restrictionId = restrictionId.toInt(),
                )
            ),
            `is`(receivedAccessInformationKeys)
        )
    }

    @Test
    fun testDeleteAccessRight() {
        // given
        val givenHeaderId = "test_primkey_to_be_deleted"
        val givenHeader = TEST_Metadata.copy(id = givenHeaderId)
        val givenAction = TEST_ACTION

        // when
        dbConnector.insertMetadata(givenHeader)
        val actionId = dbConnector.insertAction(givenAction, givenHeaderId)
        val restrictionId = dbConnector.insertRestriction(TEST_ACTION.restrictions.first(), actionId)
        val receivedAccessInformationKeys = dbConnector.getAccessInformationKeys(listOf(givenHeaderId))

        // then
        assertThat(
            "The primary keys do not match",
            listOf(
                JoinHeaderActionRestrictionIdTransient(
                    headerId = givenHeaderId,
                    actionId = actionId.toInt(),
                    restrictionId = restrictionId.toInt(),
                )
            ),
            `is`(receivedAccessInformationKeys)
        )

        // when
        val deletedItems = dbConnector.deleteAccessRights(listOf(givenHeaderId))

        // then
        assertThat(
            "One item should have been deleted",
            deletedItems,
            `is`(1)
        )

        // when
        val receivedHeader = dbConnector.getMetadata(listOf(givenHeaderId))
        val receivedActions: Map<String, List<Action>> = dbConnector.getActions(listOf(givenHeaderId))

        // then
        assertThat(
            "No header of the id $givenHeaderId should exist anymore",
            receivedHeader,
            `is`(emptyList())
        )
        assertThat(
            "No actions for the gvien header id $givenHeaderId should exist anymore",
            receivedActions,
            `is`(emptyMap())
        )
    }

    companion object {

        val TEST_Metadata = ItemMetadata(
            id = "that-test",
            accessState = AccessState.OPEN,
            band = "band",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            licenseConditions = "some conditions",
            paketSigel = "sigel",
            ppn = "ppn",
            ppnEbook = "ppn ebook",
            provenanceLicense = "provenance license",
            publicationType = PublicationType.ARTICLE,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = "some id",
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
