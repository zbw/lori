package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.route.BookmarkRoutesKtTest.Companion.TEST_BOOKMARK
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.CONFIG
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.TEST_RIGHT
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.tracer
import de.zbw.api.lori.server.type.SamlUtils
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.lori.model.BookmarkIdsRest
import de.zbw.lori.model.BookmarkRest
import de.zbw.lori.model.BookmarkTemplateRest
import de.zbw.lori.model.ExceptionsForTemplateRest
import de.zbw.lori.model.RightIdsRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.TemplateApplicationRest
import de.zbw.lori.model.TemplateApplicationsRest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.postgresql.util.PSQLException
import org.postgresql.util.ServerErrorMessage
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

/**
 * Testing [TemplateRoutes].
 *
 * Created on 04-20-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class TemplateRoutesKtTest {
    @Test
    fun testDeleteTemplateOK() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteRight(rightId) } returns 1
                coEvery { deleteItemEntriesByRightId(rightId) } returns 5
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/template/$rightId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
            coVerify(exactly = 1) { backend.deleteRight(rightId) }
            coVerify(exactly = 1) { backend.deleteItemEntriesByRightId(rightId) }
        }
    }

    @Test
    fun testDeleteTemplateNotFound() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteRight(rightId) } returns 0
                coEvery { deleteItemEntriesByRightId(rightId) } returns 0
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/template/$rightId")
            assertThat("Should return 404", response.status, `is`(HttpStatusCode.NotFound))
            coVerify(exactly = 1) { backend.deleteRight(rightId) }
            coVerify(exactly = 1) { backend.deleteItemEntriesByRightId(rightId) }
        }
    }

    @Test
    fun testDeleteTemplateException() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteRight(rightId) } throws SQLException("foo")
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/template/$rightId")
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostTemplateCreated() {
        // given
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { insertTemplate(any()) } returns "1"
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(ItemRoutesKtTest.jsonAsString(TEST_RIGHT))
                }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testPostTemplateConflict() {
        // given
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { insertTemplate(any()) } throws
                    mockk<PSQLException> {
                        every { sqlState } returns ApiError.PSQL_CONFLICT_ERR_CODE
                        every { message } returns "error"
                    }
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(ItemRoutesKtTest.jsonAsString(TEST_RIGHT))
                }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testPostTemplateInternalError() {
        // given
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { insertTemplate(any()) } throws SQLException()
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(ItemRoutesKtTest.jsonAsString(TEST_RIGHT))
                }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPutTemplateNoContent() {
        // given
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { upsertRight(any()) } returns 1
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/template") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(ItemRoutesKtTest.jsonAsString(TEST_RIGHT.copy(isTemplate = true, templateName = "name")))
                }
            assertThat("Should return 204", response.status, `is`(HttpStatusCode.NoContent))
        }
    }

    @Test
    fun testPutTemplateBadRequest() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) { }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/template") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            TEST_RIGHT.copy(isTemplate = false),
                        ),
                    )
                }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPutTemplateInternalError() {
        // given
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { upsertRight(any()) } throws SQLException()
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.put("/api/v1/template") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(ItemRoutesKtTest.jsonAsString(TEST_RIGHT.copy(isTemplate = true, templateName = "name")))
                }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetTemplateByIdOK() {
        // given
        val rightId = "45"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getRightById(rightId) } returns TEST_RIGHT.toBusiness()
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/$rightId")
            val content: String = response.bodyAsText()
            val templateListType: Type = object : TypeToken<RightRest>() {}.type
            val received: RightRest = ItemRoutesKtTest.GSON.fromJson(content, templateListType)
            assertThat(received, `is`(TEST_RIGHT))
        }
    }

    @Test
    fun testGetTemplateByRightIdNotFound() {
        // given
        val rightId = "45"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getRightById(rightId) } returns null
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/$rightId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testGetTemplateByIdInternalError() {
        // given
        val rightId = "45"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getRightById(rightId) } throws SQLException()
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/$rightId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetTemplateListOK() {
        // given
        val limit = 50
        val offset = 0
        val expected = listOf(TEST_RIGHT)
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getTemplateList(limit, offset) } returns listOf(TEST_RIGHT.toBusiness())
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/list?limit=$limit&offset=$offset")
            val content: String = response.bodyAsText()
            val templateListType: Type = object : TypeToken<ArrayList<RightRest>>() {}.type
            val received: ArrayList<RightRest> = ItemRoutesKtTest.GSON.fromJson(content, templateListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetTemplateListBadRequest() {
        // given
        val limit = 0
        val offset = 0
        val backend = mockk<LoriServerBackend>(relaxed = true) { }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/list?limit=$limit&offset=$offset")
            assertThat(response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testGetTemplateListInternalError() {
        // given
        val limit = 5
        val offset = 0
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getTemplateList(limit, offset) } throws SQLException()
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/list?limit=$limit&offset=$offset")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetBookmarksByTemplateIdOK() {
        val givenRightId = "5"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getBookmarksByRightId(givenRightId) } returns listOf(TEST_BOOKMARK)
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/$givenRightId/bookmarks")
            val content: String = response.bodyAsText()
            val bookmarkListType: Type = object : TypeToken<ArrayList<BookmarkRest>>() {}.type
            val received: ArrayList<BookmarkRest> = ItemRoutesKtTest.GSON.fromJson(content, bookmarkListType)
            assertThat(received, `is`(listOf(TEST_BOOKMARK.toRest())))
        }
    }

    @Test
    fun testGetBookmarksByRightIdInternalError() {
        val givenRightId = "5"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getBookmarksByRightId(givenRightId) } throws SQLException()
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/$givenRightId/bookmarks")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostBookmarksByTemplateIdCreated() {
        val givenRightId = "5"
        val givenBookmarkId = TEST_BOOKMARK.bookmarkId
        val givenBookmarkTemplate = BookmarkTemplate(bookmarkId = givenBookmarkId, rightId = givenRightId)
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    upsertBookmarkTemplatePairs(
                        listOf(givenBookmarkTemplate),
                    )
                } returns listOf(givenBookmarkTemplate)
                coEvery { deleteBookmarkTemplatePairsByRightId(givenRightId) } returns 0
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )

            // Case w/ deleteOld=true
            val response =
                client.post("/api/v1/template/$givenRightId/bookmarks?deleteOld=true") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            BookmarkIdsRest(
                                bookmarkIds = listOf(givenBookmarkId),
                            ),
                        ),
                    )
                }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
            val content: String = response.bodyAsText()
            val pairsCreated: Type = object : TypeToken<Array<BookmarkTemplateRest>>() {}.type
            val received: Array<BookmarkTemplateRest> = ItemRoutesKtTest.GSON.fromJson(content, pairsCreated)
            assertThat(received.toList(), `is`(listOf(givenBookmarkTemplate.toRest())))
            coVerify(exactly = 1) { backend.deleteBookmarkTemplatePairsByRightId(givenRightId) }
        }

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )

            // Case w/ deleteOld=true
            val response =
                client.post("/api/v1/template/$givenRightId/bookmarks") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            BookmarkIdsRest(
                                bookmarkIds = listOf(givenBookmarkId),
                            ),
                        ),
                    )
                }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
            val content: String = response.bodyAsText()
            val pairsCreated: Type = object : TypeToken<Array<BookmarkTemplateRest>>() {}.type
            val received: Array<BookmarkTemplateRest> = ItemRoutesKtTest.GSON.fromJson(content, pairsCreated)
            assertThat(received.toList(), `is`(listOf(givenBookmarkTemplate.toRest())))
            coVerify(exactly = 1) { backend.deleteBookmarkTemplatePairsByRightId(givenRightId) }
        }
    }

    @Test
    fun testPostApplications() {
        val givenRightId = "11"
        val givenRightId2 = "12"
        val expectedMetadataIds = listOf("metadataId1", "metadataId2")
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { applyTemplates(listOf(givenRightId)) } returns
                    listOf(
                        TemplateApplicationResult(
                            rightId = givenRightId,
                            errors = emptyList(),
                            appliedMetadataHandles = expectedMetadataIds,
                            templateName = "foobar",
                            exceptionTemplateApplicationResult = emptyList(),
                        ),
                    )
                coEvery { applyAllTemplates() } returns
                    listOf(
                        TemplateApplicationResult(
                            rightId = givenRightId,
                            errors = emptyList(),
                            appliedMetadataHandles = expectedMetadataIds,
                            templateName = "foobar",
                            exceptionTemplateApplicationResult = emptyList(),
                        ),
                        TemplateApplicationResult(
                            rightId = givenRightId2,
                            errors = emptyList(),
                            appliedMetadataHandles = expectedMetadataIds,
                            templateName = "baz",
                            exceptionTemplateApplicationResult = emptyList(),
                        ),
                    )
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // Test OK Path
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template/applications") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            RightIdsRest(
                                rightIds = listOf(givenRightId),
                            ),
                        ),
                    )
                }
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
            val content: String = response.bodyAsText()
            val receivedJSON: Type = object : TypeToken<TemplateApplicationsRest>() {}.type
            val received: TemplateApplicationsRest = ItemRoutesKtTest.GSON.fromJson(content, receivedJSON)
            assertThat(
                received,
                `is`(
                    TemplateApplicationsRest(
                        templateApplication =
                            listOf(
                                TemplateApplicationRest(
                                    rightId = givenRightId,
                                    handles = expectedMetadataIds,
                                    numberOfAppliedEntries = expectedMetadataIds.size,
                                    errors = emptyList(),
                                    exceptionTemplateApplications = emptyList(),
                                    templateName = "foobar",
                                ),
                            ),
                    ),
                ),
            )
        }

        // Test OK when all templates are applied
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template/applications?all=true") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            RightIdsRest(
                                rightIds = listOf(givenRightId),
                            ),
                        ),
                    )
                }
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
            val content: String = response.bodyAsText()
            val receivedJSON: Type = object : TypeToken<TemplateApplicationsRest>() {}.type
            val received: TemplateApplicationsRest = ItemRoutesKtTest.GSON.fromJson(content, receivedJSON)
            assertThat(
                received,
                `is`(
                    TemplateApplicationsRest(
                        templateApplication =
                            listOf(
                                TemplateApplicationRest(
                                    rightId = givenRightId,
                                    handles = expectedMetadataIds,
                                    numberOfAppliedEntries = expectedMetadataIds.size,
                                    errors = emptyList(),
                                    templateName = "foobar",
                                    exceptionTemplateApplications = emptyList(),
                                ),
                                TemplateApplicationRest(
                                    rightId = givenRightId2,
                                    handles = expectedMetadataIds,
                                    numberOfAppliedEntries = expectedMetadataIds.size,
                                    errors = emptyList(),
                                    templateName = "baz",
                                    exceptionTemplateApplications = emptyList(),
                                ),
                            ),
                    ),
                ),
            )
        }

        // Test Bad Request Path
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template/applications") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            RightIdsRest(
                                rightIds = null,
                            ),
                        ),
                    )
                }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }

        // Internal Service Error Path
        val backend2 =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { applyTemplates(listOf(givenRightId)) } throws SQLException()
            }
        val servicePool2 = ItemRoutesKtTest.getServicePool(backend2)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool2.testApplication(),
            )
            val response =
                client.post("/api/v1/template/applications") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            RightIdsRest(
                                rightIds = listOf(givenRightId),
                            ),
                        ),
                    )
                }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostTemplateExceptions() {
        val givenRightIdTemplate = "1"
        val givenRightIdException = listOf("12")

        // Test OK Path
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    addExceptionToTemplate(
                        rightIdTemplate = givenRightIdTemplate,
                        rightIdExceptions = givenRightIdException,
                    )
                } returns 1
                coEvery { isException(givenRightIdTemplate) } returns false
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/template/exceptions") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            ExceptionsForTemplateRest(
                                idOfTemplate = givenRightIdTemplate,
                                idsOfExceptions = givenRightIdException,
                            ),
                        ),
                    )
                }
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
        }

        // Test Bad Request Part 1: Given Template is an exception already
        val backend2 =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    addExceptionToTemplate(
                        rightIdTemplate = givenRightIdTemplate,
                        rightIdExceptions = givenRightIdException,
                    )
                } returns 1
                coEvery { isException(givenRightIdTemplate) } returns true
            }
        val servicePool2 = ItemRoutesKtTest.getServicePool(backend2)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool2.testApplication(),
            )
            val response =
                client.post("/api/v1/template/exceptions") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            ExceptionsForTemplateRest(
                                idOfTemplate = givenRightIdTemplate,
                                idsOfExceptions = givenRightIdException,
                            ),
                        ),
                    )
                }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }

        // Test Bad Request Part 2: Trying to add an exception to itself
        val backend3 =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    addExceptionToTemplate(
                        rightIdTemplate = givenRightIdTemplate,
                        rightIdExceptions = givenRightIdException + givenRightIdTemplate,
                    )
                } returns 1
                coEvery { isException(givenRightIdTemplate) } returns false
            }
        val servicePool3 = ItemRoutesKtTest.getServicePool(backend3)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool3.testApplication(),
            )
            val response =
                client.post("/api/v1/template/exceptions") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            ExceptionsForTemplateRest(
                                idOfTemplate = givenRightIdTemplate,
                                idsOfExceptions = givenRightIdException + givenRightIdTemplate,
                            ),
                        ),
                    )
                }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }

        // Test 500 Path
        val backend4 =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { isException(givenRightIdTemplate) } throws PSQLException(ServerErrorMessage("foo"))
            }
        val servicePool4 = ItemRoutesKtTest.getServicePool(backend4)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool4.testApplication(),
            )
            val response =
                client.post("/api/v1/template/exceptions") {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        ItemRoutesKtTest.jsonAsString(
                            ExceptionsForTemplateRest(
                                idOfTemplate = givenRightIdTemplate,
                                idsOfExceptions = givenRightIdException,
                            ),
                        ),
                    )
                }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetExceptionsByRightId() {
        val givenRightIdTemplate = "1"
        val expected = TEST_RIGHT.copy(isTemplate = true, exceptionFrom = "5")

        // Test OK Path
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getExceptionsByRightId(givenRightIdTemplate)
                } returns listOf(expected.toBusiness())
            }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/template/exceptions/$givenRightIdTemplate")
            val content: String = response.bodyAsText()
            val templateListType: Type = object : TypeToken<Array<RightRest>>() {}.type
            val received: Array<RightRest> = ItemRoutesKtTest.GSON.fromJson(content, templateListType)
            assertThat(received.toList(), `is`(listOf(expected)))
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
        }

        // Test internal error path
        val backend2 =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getExceptionsByRightId(givenRightIdTemplate)
                } throws PSQLException(ServerErrorMessage("foo"))
            }
        val servicePool2 = ItemRoutesKtTest.getServicePool(backend2)
        testApplication {
            moduleAuthForTests()
            application(
                servicePool2.testApplication(),
            )
            val response = client.get("/api/v1/template/exceptions/$givenRightIdTemplate")
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    companion object {
        fun getServicePool(
            backend: LoriServerBackend,
            samlUtils: SamlUtils = mockk(relaxed = true),
        ) = ServicePoolWithProbes(
            services =
                listOf(
                    mockk {
                        every { isReady() } returns true
                        every { isHealthy() } returns true
                    },
                ),
            config = CONFIG,
            backend = backend,
            tracer = tracer,
            samlUtils = samlUtils,
        )
    }
}
