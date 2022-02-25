package de.zbw.api.lori.server

import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.toProto
import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.Action
import de.zbw.business.lori.server.Attribute
import de.zbw.business.lori.server.Item
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.PublicationType
import de.zbw.business.lori.server.Restriction
import de.zbw.lori.api.ActionProto
import de.zbw.lori.api.ActionTypeProto
import de.zbw.lori.api.AddItemRequest
import de.zbw.lori.api.AddItemResponse
import de.zbw.lori.api.AttributeProto
import de.zbw.lori.api.AttributeTypeProto
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.GetItemRequest
import de.zbw.lori.api.GetItemResponse
import de.zbw.lori.api.ItemProto
import de.zbw.lori.api.RestrictionProto
import de.zbw.lori.api.RestrictionTypeProto
import io.grpc.StatusRuntimeException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.nio.channels.UnresolvedAddressException
import java.sql.SQLException

/**
 * Test [LoriGrpcServer].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriGrpcServerTest {

    @Test
    fun testFullImport() {
        runBlocking {
            // given
            val token = "SOME_TOKEN"

            val community = DACommunity(
                id = 5,
                name = "Some name",
                handle = null,
                type = null,
                link = "some link",
                expand = emptyList(),
                logo = null,
                parentCommunity = null,
                copyrightText = null,
                introductoryText = null,
                shortDescription = null,
                sidebarText = null,
                subcommunities = emptyList(),
                collections = listOf(
                    mockk {
                        every { id } returns 101
                    }
                ),
                countItems = 1,
            )

            val importer = mockk<DAConnector> {
                coEvery { login() } returns token
                coEvery { getCommunity(token) } returns community
                coEvery { startFullImport(token, listOf(101)) } returns listOf(3)
            }

            val expected = FullImportResponse.newBuilder()
                .setItemsImported(3)
                .build()

            val request = FullImportRequest.getDefaultInstance()
            // when
            val response = LoriGrpcServer(
                mockk(),
                mockk(),
                importer,
                mockk(),
            ).fullImport(request)

            // then
            assertThat(response, `is`(expected))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testFullImportLoginError() {
        runBlocking {
            // given
            val importer = mockk<DAConnector> {
                coEvery { login() } throws UnresolvedAddressException()
            }

            val request = FullImportRequest.getDefaultInstance()
            // when
            LoriGrpcServer(
                mockk(),
                mockk(),
                importer,
                mockk(),
            ).fullImport(request)
        }
    }

    @Test
    fun testAddItem() {
        runBlocking {
            val request = AddItemRequest
                .newBuilder()
                .build()

            val backendMockk = mockk<LoriServerBackend> {
                every { insertItem(any()) } returns "foo"
            }

            val response = LoriGrpcServer(
                mockk(),
                backendMockk,
                mockk(),
                mockk(),
            ).addItem(request)
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

            val backendMockk = mockk<LoriServerBackend> {
                every { insertItem(any()) } throws SQLException()
            }

            LoriGrpcServer(
                mockk(),
                backendMockk,
                mockk(),
                mockk(),
            ).addItem(request)
        }
    }

    @Test
    fun testGetItem() {
        runBlocking {
            val request = GetItemRequest
                .newBuilder()
                .addAllIds(listOf("foo"))
                .build()

            val backendMockk = mockk<LoriServerBackend> {
                every { getItems(any()) } returns listOf(
                    Item(
                        itemMetadata = TEST_Metadata,
                        actions = listOf(
                            Action(
                                type = de.zbw.business.lori.server.ActionType.READ,
                                permission = true,
                                restrictions = listOf(
                                    Restriction(
                                        type = de.zbw.business.lori.server.RestrictionType.DATE,
                                        attribute = Attribute(
                                            type = de.zbw.business.lori.server.AttributeType.FROM_DATE,
                                            values = listOf("2022-01-01"),
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }

            val response = LoriGrpcServer(
                mockk(),
                backendMockk,
                mockk(),
                tracer,
            ).getItem(request)
            assertThat(
                response,
                `is`(
                    GetItemResponse.newBuilder()
                        .addAllAccessRights(
                            listOf(
                                ItemProto.newBuilder()
                                    .setId(TEST_Metadata.id)
                                    .setAccessState(TEST_Metadata.accessState!!.toProto())
                                    .setBand(TEST_Metadata.band)
                                    .setDoi(TEST_Metadata.doi)
                                    .setHandle(TEST_Metadata.handle)
                                    .setIsbn(TEST_Metadata.isbn)
                                    .setIssn(TEST_Metadata.issn)
                                    .setPaketSigel(TEST_Metadata.paketSigel)
                                    .setPpn(TEST_Metadata.ppn)
                                    .setPpnEbook(TEST_Metadata.ppnEbook)
                                    .setPublicationType(TEST_Metadata.publicationType.toProto())
                                    .setPublicationYear(TEST_Metadata.publicationYear)
                                    .setRightsK10Plus(TEST_Metadata.rightsK10plus)
                                    .setSerialNumber(TEST_Metadata.serialNumber)
                                    .setTitle(TEST_Metadata.title)
                                    .setTitleJournal(TEST_Metadata.titleJournal)
                                    .setTitleSeries(TEST_Metadata.titleSeries)
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

            val backendMockk = mockk<LoriServerBackend> {
                every { getItems(any()) } throws SQLException()
            }

            LoriGrpcServer(
                mockk(),
                backendMockk,
                mockk(),
                tracer,
            ).getItem(request)
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
            publicationType = PublicationType.ARTICLE,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = "some journal",
            titleSeries = "some series",
            zbdId = null,
        )

        private val sdkTracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpGrpcSpanExporter
                        .builder()
                        .setEndpoint("http://localhost:4317")
                        .build()
                )
                    .build()
            )
            .build()

        private val openTelemetry: OpenTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal()

        val tracer: Tracer = openTelemetry.getTracer("de.zbw.api.lori.server.LoriServerTest")
    }
}
