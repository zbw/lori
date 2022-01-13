package de.zbw.api.access.server.type

import de.zbw.access.api.ActionProto
import de.zbw.access.api.ActionTypeProto
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.AttributeTypeProto
import de.zbw.access.api.ItemProto
import de.zbw.access.api.RestrictionProto
import de.zbw.access.api.RestrictionTypeProto
import de.zbw.business.access.server.AccessState
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Item
import de.zbw.business.access.server.Metadata
import de.zbw.business.access.server.PublicationType
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
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
            metadata = TEST_Metadata.copy(zbd_id = null),
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
            .setId(expected.metadata.id)
            .setAccessState(expected.metadata.access_state?.toProto())
            .setBand(expected.metadata.band)
            .setDoi(expected.metadata.doi)
            .setHandle(expected.metadata.handle)
            .setIsbn(expected.metadata.isbn)
            .setIssn(expected.metadata.issn)
            .setPaketSigel(expected.metadata.paket_sigel)
            .setPpn(expected.metadata.ppn)
            .setPpnEbook(expected.metadata.ppn_ebook)
            .setPublicationType(expected.metadata.publicationType.toProto())
            .setPublicationYear(expected.metadata.publicationYear)
            .setRightsK10Plus(expected.metadata.rights_k10plus)
            .setSerialNumber(expected.metadata.serialNumber)
            .setTitle(expected.metadata.title)
            .setTitleJournal(expected.metadata.title_journal)
            .setTitleSeries(expected.metadata.title_series)
            .setId(expected.metadata.id)
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
        val TEST_Metadata = Metadata(
            id = "that-test",
            access_state = AccessState.OPEN,
            band = "band",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            paket_sigel = "sigel",
            ppn = "ppn",
            ppn_ebook = "ppn ebook",
            publicationType = PublicationType.MONO,
            publicationYear = 2000,
            rights_k10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            title_journal = "Journal title",
            title_series = "Title series",
            zbd_id = "zbd id",
        )
    }
}
