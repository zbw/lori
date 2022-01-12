package de.zbw.api.access.server.type

import de.zbw.access.model.ItemRest
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Item
import de.zbw.business.access.server.Metadata
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

class RestConverterTest {

    @Test
    fun testAccessRightConversion() {
        // given
        val expected = Item(
            metadata = TEST_Metadata,
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
            accessState = TEST_Metadata.access_state,
            band = TEST_Metadata.band,
            doi = TEST_Metadata.doi,
            handle = TEST_Metadata.handle,
            isbn = TEST_Metadata.isbn,
            issn = TEST_Metadata.issn,
            paketSigel = TEST_Metadata.paket_sigel,
            ppn = TEST_Metadata.ppn,
            ppnEbook = TEST_Metadata.ppn_ebook,
            publicationType = TEST_Metadata.publicationType,
            publicationYear = TEST_Metadata.publicationYear,
            rightsK10plus = TEST_Metadata.rights_k10plus,
            serialNumber = TEST_Metadata.serialNumber,
            title = TEST_Metadata.title,
            titleJournal = TEST_Metadata.title_journal,
            titleSeries = TEST_Metadata.title_series,
            zbdId = TEST_Metadata.zbd_id,
            actions = listOf(
                de.zbw.access.model.ActionRest(
                    permission = true,
                    actiontype = de.zbw.access.model.ActionRest.Actiontype.read,
                    restrictions = listOf(
                        de.zbw.access.model.RestrictionRest(
                            restrictiontype = de.zbw.access.model.RestrictionRest.Restrictiontype.date,
                            attributetype = de.zbw.access.model.RestrictionRest.Attributetype.fromdate,
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
        de.zbw.access.model.ActionRest.Actiontype.values().toList().forEach {
            assertThat(it.toBusiness().toRest(), `is`(it))
        }
    }

    @Test
    fun testAttributeTypeConversionRoundtrip() {
        de.zbw.access.model.RestrictionRest.Attributetype.values().toList().forEach {
            assertThat(
                it.toBusiness().toRest(),
                `is`(it)
            )
        }
    }

    @Test
    fun testRestrictionTypeConversionRoundtrip() {
        de.zbw.access.model.RestrictionRest.Restrictiontype.values().toList().forEach {
            assertThat(
                it.toBusiness().toRest(),
                `is`(it)
            )
        }
    }

    companion object {
        val TEST_Metadata = Metadata(
            id = "that-test",
            access_state = "open",
            band = "band",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            paket_sigel = "sigel",
            ppn = "ppn",
            ppn_ebook = "ppn ebook",
            publicationType = "publicationType",
            publicationYear = 2000,
            rights_k10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            title_journal = null,
            title_series = null,
            zbd_id = null,
        )
    }
}
