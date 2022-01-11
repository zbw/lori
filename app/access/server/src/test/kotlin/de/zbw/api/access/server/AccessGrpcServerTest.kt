package de.zbw.api.access.server

import de.zbw.access.api.AccessRightProto
import de.zbw.access.api.ActionProto
import de.zbw.access.api.ActionTypeProto
import de.zbw.access.api.AddAccessInformationRequest
import de.zbw.access.api.AddAccessInformationResponse
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.AttributeTypeProto
import de.zbw.access.api.GetAccessInformationRequest
import de.zbw.access.api.GetAccessInformationResponse
import de.zbw.access.api.RestrictionProto
import de.zbw.access.api.RestrictionTypeProto
import de.zbw.business.access.server.AccessServerBackend
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Item
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.sql.SQLException

/**
 * Test [AccessGrpcServer].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessGrpcServerTest {

    @Test
    fun testAddAccessInformation() {
        runBlocking {
            val request = AddAccessInformationRequest
                .newBuilder()
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { insertAccessRightEntry(any()) } returns "foo"
            }

            val response = AccessGrpcServer(mockk(), backendMockk).addAccessInformation(request)
            assertThat(response, `is`(AddAccessInformationResponse.getDefaultInstance()))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testAddAccessInformationError() {
        runBlocking {
            val request = AddAccessInformationRequest
                .newBuilder()
                .addAllItems(
                    listOf(
                        AccessRightProto
                            .newBuilder()
                            .setId("foo")
                            .build()
                    )
                )
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { insertAccessRightEntry(any()) } throws SQLException()
            }

            AccessGrpcServer(mockk(), backendMockk).addAccessInformation(request)
        }
    }

    @Test
    fun testGetAccessInformation() {
        runBlocking {
            val request = GetAccessInformationRequest
                .newBuilder()
                .addAllIds(listOf("foo"))
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { getAccessRightEntries(any()) } returns listOf(
                    Item(
                        metadata = de.zbw.business.access.server.Metadata(
                            id = "foo",
                            tenant = "www.zbw.eu",
                            usageGuide = "usageGuide",
                            template = "CC",
                            mention = true,
                            shareAlike = true,
                            commercialUse = false,
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
                                            values = listOf("2022-01-01"),
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }

            val response = AccessGrpcServer(mockk(), backendMockk).getAccessInformation(request)
            assertThat(
                response,
                `is`(
                    GetAccessInformationResponse.newBuilder()
                        .addAllAccessRights(
                            listOf(
                                AccessRightProto.newBuilder()
                                    .setId("foo")
                                    .setTenant("www.zbw.eu")
                                    .setUsageGuide("usageGuide")
                                    .setTemplate("CC")
                                    .setMention(true)
                                    .setSharealike(true)
                                    .setCommercialuse(false)
                                    .setCopyright(true)
                                    .addAllActions(
                                        listOf(
                                            ActionProto.newBuilder()
                                                .setPermission(true)
                                                .setType(ActionTypeProto.ACTION_TYPE_PROTO_READ)
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
                            )
                        )
                        .build()
                )
            )
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testGetAccessInformationError() {
        runBlocking {
            val request = GetAccessInformationRequest
                .newBuilder()
                .addAllIds(listOf("foo"))
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { getAccessRightEntries(any()) } throws SQLException()
            }

            AccessGrpcServer(mockk(), backendMockk).getAccessInformation(request)
        }
    }
}
