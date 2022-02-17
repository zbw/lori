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
import de.zbw.lori.api.ActionProto
import de.zbw.lori.api.ActionTypeProto
import de.zbw.lori.api.AttributeProto
import de.zbw.lori.api.AttributeTypeProto
import de.zbw.lori.api.ItemProto
import de.zbw.lori.api.RestrictionProto
import de.zbw.lori.api.RestrictionTypeProto
import io.grpc.StatusRuntimeException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.AssertJUnit.fail
import org.testng.annotations.Test

/**
 * Tests for protobuf convertions.
 *
 * Created on 07-23-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ProtoConverterTest {

    @Test
    fun testToBusinessConversion() {
        // given
        val expected = Item(
            itemMetadata = TEST_Metadata.copy(zbdId = null),
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

        val protoObject = ItemProto.newBuilder()
            .setId(expected.itemMetadata.id)
            .setAccessState(expected.itemMetadata.accessState?.toProto())
            .setBand(expected.itemMetadata.band)
            .setDoi(expected.itemMetadata.doi)
            .setHandle(expected.itemMetadata.handle)
            .setIsbn(expected.itemMetadata.isbn)
            .setIssn(expected.itemMetadata.issn)
            .setLicenseConditions(expected.itemMetadata.licenseConditions)
            .setPaketSigel(expected.itemMetadata.paketSigel)
            .setPpn(expected.itemMetadata.ppn)
            .setPpnEbook(expected.itemMetadata.ppnEbook)
            .setProvenanceLicense(expected.itemMetadata.provenanceLicense)
            .setPublicationType(expected.itemMetadata.publicationType.toProto())
            .setPublicationYear(expected.itemMetadata.publicationYear)
            .setRightsK10Plus(expected.itemMetadata.rightsK10plus)
            .setSerialNumber(expected.itemMetadata.serialNumber)
            .setTitle(expected.itemMetadata.title)
            .setTitleJournal(expected.itemMetadata.titleJournal)
            .setTitleSeries(expected.itemMetadata.titleSeries)
            .setId(expected.itemMetadata.id)
            .addAllActions(
                listOf(
                    ActionProto.newBuilder()
                        .setType(ActionTypeProto.ACTION_TYPE_PROTO_READ)
                        .setPermission(true)
                        .addAllRestrictions(
                            listOf(
                                RestrictionProto.newBuilder()
                                    .setType(RestrictionTypeProto.RESTRICTION_TYPE_PROTO_DATE)
                                    .setAttribute(
                                        AttributeProto.newBuilder()
                                            .setType(AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_FROM_DATE)
                                            .addAllValues(listOf("2022-01-01"))
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                )
            )
            .build()

        // when + then
        assertThat(protoObject.toBusiness(), `is`(expected))
    }

    @Test
    fun testActionTypeConversionRoundtrip() {
        ActionTypeProto.values().toList().forEach {
            when (it) {
                ActionTypeProto.ACTION_TYPE_PROTO_UNSPECIFIED, ActionTypeProto.UNRECOGNIZED -> try {
                    it.toBusiness()
                    fail("An exception should have been thrown")
                } catch (sre: StatusRuntimeException) {
                }
                else -> assertThat(it.toBusiness().toProto(), `is`(it))
            }
        }
    }

    @Test
    fun testAttributeTypeConversionRoundtrip() {
        AttributeTypeProto.values().toList().forEach {
            when (it) {
                AttributeTypeProto.ATTRIBUTE_TYPE_PROTO_UNSPECIFIED, AttributeTypeProto.UNRECOGNIZED -> try {
                    it.toBusiness()
                    fail("An exception should have been thrown")
                } catch (sre: StatusRuntimeException) {
                }
                else -> assertThat(
                    it.toBusiness().toProto(),
                    `is`(it)
                )
            }
        }
    }

    @Test
    fun testRestrictionTypeConversionRoundtrip() {
        RestrictionTypeProto.values().toList().forEach {
            when (it) {
                RestrictionTypeProto.RESTRICTION_TYPE_PROTO_UNSPECIFIED, RestrictionTypeProto.UNRECOGNIZED ->
                    try {
                        it.toBusiness()
                        fail("An exception should have been thrown")
                    } catch (sre: StatusRuntimeException) {
                    }
                else -> assertThat(
                    it.toBusiness().toProto(),
                    `is`(it)
                )
            }
        }
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
            publicationType = PublicationType.BOOK,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = "Journal title",
            titleSeries = "Title series",
            zbdId = "zbd id",
        )
    }
}
