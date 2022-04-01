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
                metadataId = TEST_METADATA.id,
                band = TEST_METADATA.band,
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

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
        val TEST_METADATA = ItemMetadata(
            id = "that-test",
            band = "band",
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
    }
}
