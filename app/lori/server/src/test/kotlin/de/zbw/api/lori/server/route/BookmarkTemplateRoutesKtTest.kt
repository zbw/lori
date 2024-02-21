package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.BookmarkTemplateBatchRest
import de.zbw.lori.model.BookmarkTemplateRest
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.postgresql.util.PSQLException
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

/**
 * Testing [BookmarkTemplateRoutes].
 *
 * Created on 05-08-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkTemplateRoutesKtTest {
    @Test
    fun testPostBookmarkTemplateCreated() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                insertBookmarkTemplatePair(
                    bookmarkId = TEST_BOOKMARK_TEMPLATE.bookmarkId,
                    templateId = TEST_BOOKMARK_TEMPLATE.templateId,
                )
            } returns 1
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE.copy(bookmarkId = 500)))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostBookmarkTemplateConflict() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                insertBookmarkTemplatePair(
                    bookmarkId = TEST_BOOKMARK_TEMPLATE.bookmarkId,
                    templateId = TEST_BOOKMARK_TEMPLATE.templateId,
                )
            } throws mockk<PSQLException> {
                every { sqlState } returns ApiError.PSQL_CONFLICT_ERR_CODE
                every { message } returns "error"
            }
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE))
            }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE.copy(bookmarkId = 500)))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostBookmarkTemplateDeleted() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                deleteBookmarkTemplatePair(
                    bookmarkId = TEST_BOOKMARK_TEMPLATE.bookmarkId,
                    templateId = TEST_BOOKMARK_TEMPLATE.templateId,
                )
            } returns 1
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response =
                client.delete("/api/v1/bookmarktemplates?templateid=${TEST_BOOKMARK_TEMPLATE.templateId}&bookmarkid=${TEST_BOOKMARK_TEMPLATE.bookmarkId}")
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
        }

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response =
                client.delete("/api/v1/bookmarktemplates?bookmarkid=${TEST_BOOKMARK_TEMPLATE.templateId}&templateid=${TEST_BOOKMARK_TEMPLATE.bookmarkId}")
            assertThat("Should return 404", response.status, `is`(HttpStatusCode.NotFound))
        }

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response =
                client.delete("/api/v1/bookmarktemplates?bookmarkid=foobar&templateid=${TEST_BOOKMARK_TEMPLATE.bookmarkId}")
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testBatchDelete() {
        // given
        var backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                deleteBookmarkTemplatePairs(TEST_BOOKMARK_TEMPLATE_BATCH.batch!!.map { it.toBusiness() })
            } returns 1
        }
        var servicePool = ItemRoutesKtTest.getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.delete("/api/v1/bookmarktemplates/batch") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE_BATCH))
            }
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
            val content: String = response.bodyAsText()
            val itemsDeleted: Type = object : TypeToken<Int>() {}.type
            val received: Int = ItemRoutesKtTest.GSON.fromJson(content, itemsDeleted)
            assertThat(received, `is`(1))
        }

        backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                deleteBookmarkTemplatePairs(TEST_BOOKMARK_TEMPLATE_BATCH.batch!!.map { it.toBusiness() })
            } throws SQLException()
        }
        servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.delete("/api/v1/bookmarktemplates/batch") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE_BATCH))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testBatchPost() {
        // given
        var backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                upsertBookmarkTemplatePairs(TEST_BOOKMARK_TEMPLATE_BATCH.batch!!.map { it.toBusiness() })
            } returns listOf(TEST_BOOKMARK_TEMPLATE.toBusiness())
        }
        var servicePool = ItemRoutesKtTest.getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/bookmarktemplates/batch") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE_BATCH))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
            val content: String = response.bodyAsText()
            val pairsCreated: Type = object : TypeToken<Array<BookmarkTemplateRest>>() {}.type
            val received: Array<BookmarkTemplateRest> = ItemRoutesKtTest.GSON.fromJson(content, pairsCreated)
            assertThat(
                received.toList(),
                `is`(
                    listOf(
                        BookmarkTemplateRest(
                            bookmarkId = TEST_BOOKMARK_TEMPLATE.bookmarkId,
                            templateId = TEST_BOOKMARK_TEMPLATE.templateId
                        )
                    )
                )
            )
        }

        backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                upsertBookmarkTemplatePairs(TEST_BOOKMARK_TEMPLATE_BATCH.batch!!.map { it.toBusiness() })
            } throws SQLException()
        }
        servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/bookmarktemplates/batch") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOKMARK_TEMPLATE_BATCH))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    companion object {
        val TEST_BOOKMARK_TEMPLATE = BookmarkTemplateRest(
            bookmarkId = 10,
            templateId = 15,
        )

        val TEST_BOOKMARK_TEMPLATE_BATCH = BookmarkTemplateBatchRest(
            listOf(
                TEST_BOOKMARK_TEMPLATE,
                TEST_BOOKMARK_TEMPLATE,
            )
        )
    }
}
