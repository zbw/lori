package de.zbw.api.access.server

import de.zbw.access.api.ActionProto
import de.zbw.access.api.ActionTypeProto
import de.zbw.access.api.AddItemRequest
import de.zbw.access.api.AddItemResponse
import de.zbw.access.api.AttributeProto
import de.zbw.access.api.AttributeTypeProto
import de.zbw.access.api.GetItemRequest
import de.zbw.access.api.GetItemResponse
import de.zbw.access.api.ItemProto
import de.zbw.access.api.RestrictionProto
import de.zbw.access.api.RestrictionTypeProto
import de.zbw.api.access.server.type.toProto
import de.zbw.business.access.server.AccessServerBackend
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
    fun testAddItem() {
        runBlocking {
            val request = AddItemRequest
                .newBuilder()
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { insertAccessRightEntry(any()) } returns "foo"
            }

            val response = AccessGrpcServer(mockk(), backendMockk).addItem(request)
            assertThat(response, `is`(AddItemResponse.getDefaultInstance()))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testAddItemError() {
        runBlocking {
            val request = AddItemRequest
                .newBuilder()
                .addAllItems(
                    listOf(
                        ItemProto
                            .newBuilder()
                            .setId("foo")
                            .build()
                    )
                )
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { insertAccessRightEntry(any()) } throws SQLException()
            }

            AccessGrpcServer(mockk(), backendMockk).addItem(request)
        }
    }

    @Test
    fun testGetItem() {
        runBlocking {
            val request = GetItemRequest
                .newBuilder()
                .addAllIds(listOf("foo"))
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { getAccessRightEntries(any()) } returns listOf(
                    Item(
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
                                            values = listOf("2022-01-01"),
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }

            val response = AccessGrpcServer(mockk(), backendMockk).getItem(request)
            assertThat(
                response,
                `is`(
                    GetItemResponse.newBuilder()
                        .addAllAccessRights(
                            listOf(
                                ItemProto.newBuilder()
                                    .setId(TEST_Metadata.id)
                                    .setAccessState(TEST_Metadata.access_state!!.toProto())
                                    .setBand(TEST_Metadata.band)
                                    .setDoi(TEST_Metadata.doi)
                                    .setHandle(TEST_Metadata.handle)
                                    .setIsbn(TEST_Metadata.isbn)
                                    .setIssn(TEST_Metadata.issn)
                                    .setPaketSigel(TEST_Metadata.paket_sigel)
                                    .setPpn(TEST_Metadata.ppn)
                                    .setPpnEbook(TEST_Metadata.ppn_ebook)
                                    .setPublicationType(TEST_Metadata.publicationType.toProto())
                                    .setPublicationYear(TEST_Metadata.publicationYear)
                                    .setRightsK10Plus(TEST_Metadata.rights_k10plus)
                                    .setSerialNumber(TEST_Metadata.serialNumber)
                                    .setTitle(TEST_Metadata.title)
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
    fun testGetItemError() {
        runBlocking {
            val request = GetItemRequest
                .newBuilder()
                .addAllIds(listOf("foo"))
                .build()

            val backendMockk = mockk<AccessServerBackend> {
                every { getAccessRightEntries(any()) } throws SQLException()
            }

            AccessGrpcServer(mockk(), backendMockk).getItem(request)
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
            title_journal = null,
            title_series = null,
            zbd_id = null,
        )
    }

}
