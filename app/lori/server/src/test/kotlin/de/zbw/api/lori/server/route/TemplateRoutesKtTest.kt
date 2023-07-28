package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.route.BookmarkRoutesKtTest.Companion.TEST_BOOKMARK
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.TEST_RIGHT
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.lori.model.BookmarkIdsRest
import de.zbw.lori.model.BookmarkRest
import de.zbw.lori.model.BookmarkTemplateRest
import de.zbw.lori.model.TemplateApplicationRest
import de.zbw.lori.model.TemplateApplicationsRest
import de.zbw.lori.model.TemplateIdsRest
import de.zbw.lori.model.TemplateRest
import de.zbw.persistence.lori.server.TemplateRightIdCreated
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.postgresql.util.PSQLException
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
    fun testPostTemplateCreated() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertTemplate(any()) } returns TemplateRightIdCreated(1, "1")
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/template") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_TEMPLATE))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testPostTemplateConflict() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertTemplate(any()) } throws mockk<PSQLException> {
                every { sqlState } returns ApiError.PSQL_CONFLICT_ERR_CODE
                every { message } returns "error"
            }
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/template") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_TEMPLATE))
            }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testPostTemplateInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertTemplate(any()) } throws SQLException()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/template") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_TEMPLATE))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testDeleteByTemplateIdNotFound() {
        // given
        val templateId = 4
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteTemplate(templateId) } returns 0
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/template/$templateId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testDeleteByTemplateIdInternalError() {
        // given
        val templateId = 4
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteTemplate(templateId) } throws SQLException()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/template/$templateId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPutTemplateNoContent() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateTemplate(TEST_TEMPLATE.templateId!!, any()) } returns 1
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/template") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_TEMPLATE))
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
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/template") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_TEMPLATE.copy(templateId = null)))
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPutTemplateInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateTemplate(TEST_TEMPLATE.templateId!!, any()) } throws SQLException()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/template") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_TEMPLATE))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetTemplateByIdOK() {
        // given
        val templateId = 45
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getTemplateById(templateId) } returns TEST_TEMPLATE.toBusiness()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/$templateId")
            val content: String = response.bodyAsText()
            val templateListType: Type = object : TypeToken<TemplateRest>() {}.type
            val received: TemplateRest = ItemRoutesKtTest.GSON.fromJson(content, templateListType)
            assertThat(received, `is`(TEST_TEMPLATE))
        }
    }

    @Test
    fun testGetTemplateByIdNotFound() {
        // given
        val templateId = 45
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getTemplateById(templateId) } returns null
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/$templateId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testGetTemplateByIdInternalError() {
        // given
        val templateId = 45
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getTemplateById(templateId) } throws SQLException()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/$templateId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetTemplateListOK() {
        // given
        val limit = 50
        val offset = 0
        val expected = listOf(TEST_TEMPLATE)
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getTemplateList(limit, offset) } returns listOf(TEST_TEMPLATE.toBusiness())
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/list?limit=$limit&offset=$offset")
            val content: String = response.bodyAsText()
            val templateListType: Type = object : TypeToken<ArrayList<TemplateRest>>() {}.type
            val received: ArrayList<TemplateRest> = ItemRoutesKtTest.GSON.fromJson(content, templateListType)
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
            application(
                servicePool.application()
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
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getTemplateList(limit, offset) } throws SQLException()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/list?limit=$limit&offset=$offset")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetBookmarksByTemplateIdOK() {
        val givenTemplateId = 5
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getBookmarksByTemplateId(5) } returns listOf(TEST_BOOKMARK)
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/$givenTemplateId/bookmarks")
            val content: String = response.bodyAsText()
            val bookmarkListType: Type = object : TypeToken<ArrayList<BookmarkRest>>() {}.type
            val received: ArrayList<BookmarkRest> = ItemRoutesKtTest.GSON.fromJson(content, bookmarkListType)
            assertThat(received, `is`(listOf(TEST_BOOKMARK.toRest())))
        }
    }

    @Test
    fun testGetBookmarksByTemplateIdInternalError() {
        val givenTemplateId = 5
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getBookmarksByTemplateId(5) } throws SQLException()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/template/$givenTemplateId/bookmarks")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostBookmarksByTemplateIdCreated() {
        val givenTemplateId = 5
        val givenBookmarkId = TEST_BOOKMARK.bookmarkId
        val givenBookmarkTemplate = BookmarkTemplate(bookmarkId = givenBookmarkId, templateId = givenTemplateId)
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                upsertBookmarkTemplatePairs(
                    listOf(givenBookmarkTemplate)
                )
            } returns listOf(givenBookmarkTemplate)
            every { deleteBookmarkTemplatePairsByTemplateId(givenTemplateId) } returns 0
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )

            // Case w/ deleteOld=true
            val response = client.post("/api/v1/template/$givenTemplateId/bookmarks?deleteOld=true") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ItemRoutesKtTest.jsonAsString(
                        BookmarkIdsRest(
                            bookmarkIds = listOf(givenBookmarkId)
                        )
                    )
                )
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
            val content: String = response.bodyAsText()
            val pairsCreated: Type = object : TypeToken<Array<BookmarkTemplateRest>>() {}.type
            val received: Array<BookmarkTemplateRest> = ItemRoutesKtTest.GSON.fromJson(content, pairsCreated)
            assertThat(received.toList(), `is`(listOf(givenBookmarkTemplate.toRest())))
            verify(exactly = 1) { backend.deleteBookmarkTemplatePairsByTemplateId(givenTemplateId) }
        }

        testApplication {
            application(
                servicePool.application()
            )

            // Case w/ deleteOld=true
            val response = client.post("/api/v1/template/$givenTemplateId/bookmarks") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ItemRoutesKtTest.jsonAsString(
                        BookmarkIdsRest(
                            bookmarkIds = listOf(givenBookmarkId)
                        )
                    )
                )
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
            val content: String = response.bodyAsText()
            val pairsCreated: Type = object : TypeToken<Array<BookmarkTemplateRest>>() {}.type
            val received: Array<BookmarkTemplateRest> = ItemRoutesKtTest.GSON.fromJson(content, pairsCreated)
            assertThat(received.toList(), `is`(listOf(givenBookmarkTemplate.toRest())))
            verify(exactly = 1) { backend.deleteBookmarkTemplatePairsByTemplateId(givenTemplateId) }
        }
    }

    @Test
    fun testPostApplications() {
        val givenTemplateId = 11
        val givenTemplateId2 = 12
        val expectedMetadataIds = listOf("metadataId1", "metadataId2")
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { applyTemplates(listOf(givenTemplateId)) } returns mapOf(givenTemplateId to expectedMetadataIds)
            every { applyAllTemplates() } returns listOf(givenTemplateId to expectedMetadataIds, givenTemplateId2 to expectedMetadataIds).toMap()
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // Test OK Path
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/template/applications") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ItemRoutesKtTest.jsonAsString(
                        TemplateIdsRest(
                            templateIds = listOf(givenTemplateId)
                        )
                    )
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
                        templateApplication = listOf(
                            TemplateApplicationRest(
                                templateId = givenTemplateId,
                                metadataIds = expectedMetadataIds,
                                numberOfAppliedEntries = expectedMetadataIds.size
                            )
                        )
                    )
                )
            )
        }

        // Test OK when all templates are applied
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/template/applications?all=true") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ItemRoutesKtTest.jsonAsString(
                        TemplateIdsRest(
                            templateIds = listOf(givenTemplateId)
                        )
                    )
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
                        templateApplication = listOf(
                            TemplateApplicationRest(
                                templateId = givenTemplateId,
                                metadataIds = expectedMetadataIds,
                                numberOfAppliedEntries = expectedMetadataIds.size
                            ),
                            TemplateApplicationRest(
                                templateId = givenTemplateId2,
                                metadataIds = expectedMetadataIds,
                                numberOfAppliedEntries = expectedMetadataIds.size
                            )
                        )
                    )
                )
            )
        }

        // Test Bad Request Path
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/template/applications") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ItemRoutesKtTest.jsonAsString(
                        TemplateIdsRest(
                            templateIds = null,
                        )
                    )
                )
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }

        // Internal Service Error Path
        val backend2 = mockk<LoriServerBackend>(relaxed = true) {
            every { applyTemplates(listOf(givenTemplateId)) } throws SQLException()
        }
        val servicePool2 = ItemRoutesKtTest.getServicePool(backend2)
        testApplication {
            application(
                servicePool2.application()
            )
            val response = client.post("/api/v1/template/applications") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ItemRoutesKtTest.jsonAsString(
                        TemplateIdsRest(
                            templateIds = listOf(givenTemplateId)
                        )
                    )
                )
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    companion object {
        val TEST_TEMPLATE = TemplateRest(
            templateName = "name",
            description = "some description",
            templateId = 2,
            right = TEST_RIGHT,
        )
    }
}
