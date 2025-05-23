package de.zbw.api.lori.server

import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.CheckForRightErrorsRequest
import de.zbw.lori.api.CheckForRightErrorsResponse
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.TemplateApplication
import io.grpc.StatusRuntimeException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.nio.channels.UnresolvedAddressException
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Test [LoriGrpcServer].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriGrpcServerTest {
    @Test
    fun testApplyTemplatesAll() {
        runBlocking {
            // given
            val expectedResult =
                listOf(
                    TemplateApplicationResult(
                        rightId = "1",
                        errors = emptyList(),
                        appliedMetadataHandles = listOf("2"),
                        templateName = "foobar",
                        exceptionTemplateApplicationResult = null,
                        testId = "123",
                        numberOfErrors = 0,
                    ),
                )
            val expectedResponse =
                ApplyTemplatesResponse
                    .newBuilder()
                    .addAllTemplateApplications(
                        listOf(
                            TemplateApplication
                                .newBuilder()
                                .setRightId("1")
                                .setTemplateName("foobar")
                                .setNumberAppliedEntries(1)
                                .setException(
                                    TemplateApplication.newBuilder().build(),
                                ).build(),
                        ),
                    ).build()

            val request =
                ApplyTemplatesRequest
                    .newBuilder()
                    .setAll(true)
                    .setSkipDraft(false)
                    .setDryRun(false)
                    .build()
            val backendMock =
                mockk<LoriServerBackend> {
                    coEvery {
                        applyAllTemplates(skipTemplateDrafts = false, dryRun = false, createdBy = any())
                    } returns expectedResult
                }
            // when
            val grpcServer =
                LoriGrpcServer(
                    config = mockk(),
                    backend = backendMock,
                    daConnector =
                        mockk {
                            every { backend } returns backendMock
                        },
                    tracer = tracer,
                )
            val response = grpcServer.applyTemplates(request)

            // then
            assertThat(response, `is`(expectedResponse))
        }
    }

    @Test
    fun testApplyTemplatesIds() {
        runBlocking {
            // given
            val expectedResult =
                listOf(
                    TemplateApplicationResult(
                        rightId = "1",
                        errors = emptyList(),
                        appliedMetadataHandles = listOf("2"),
                        templateName = "foobar",
                        exceptionTemplateApplicationResult = null,
                        testId = "123",
                        numberOfErrors = 0,
                    ),
                )
            val expectedResponse =
                ApplyTemplatesResponse
                    .newBuilder()
                    .addAllTemplateApplications(
                        listOf(
                            TemplateApplication
                                .newBuilder()
                                .setRightId("1")
                                .setTemplateName("foobar")
                                .setNumberAppliedEntries(1)
                                .setException(
                                    TemplateApplication.newBuilder().build(),
                                ).build(),
                        ),
                    ).build()

            val request =
                ApplyTemplatesRequest
                    .newBuilder()
                    .setAll(false)
                    .setSkipDraft(false)
                    .setDryRun(false)
                    .addAllRightIds(listOf("1"))
                    .build()
            val backendMock =
                mockk<LoriServerBackend> {
                    coEvery {
                        applyTemplates(
                            any(),
                            skipTemplateDrafts = false,
                            dryRun = false,
                            createdBy = any(),
                        )
                    } returns expectedResult
                }
            // when
            val grpcServer =
                LoriGrpcServer(
                    config = mockk(),
                    backend = backendMock,
                    daConnector =
                        mockk {
                            every { backend } returns backendMock
                        },
                    tracer = tracer,
                )
            val response = grpcServer.applyTemplates(request)

            // then
            assertThat(response, `is`(expectedResponse))
        }
    }

    @Test
    fun testFullImport() {
        runBlocking {
            // given
            val token = "SOME_TOKEN"
            val importsPerCommunity = 3
            val communityIds = listOf("4")
            val community =
                DACommunity(
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
                    collections =
                        listOf(
                            mockk {
                                every { id } returns 101
                            },
                        ),
                    countItems = 1,
                )

            val importer =
                mockk<DAConnector> {
                    coEvery { login() } returns token
                    coEvery { getCommunityById(token, any()) } returns community
                    coEvery { getAllCommunityIds(token) } returns listOf(community.id)
                    coEvery { importAllCollectionsOfCommunity(token, any()) } returns listOf(importsPerCommunity)
                }

            val expected =
                FullImportResponse
                    .newBuilder()
                    .setItemsImported(communityIds.size * importsPerCommunity)
                    .setItemsDeleted(5)
                    .build()

            val request = FullImportRequest.getDefaultInstance()
            // when
            val response =
                LoriGrpcServer(
                    mockk(),
                    mockk<LoriServerBackend> {
                        coEvery { updateMetadataAsDeleted(any()) } returns 5
                    },
                    importer,
                    tracer,
                ).fullImport(request)

            // then
            assertThat(response, `is`(expected))
        }
    }

    @Test(expectedExceptions = [StatusRuntimeException::class])
    fun testFullImportLoginError() {
        runBlocking {
            // given
            val importer =
                mockk<DAConnector> {
                    coEvery { login() } throws UnresolvedAddressException()
                }

            val request = FullImportRequest.getDefaultInstance()
            // when
            LoriGrpcServer(
                mockk(),
                mockk(),
                importer,
                tracer,
            ).fullImport(request)
        }
    }

    @Test
    fun testCheckForRightErrors() {
        runBlocking {
            // given
            val expectedResult =
                listOf(
                    de.zbw.business.lori.server.type.RightError(
                        errorId = 1,
                        message = "foobar",
                        handle = "handle",
                        createdOn = NOW,
                        conflictType = ConflictType.GAP,
                        conflictByContext = "sigel1234",
                        conflictByRightId = null,
                        conflictingWithRightId = null,
                        testId = null,
                        createdBy = "user1",
                    ),
                )
            val expectedResponse =
                CheckForRightErrorsResponse
                    .newBuilder()
                    .setNumberOfErrors(
                        1,
                    ).build()

            val request =
                CheckForRightErrorsRequest
                    .newBuilder()
                    .build()
            val backendMock =
                mockk<LoriServerBackend> {
                    coEvery {
                        checkForRightErrors(any())
                    } returns expectedResult
                }
            // when
            val grpcServer =
                LoriGrpcServer(
                    config = mockk(),
                    backend = backendMock,
                    daConnector =
                        mockk {
                            every { backend } returns backendMock
                        },
                    tracer = tracer,
                )
            val response = grpcServer.checkForRightErrors(request)

            // then
            assertThat(response, `is`(expectedResponse))
        }
    }

    companion object {
        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.LoriGrpcServerTest")
        private val NOW =
            OffsetDateTime.of(
                2022,
                3,
                2,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            )
    }
}
