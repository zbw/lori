package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
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
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
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
    fun testDeleteItem() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "item_roundtrip_meta")
        val expectedRight = TEST_RIGHT

        // when
        dbConnector.metadataDB.insertMetadata(expectedMetadata)
        val generatedRightId = dbConnector.rightDB.insertRight(expectedRight)
        dbConnector.itemDB.insertItem(expectedMetadata.metadataId, generatedRightId)

        // then
        assertThat(
            dbConnector.rightDB.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(generatedRightId))
        )

        val deletedItems = dbConnector.itemDB.deleteItem(expectedMetadata.metadataId, generatedRightId)
        assertThat(
            deletedItems,
            `is`(1),
        )

        assertThat(
            dbConnector.rightDB.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )
    }

    @Test
    fun testDeleteItemBy() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "delete_item_meta")
        val expectedRight = TEST_RIGHT.copy(templateName = null, isTemplate = false)

        // when
        dbConnector.metadataDB.insertMetadata(expectedMetadata)
        val generatedRightId = dbConnector.rightDB.insertRight(expectedRight)
        dbConnector.itemDB.insertItem(expectedMetadata.metadataId, generatedRightId)

        // then
        assertThat(
            dbConnector.rightDB.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(generatedRightId))
        )

        val deletedItemsByMetadata = dbConnector.itemDB.deleteItemByMetadataId(expectedMetadata.metadataId)
        assertThat(
            deletedItemsByMetadata,
            `is`(1),
        )

        assertThat(
            dbConnector.rightDB.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )

        // when
        dbConnector.itemDB.insertItem(expectedMetadata.metadataId, generatedRightId)
        // then
        assertThat(
            dbConnector.rightDB.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(generatedRightId))
        )

        val deletedItemsByRight = dbConnector.itemDB.deleteItemByRightId(generatedRightId)
        assertThat(
            deletedItemsByRight,
            `is`(1),
        )

        assertThat(
            dbConnector.rightDB.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )
    }

    @Test
    fun testItemExists() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "item_exists_metadata")
        val expectedRight = TEST_RIGHT.copy(templateName = null, isTemplate = false)

        assertFalse(dbConnector.itemDB.itemContainsRightId(expectedRight.rightId!!))
        assertFalse(dbConnector.itemDB.itemContainsEntry(expectedMetadata.metadataId, expectedRight.rightId!!))
        assertFalse(dbConnector.metadataDB.itemContainsMetadata(expectedMetadata.metadataId))
        // when
        dbConnector.metadataDB.insertMetadata(expectedMetadata)
        val generatedRightId = dbConnector.rightDB.insertRight(expectedRight)
        dbConnector.itemDB.insertItem(expectedMetadata.metadataId, generatedRightId)

        // then
        assertTrue(dbConnector.itemDB.itemContainsRightId(generatedRightId))
        assertTrue(dbConnector.metadataDB.itemContainsMetadata(expectedMetadata.metadataId))
        assertTrue(dbConnector.itemDB.itemContainsEntry(expectedMetadata.metadataId, generatedRightId))
        assertThat(dbConnector.itemDB.countItemByRightId(generatedRightId), `is`(1))
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
            author = "Colbjørnsen, Terje",
            band = "band",
            collectionName = "collectionName",
            collectionHandle = "colHandle",
            communityHandle = "comHandle",
            communityName = "communityName",
            createdBy = "user1",
            createdOn = NOW,
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            isPartOfSeries = "series123",
            lastUpdatedBy = "user2",
            lastUpdatedOn = NOW,
            licenceUrl = "https://creativecommons.org/licenses/by-sa/4.0/legalcode.de",
            paketSigel = "sigel",
            ppn = "ppn",
            publicationType = PublicationType.ARTICLE,
            publicationDate = LocalDate.of(2022, 9, 26),
            rightsK10plus = "some rights",
            subCommunityHandle = "11509/1111",
            subCommunityName = "Department of University of Foo",
            storageDate = NOW.minusDays(3),
            title = "Important title",
            titleJournal = "anything",
            titleSeries = null,
            zdbIdJournal = "some journal id",
            zdbIdSeries = "some series id",
        )

        val TEST_RIGHT = ItemRight(
            rightId = "testright",
            accessState = AccessState.OPEN,
            authorRightException = true,
            basisAccessState = BasisAccessState.LICENCE_CONTRACT,
            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
            createdBy = "user1",
            createdOn = NOW,
            endDate = TODAY,
            exceptionFrom = null,
            groupIds = emptyList(),
            isTemplate = false,
            lastUpdatedBy = "user2",
            lastUpdatedOn = NOW,
            lastAppliedOn = NOW.minusDays(1),
            licenceContract = "some contract",
            nonStandardOpenContentLicence = true,
            nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
            notesGeneral = "Some general notes",
            notesFormalRules = "Some formal rule notes",
            notesProcessDocumentation = "Some process documentation",
            notesManagementRelated = "Some management related notes",
            openContentLicence = "some licence",
            restrictedOpenContentLicence = false,
            startDate = TODAY.minusDays(1),
            templateDescription = "description",
            templateName = "name",
            zbwUserAgreement = true,
        )
    }
}
