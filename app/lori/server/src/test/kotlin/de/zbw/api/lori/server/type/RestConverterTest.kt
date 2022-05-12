package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.AccessState
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
                ppnEbook = TEST_METADATA.ppnEbook,
                publicationType = TEST_METADATA.publicationType.toRest(),
                publicationYear = TEST_METADATA.publicationYear,
                rightsK10plus = TEST_METADATA.rightsK10plus,
                serialNumber = TEST_METADATA.serialNumber,
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
                    createdBy = TEST_RIGHT.createdBy,
                    createdOn = TEST_RIGHT.createdOn,
                    endDate = TEST_RIGHT.endDate,
                    lastUpdatedBy = TEST_RIGHT.lastUpdatedBy,
                    lastUpdatedOn = TEST_RIGHT.lastUpdatedOn,
                    licenseConditions = TEST_RIGHT.licenseConditions,
                    startDate = TEST_RIGHT.startDate,
                    provenanceLicense = TEST_RIGHT.provenanceLicense,
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
            ppnEbook = null,
            publicationType = PublicationType.ARTICLE,
            publicationYear = 2020,
            rightsK10plus = null,
            serialNumber = null,
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

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
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
            ppnEbook = "ppn ebook",
            publicationType = PublicationType.BOOK,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            storageDate = OffsetDateTime.now(),
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = null,
        )

        val TEST_RIGHT = ItemRight(
            rightId = "rightId",
            accessState = AccessState.CLOSED,
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
            licenseConditions = "license",
            provenanceLicense = "provenance",
            startDate = TODAY.minusDays(1),
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
                    value = "2020",
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
