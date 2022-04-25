package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.PublicationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
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
        tracer = OpenTelemetry.noop().getTracer("foo")
    )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testInsertHeaderException() {

        // given
        val testHeaderId = "double_entry"
        val testHeader = TEST_Metadata.copy(metadataId = testHeaderId)

        // when
        dbConnector.insertMetadata(testHeader)

        // exception
        dbConnector.insertMetadata(testHeader)
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertMetadataNoInsertError() {
        // given
        val prepStmt = spyk(dbConnector.connection.prepareStatement(DatabaseConnector.STATEMENT_INSERT_METADATA)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection> {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            },
            tracer,
        )
        // when
        dbConnectorMockked.insertMetadata(TEST_Metadata)
        // then exception
    }

    @Test
    fun testMetadataRoundtrip() {
        // given
        val testId = "id_test"
        val testMetadata = TEST_Metadata.copy(metadataId = testId, title = "foo")

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

        // when
        val deletedMetadata = dbConnector.deleteMetadata(listOf(testId))

        // then
        assertThat(deletedMetadata, `is`(1))
        assertThat(dbConnector.getMetadata(listOf(testId)), `is`(listOf()))
    }

    @Test
    fun testBatchUpsert() {
        // given
        val id1 = "upsert1"
        val id2 = "upsert2"
        val m1 = TEST_Metadata.copy(metadataId = id1, title = "foo")
        val m2 = TEST_Metadata.copy(metadataId = id2, title = "bar")

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
        unmockkAll()

        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.plusDays(1).toInstant()
        val m1Changed = m1.copy(title = "foo2", lastUpdatedBy = "user2", lastUpdatedOn = NOW.plusDays(1))
        val m2Changed = m2.copy(title = "bar2", lastUpdatedBy = "user2", lastUpdatedOn = NOW.plusDays(1))

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

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertRightNoRowInsertedError() {
        // given
        val prepStmt = spyk(dbConnector.connection.prepareStatement(DatabaseConnector.STATEMENT_INSERT_RIGHT)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            },
            tracer,
        )
        // when
        dbConnectorMockked.insertRight(TEST_RIGHT)
        // then exception
    }

    @Test
    fun testRightRoundtrip() {
        // given
        val rightId = "testInsertRight"
        val initialRight = TEST_RIGHT.copy(rightId = rightId)

        // Insert
        // when
        val actionResponse = dbConnector.insertRight(initialRight)
        // then
        assertEquals(actionResponse, rightId, "Inserting a right column was not successful")

        // when
        val receivedRights: List<ItemRight> = dbConnector.getRights(listOf(rightId))

        // then
        assertThat(receivedRights.first(), `is`(initialRight))
        assertTrue(dbConnector.rightContainsId(rightId))

        // upsert

        // given
        val updatedRight = initialRight.copy(lastUpdatedBy = "user2", accessState = AccessState.RESTRICTED)
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.plusDays(1).toInstant()

        // when
        val updatedRights = dbConnector.upsertRight(updatedRight)

        // then
        assertThat(updatedRights, `is`(1))
        val receivedUpdatedRights: List<ItemRight> = dbConnector.getRights(listOf(rightId))
        assertThat(receivedUpdatedRights.first(), `is`(updatedRight.copy(lastUpdatedOn = NOW.plusDays(1))))

        // delete
        // when
        val deletedItems = dbConnector.deleteRights(listOf(rightId))

        // then
        assertThat(deletedItems, `is`(1))

        // when + then
        assertThat(dbConnector.getRights(listOf(rightId)), `is`(emptyList()))
        assertFalse(dbConnector.rightContainsId(rightId))
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetMetadataException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any()) } throws SQLException()
            },
            tracer,
        )
        dbConnector.getMetadata(listOf("foo"))
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetRightException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>() {
                every { prepareStatement(any()) } throws SQLException()
            },
            tracer,
        )
        dbConnector.getRights(listOf("foo"))
    }

    @Test
    fun testContainsMetadata() {

        // given
        val metadataId = "metadataIdContainCheck"
        val expectedMetadata = TEST_Metadata.copy(metadataId = metadataId)

        // when
        val containedBefore = dbConnector.metadataContainsId(metadataId)
        assertFalse(containedBefore, "Metadata should not exist yet")

        // when
        dbConnector.insertMetadata(expectedMetadata)
        val containedAfter = dbConnector.metadataContainsId(metadataId)
        assertTrue(containedAfter, "Metadata should exist now")
    }

    @Test
    fun testMetadataRange() {
        // when
        dbConnector.insertMetadata(TEST_Metadata.copy(metadataId = "aaaa"))
        dbConnector.insertMetadata(TEST_Metadata.copy(metadataId = "aaaab"))
        dbConnector.insertMetadata(TEST_Metadata.copy(metadataId = "aaaac"))

        // then
        assertThat(
            dbConnector.getMetadataRange(limit = 3, offset = 0),
            `is`(listOf("aaaa", "aaaab", "aaaac"))
        )
        assertThat(
            dbConnector.getMetadataRange(limit = 2, offset = 1),
            `is`(listOf("aaaab", "aaaac"))
        )
    }

    @Test
    fun testDeleteItem() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "item_roundtrip_meta")
        val expectedRight = TEST_RIGHT.copy(rightId = "item_roundtrip_right")

        // when
        dbConnector.insertMetadata(expectedMetadata)
        dbConnector.insertRight(expectedRight)
        dbConnector.insertItem(expectedMetadata.metadataId, expectedRight.rightId)

        // then
        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(expectedRight.rightId))
        )

        val deletedItems = dbConnector.deleteItem(expectedMetadata.metadataId, expectedRight.rightId)
        assertThat(
            deletedItems,
            `is`(1),
        )

        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )
    }

    @Test
    fun testDeleteItemBy() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "delete_item_meta")
        val expectedRight = TEST_RIGHT.copy(rightId = "delete_item_right")

        // when
        dbConnector.insertMetadata(expectedMetadata)
        dbConnector.insertRight(expectedRight)
        dbConnector.insertItem(expectedMetadata.metadataId, expectedRight.rightId)

        // then
        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(expectedRight.rightId))
        )

        val deletedItemsByMetadata = dbConnector.deleteItemByMetadata(expectedMetadata.metadataId)
        assertThat(
            deletedItemsByMetadata,
            `is`(1),
        )

        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )

        // when
        dbConnector.insertItem(expectedMetadata.metadataId, expectedRight.rightId)
        // then
        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(expectedRight.rightId))
        )

        val deletedItemsByRight = dbConnector.deleteItemByRight(expectedRight.rightId)
        assertThat(
            deletedItemsByRight,
            `is`(1),
        )

        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )
    }

    @Test
    fun testItemExists() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "item_exists_metadata")
        val expectedRight = TEST_RIGHT.copy(rightId = "item_exists_right")

        assertFalse(dbConnector.itemContainsRight(expectedRight.rightId))
        assertFalse(dbConnector.itemContainsEntry(expectedMetadata.metadataId, expectedRight.rightId))
        assertFalse(dbConnector.itemContainsMetadata(expectedMetadata.metadataId))
        // when
        dbConnector.insertMetadata(expectedMetadata)
        dbConnector.insertRight(expectedRight)
        dbConnector.insertItem(expectedMetadata.metadataId, expectedRight.rightId)

        // then
        assertTrue(dbConnector.itemContainsRight(expectedRight.rightId))
        assertTrue(dbConnector.itemContainsMetadata(expectedMetadata.metadataId))
        assertTrue(dbConnector.itemContainsEntry(expectedMetadata.metadataId, expectedRight.rightId))
        assertThat(dbConnector.countItemByRightId(expectedRight.rightId), `is`(1))
    }

    companion object {
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

        private val TODAY: LocalDate = LocalDate.of(2022, 3, 1)

        val TEST_Metadata = ItemMetadata(
            metadataId = "that-test",
            band = "band",
            createdBy = "user1",
            createdOn = NOW,
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            lastUpdatedBy = "user2",
            lastUpdatedOn = NOW,
            paketSigel = "sigel",
            ppn = "ppn",
            ppnEbook = "ppn ebook",
            publicationType = PublicationType.ARTICLE,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = "anything",
            titleSeries = null,
            zbdId = "some id",
        )

        private
        val TEST_RIGHT = ItemRight(
            rightId = "testright",
            accessState = AccessState.OPEN,
            createdBy = "user1",
            createdOn = NOW,
            licenseConditions = "some conditions",
            provenanceLicense = "provenance license",
            lastUpdatedBy = "user2",
            lastUpdatedOn = NOW,
            startDate = TODAY.minusDays(1),
            endDate = TODAY,
        )
    }

    private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")
}
