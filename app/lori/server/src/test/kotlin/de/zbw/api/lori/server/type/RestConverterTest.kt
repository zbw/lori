package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.Action
import de.zbw.business.lori.server.ActionType
import de.zbw.business.lori.server.Attribute
import de.zbw.business.lori.server.AttributeType
import de.zbw.business.lori.server.Item
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.PublicationType
import de.zbw.business.lori.server.Restriction
import de.zbw.business.lori.server.RestrictionType
import de.zbw.lori.model.ActionRest
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.RestrictionRest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class RestConverterTest {

    @Test
    fun testAccessRightConversion() {
        // given
        val expected = Item(
            itemMetadata = TEST_Metadata,
            actions = listOf(
                Action(
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
            ),
        )

        val restObject = ItemRest(
            id = TEST_Metadata.id,
            accessState = TEST_Metadata.accessState?.toRest(),
            band = TEST_Metadata.band,
            createdBy = TEST_Metadata.createdBy,
            createdOn = TEST_Metadata.createdOn,
            doi = TEST_Metadata.doi,
            handle = TEST_Metadata.handle,
            isbn = TEST_Metadata.isbn,
            issn = TEST_Metadata.issn,
            lastUpdatedBy = TEST_Metadata.lastUpdatedBy,
            lastUpdatedOn = TEST_Metadata.lastUpdatedOn,
            licenseConditions = TEST_Metadata.licenseConditions,
            paketSigel = TEST_Metadata.paketSigel,
            provenanceLicense = TEST_Metadata.provenanceLicense,
            ppn = TEST_Metadata.ppn,
            ppnEbook = TEST_Metadata.ppnEbook,
            publicationType = TEST_Metadata.publicationType.toRest(),
            publicationYear = TEST_Metadata.publicationYear,
            rightsK10plus = TEST_Metadata.rightsK10plus,
            serialNumber = TEST_Metadata.serialNumber,
            title = TEST_Metadata.title,
            titleJournal = TEST_Metadata.titleJournal,
            titleSeries = TEST_Metadata.titleSeries,
            zbdId = TEST_Metadata.zbdId,
            actions = listOf(
                ActionRest(
                    permission = true,
                    actiontype = ActionRest.Actiontype.read,
                    restrictions = listOf(
                        RestrictionRest(
                            restrictiontype = RestrictionRest.Restrictiontype.date,
                            attributetype = RestrictionRest.Attributetype.fromdate,
                            attributevalues = listOf("2022-01-01"),
                        )
                    )
                )
            ),
        )

        // when + then
        assertThat(restObject.toBusiness(), `is`(expected))
    }

    @Test
    fun testActionTypeConversionRoundtrip() {
        ActionRest.Actiontype.values().toList().forEach {
            assertThat(it.toBusiness().toRest(), `is`(it))
        }
    }

    @Test
    fun testAttributeTypeConversionRoundtrip() {
        RestrictionRest.Attributetype.values().toList().forEach {
            assertThat(
                it.toBusiness().toRest(),
                `is`(it)
            )
        }
    }

    @Test
    fun testRestrictionTypeConversionRoundtrip() {
        RestrictionRest.Restrictiontype.values().toList().forEach {
            assertThat(
                it.toBusiness().toRest(),
                `is`(it)
            )
        }
    }

    companion object {
        val TEST_Metadata = ItemMetadata(
            id = "that-test",
            accessState = AccessState.OPEN,
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
            licenseConditions = "some conditions",
            paketSigel = "sigel",
            ppn = "ppn",
            ppnEbook = "ppn ebook",
            provenanceLicense = "provenance license",
            publicationType = PublicationType.BOOK,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = null,
        )
    }
}
