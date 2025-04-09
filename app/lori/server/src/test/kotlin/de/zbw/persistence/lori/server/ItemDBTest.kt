package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Testing [ItemDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ItemDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
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

    @Test
    fun testDeleteItem() =
        runBlocking {
            // given
            val expectedMetadata = TEST_Metadata.copy(handle = "item_roundtrip_meta")
            val expectedRight = TEST_RIGHT

            // when
            dbConnector.metadataDB.insertMetadata(expectedMetadata)
            val generatedRightId = dbConnector.rightDB.insertRight(expectedRight)
            dbConnector.itemDB.insertItemBatch(
                listOf(
                    ItemId(
                        handle = expectedMetadata.handle,
                        rightId = generatedRightId,
                    ),
                ),
            )

            // then
            assertThat(
                dbConnector.rightDB.getRightIdsByHandle(expectedMetadata.handle),
                `is`(listOf(generatedRightId)),
            )

            val deletedItems = dbConnector.itemDB.deleteItem(expectedMetadata.handle, generatedRightId)
            assertThat(
                deletedItems,
                `is`(1),
            )

            assertThat(
                dbConnector.rightDB.getRightIdsByHandle(expectedMetadata.handle),
                `is`(emptyList()),
            )
        }

    @Test
    fun testDeleteItemBy() =
        runBlocking {
            // given
            val expectedMetadata = TEST_Metadata.copy(handle = "delete_item_meta")
            val expectedRight = TEST_RIGHT.copy(templateName = null, isTemplate = false)

            // when
            dbConnector.metadataDB.insertMetadata(expectedMetadata)
            val generatedRightId = dbConnector.rightDB.insertRight(expectedRight)
            dbConnector.itemDB.insertItemBatch(
                listOf(
                    ItemId(
                        handle = expectedMetadata.handle,
                        rightId = generatedRightId,
                    ),
                ),
            )

            // then
            assertThat(
                dbConnector.rightDB.getRightIdsByHandle(expectedMetadata.handle),
                `is`(listOf(generatedRightId)),
            )

            val deletedItemsByMetadata = dbConnector.itemDB.deleteItemByHandle(expectedMetadata.handle)
            assertThat(
                deletedItemsByMetadata,
                `is`(1),
            )

            assertThat(
                dbConnector.rightDB.getRightIdsByHandle(expectedMetadata.handle),
                `is`(emptyList()),
            )

            // when
            dbConnector.itemDB.insertItem(
                ItemId(
                    handle = expectedMetadata.handle,
                    rightId = generatedRightId,
                ),
            )
            // then
            assertThat(
                dbConnector.rightDB.getRightIdsByHandle(expectedMetadata.handle),
                `is`(listOf(generatedRightId)),
            )

            val deletedItemsByRight = dbConnector.itemDB.deleteItemByRightId(generatedRightId)
            assertThat(
                deletedItemsByRight,
                `is`(1),
            )

            assertThat(
                dbConnector.rightDB.getRightIdsByHandle(expectedMetadata.handle),
                `is`(emptyList()),
            )
        }

    @Test
    fun testItemExists() =
        runBlocking {
            // given
            val expectedMetadata = TEST_Metadata.copy(handle = "item_exists_metadata")
            val expectedRight = TEST_RIGHT.copy(templateName = null, isTemplate = false)

            assertFalse(dbConnector.itemDB.itemContainsRightId(expectedRight.rightId!!))
            assertFalse(dbConnector.itemDB.itemContainsEntry(expectedMetadata.handle, expectedRight.rightId))
            assertFalse(dbConnector.metadataDB.itemContainsHandle(expectedMetadata.handle))
            // when
            dbConnector.metadataDB.insertMetadata(expectedMetadata)
            val generatedRightId = dbConnector.rightDB.insertRight(expectedRight)
            dbConnector.itemDB.insertItem(
                ItemId(
                    handle = expectedMetadata.handle,
                    rightId = generatedRightId,
                ),
            )

            // then
            assertTrue(dbConnector.itemDB.itemContainsRightId(generatedRightId))
            assertTrue(dbConnector.metadataDB.itemContainsHandle(expectedMetadata.handle))
            assertTrue(dbConnector.itemDB.itemContainsEntry(expectedMetadata.handle, generatedRightId))
            assertThat(dbConnector.itemDB.countItemByRightId(generatedRightId), `is`(1))
        }

    @Test
    fun testItemBatchInsert() =
        runBlocking {
            // Given
            val expectedMetadata1 = TEST_Metadata.copy(handle = "item_1")
            val expectedMetadata2 = TEST_Metadata.copy(handle = "item_2")
            val expectedRight1 = TEST_RIGHT.copy(rightId = "right1", templateName = null, isTemplate = false)
            val expectedRight2 = TEST_RIGHT.copy(rightId = "right2", templateName = null, isTemplate = false)
            dbConnector.metadataDB.insertMetadata(expectedMetadata1)
            val generatedRightId1 = dbConnector.rightDB.insertRight(expectedRight1)
            dbConnector.metadataDB.insertMetadata(expectedMetadata2)
            val generatedRightId2 = dbConnector.rightDB.insertRight(expectedRight2)

            // When
            dbConnector.itemDB.insertItem(
                ItemId(
                    handle = expectedMetadata1.handle,
                    rightId = generatedRightId1,
                ),
            )

            dbConnector.itemDB.insertItem(
                ItemId(
                    handle = expectedMetadata2.handle,
                    rightId = generatedRightId2,
                ),
            )

            // then
            assertThat(
                dbConnector.itemDB.getAllHandles().size,
                `is`(2),
            )
        }

    companion object {
        val NOW: OffsetDateTime =
            OffsetDateTime.of(
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

        val TEST_Metadata =
            ItemMetadata(
                author = "Colbjørnsen, Terje",
                band = "band",
                collectionName = "collectionName",
                collectionHandle = "colHandle",
                communityHandle = "comHandle",
                communityName = "communityName",
                createdBy = "user1",
                createdOn = NOW,
                deleted = false,
                doi = listOf("10.992", "10.001"),
                handle = "hdl:example.handle.net",
                isbn = listOf("12345", "67890123"),
                issn = "123456",
                isPartOfSeries = listOf("series123"),
                lastUpdatedBy = "user2",
                lastUpdatedOn = NOW,
                licenceUrl = "https://creativecommons.org/licenses/by-sa/4.0/legalcode.de",
                licenceUrlFilter = "by-sa/4.0/legalcode.de",
                paketSigel = listOf("sigel"),
                ppn = "ppn",
                publicationType = PublicationType.ARTICLE,
                publicationYear = 2022,
                subCommunityHandle = "11509/1111",
                subCommunityName = "Department of University of Foo",
                storageDate = NOW.minusDays(3),
                title = "Important title",
                titleJournal = "anything",
                titleSeries = null,
                zdbIds = listOf("some journal id"),
            )

        val TEST_RIGHT =
            ItemRight(
                rightId = "testright",
                accessState = AccessState.OPEN,
                authorRightException = true,
                basisAccessState = BasisAccessState.LICENCE_CONTRACT,
                basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
                createdBy = "user1",
                createdOn = NOW,
                endDate = TODAY,
                exceptionFrom = null,
                hasLegalRisk = true,
                groups = emptyList(),
                groupIds = emptyList(),
                isTemplate = false,
                lastUpdatedBy = "user2",
                lastUpdatedOn = NOW,
                lastAppliedOn = NOW.minusDays(1),
                licenceContract = "some contract",
                notesGeneral = "Some general notes",
                notesFormalRules = "Some formal rule notes",
                notesProcessDocumentation = "Some process documentation",
                notesManagementRelated = "Some management related notes",
                restrictedOpenContentLicence = false,
                startDate = TODAY.minusDays(1),
                templateDescription = "description",
                templateName = "name",
                zbwUserAgreement = true,
            )
    }
}
