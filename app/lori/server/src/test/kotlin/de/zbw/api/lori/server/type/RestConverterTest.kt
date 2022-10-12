package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.BasisAccessState
import de.zbw.business.lori.server.BasisStorage
import de.zbw.business.lori.server.Item
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.PublicationType
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.RightRest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RestConverterTest {

    @Test
    fun testItemConversion() {
        // given
        val expected = Item(
            metadata = TEST_METADATA,
            rights = listOf(TEST_RIGHT)
        )

        val restObject = ItemRest(
            metadata = MetadataRest(
                metadataId = TEST_METADATA.metadataId,
                author = TEST_METADATA.author,
                band = TEST_METADATA.band,
                collectionName = TEST_METADATA.collectionName,
                communityName = TEST_METADATA.communityName,
                createdBy = TEST_METADATA.createdBy,
                createdOn = TEST_METADATA.createdOn,
                doi = TEST_METADATA.doi,
                handle = TEST_METADATA.handle,
                isbn = TEST_METADATA.isbn,
                issn = TEST_METADATA.issn,
                lastUpdatedBy = TEST_METADATA.lastUpdatedBy,
                lastUpdatedOn = TEST_METADATA.lastUpdatedOn,
                paketSigel = TEST_METADATA.paketSigel,
                ppn = TEST_METADATA.ppn,
                publicationType = TEST_METADATA.publicationType.toRest(),
                publicationDate = TEST_METADATA.publicationDate,
                rightsK10plus = TEST_METADATA.rightsK10plus,
                storageDate = TEST_METADATA.storageDate,
                title = TEST_METADATA.title,
                titleJournal = TEST_METADATA.titleJournal,
                titleSeries = TEST_METADATA.titleSeries,
                zbdId = TEST_METADATA.zbdId,
            ),
            rights = listOf(
                RightRest(
                    rightId = TEST_RIGHT.rightId,
                    accessState = TEST_RIGHT.accessState?.toRest(),
                    authorRightException = TEST_RIGHT.authorRightException,
                    basisAccessState = TEST_RIGHT.basisAccessState?.toRest(),
                    basisStorage = TEST_RIGHT.basisStorage?.toRest(),
                    createdBy = TEST_RIGHT.createdBy,
                    createdOn = TEST_RIGHT.createdOn,
                    endDate = TEST_RIGHT.endDate,
                    lastUpdatedBy = TEST_RIGHT.lastUpdatedBy,
                    lastUpdatedOn = TEST_RIGHT.lastUpdatedOn,
                    licenceContract = TEST_RIGHT.licenceContract,
                    nonStandardOpenContentLicence = TEST_RIGHT.nonStandardOpenContentLicence,
                    nonStandardOpenContentLicenceURL = TEST_RIGHT.nonStandardOpenContentLicenceURL,
                    notesGeneral = TEST_RIGHT.notesGeneral,
                    notesFormalRules = TEST_RIGHT.notesFormalRules,
                    notesProcessDocumentation = TEST_RIGHT.notesProcessDocumentation,
                    notesManagementRelated = TEST_RIGHT.notesManagementRelated,
                    openContentLicence = TEST_RIGHT.openContentLicence,
                    restrictedOpenContentLicence = TEST_RIGHT.restrictedOpenContentLicence,
                    startDate = TEST_RIGHT.startDate,
                    zbwUserAgreement = TEST_RIGHT.zbwUserAgreement,
                )
            ),
        )

        // when + then
        assertThat(restObject.toBusiness(), `is`(expected))
        assertThat(restObject.toBusiness().toRest(), `is`(restObject))
    }

    @Test
    fun testDAItemConverter() {
        // given
        val expected = ItemMetadata(
            metadataId = "5",
            author = "Colbjørnsen, Terje",
            band = null,
            collectionName = "Collectionname",
            communityName = "Communityname",
            createdBy = null,
            createdOn = null,
            doi = null,
            handle = "some_handle",
            isbn = null,
            issn = null,
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            paketSigel = null,
            ppn = null,
            publicationType = PublicationType.ARTICLE,
            publicationDate = LocalDate.of(2022, 9, 1),
            rightsK10plus = null,
            storageDate = OffsetDateTime.of(
                2022,
                1,
                19,
                7,
                57,
                26,
                0,
                ZoneOffset.UTC,
            ),
            title = "some_title",
            titleJournal = "some_journal",
            titleSeries = "some_series",
            zbdId = null,
        )

        // when
        val receivedItem = TEST_DA_ITEM.toBusiness()
        // then
        assertThat(expected, `is`(receivedItem))

        // when + then
        val receivedItem2 = TEST_DA_ITEM.copy(handle = null)
        assertThat(receivedItem2, `is`(receivedItem2))
    }

    @Test
    fun testParseToDate() {
        // when + then
        assertThat(
            RestConverter.parseToDate("2022"),
            `is`(LocalDate.of(2022, 1, 1))
        )
        assertThat(
            RestConverter.parseToDate("2022-09"),
            `is`(LocalDate.of(2022, 9, 1))
        )
        assertThat(
            RestConverter.parseToDate("2022-09-02"),
            `is`(LocalDate.of(2022, 9, 2))
        )
        assertThat(
            RestConverter.parseToDate("2022/09"),
            `is`(LocalDate.of(2022, 9, 1))
        )
        assertThat(
            RestConverter.parseToDate("foo"),
            `is`(LocalDate.of(1970, 1, 1))
        )
    }

    companion object {
        val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
        val TEST_METADATA = ItemMetadata(
            metadataId = "that-test",
            author = "Colbjørnsen, Terje",
            band = "band",
            collectionName = "Collectioname",
            communityName = "Communityname",
            createdBy = "user1",
            createdOn = OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            lastUpdatedBy = "user2",
            lastUpdatedOn = OffsetDateTime.of(
                2022,
                3,
                2,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            paketSigel = "sigel",
            ppn = "ppn",
            publicationType = PublicationType.BOOK,
            publicationDate = LocalDate.of(2022, 9, 1),
            rightsK10plus = "some rights",
            storageDate = OffsetDateTime.now(),
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = null,
        )

        val TEST_RIGHT = ItemRight(
            rightId = "123",
            accessState = AccessState.CLOSED,
            authorRightException = true,
            basisAccessState = BasisAccessState.LICENCE_CONTRACT,
            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
            createdBy = "user1",
            createdOn = OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            endDate = TODAY,
            lastUpdatedBy = "user2",
            lastUpdatedOn = OffsetDateTime.of(
                2022,
                3,
                2,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            startDate = TODAY.minusDays(1),
            licenceContract = "some contract",
            nonStandardOpenContentLicence = true,
            nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
            notesGeneral = "Some general notes",
            notesFormalRules = "Some formal rule notes",
            notesProcessDocumentation = "Some process documentation",
            notesManagementRelated = "Some management related notes",
            openContentLicence = "some licence",
            restrictedOpenContentLicence = false,
            zbwUserAgreement = true,
        )

        val TEST_DA_ITEM = DAItem(
            id = 5,
            name = "name",
            handle = "handle",
            type = "type",
            link = "link",
            expand = listOf("foo"),
            lastModified = "2020-10-04",
            parentCollection = DACollection(
                id = 3,
                name = "Collectionname",
                handle = null,
                type = null,
                link = "link",
                expand = emptyList(),
                logo = null,
                parentCommunity = null,
                copyrightText = null,
                introductoryText = null,
                shortDescription = null,
                sidebarText = null,
                items = emptyList(),
                license = null,
                numberItems = 4,
                parentCommunityList = emptyList(),
            ),
            parentCollectionList = emptyList(),
            parentCommunityList = listOf(
                DACommunity(
                    id = 1,
                    name = "Communityname",
                    handle = null,
                    type = null,
                    countItems = null,
                    link = "link",
                    expand = emptyList(),
                    logo = null,
                    parentCommunity = null,
                    copyrightText = null,
                    introductoryText = null,
                    shortDescription = null,
                    sidebarText = null,
                    subcommunities = emptyList(),
                    collections = emptyList(),
                )
            ),
            metadata = listOf(
                DAMetadata(
                    key = "dc.identifier.uri",
                    value = "some_handle",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.type",
                    value = "article",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.contributor.author",
                    value = "Colbjørnsen, Terje",
                    language = null,
                ),
                DAMetadata(
                    key = "dc.date.issued",
                    value = "2022-09",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.date.accessioned",
                    value = "2022-01-19T07:57:26Z",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.title",
                    value = "some_title",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.journalname",
                    value = "some_journal",
                    language = "DE",
                ),
                DAMetadata(
                    key = "dc.seriesname",
                    value = "some_series",
                    language = "DE",
                ),
            ),
            bitstreams = emptyList(),
            archived = "archived",
            withdrawn = "withdrawn",
        )
    }
}
