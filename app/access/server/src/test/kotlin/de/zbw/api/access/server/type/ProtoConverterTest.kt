package de.zbw.api.access.server.type

import de.zbw.access.api.AccessRightProto
import de.zbw.access.api.ActionProto
import de.zbw.access.api.ActionTypeProto
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.AttributeTypeProto
import de.zbw.access.api.RestrictionProto
import de.zbw.access.api.RestrictionTypeProto
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Item
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
    fun testAccessRightConversion() {
        // given
        val expected = Item(
            metadata = de.zbw.business.access.server.Metadata(
                id = "foo",
                tenant = "bla",
                usageGuide = "guide",
                template = null,
                mention = true,
                shareAlike = false,
                commercialUse = true,
                copyright = true,
            ),
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

        val protoObject = AccessRightProto.newBuilder()
            .setId(expected.metadata.id)
            .setTenant(expected.metadata.tenant)
            .setUsageGuide(expected.metadata.usageGuide)
            .setMention(true)
            .setSharealike(false)
            .setCommercialuse(true)
            .setCopyright(true)
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
}
