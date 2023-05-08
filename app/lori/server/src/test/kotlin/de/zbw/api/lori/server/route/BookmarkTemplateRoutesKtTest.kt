package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.BookmarkTemplateRest
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
                    bookmarkId = TEST_BOOOKMARK_TEMPLATE.bookmarkId,
                    templateId = TEST_BOOOKMARK_TEMPLATE.templateId,
                )
            } returns 1
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOOKMARK_TEMPLATE))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }

        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOOKMARK_TEMPLATE.copy(bookmarkId = 500)))
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
                    bookmarkId = TEST_BOOOKMARK_TEMPLATE.bookmarkId,
                    templateId = TEST_BOOOKMARK_TEMPLATE.templateId,
                )
            } throws mockk<PSQLException> {
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
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOOKMARK_TEMPLATE))
            }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }

        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmarktemplates") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(ItemRoutesKtTest.jsonAsString(TEST_BOOOKMARK_TEMPLATE.copy(bookmarkId = 500)))
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
                    bookmarkId = TEST_BOOOKMARK_TEMPLATE.bookmarkId,
                    templateId = TEST_BOOOKMARK_TEMPLATE.templateId,
                )
            } returns 1
        }
        val servicePool = ItemRoutesKtTest.getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response =
                client.delete("/api/v1/bookmarktemplates?templateid=${TEST_BOOOKMARK_TEMPLATE.templateId}&bookmarkid=${TEST_BOOOKMARK_TEMPLATE.bookmarkId}")
            assertThat("Should return 200", response.status, `is`(HttpStatusCode.OK))
        }

        testApplication {
            application(
                servicePool.application()
            )
            val response =
                client.delete("/api/v1/bookmarktemplates?bookmarkid=${TEST_BOOOKMARK_TEMPLATE.templateId}&templateid=${TEST_BOOOKMARK_TEMPLATE.bookmarkId}")
            assertThat("Should return 404", response.status, `is`(HttpStatusCode.NotFound))
        }

        testApplication {
            application(
                servicePool.application()
            )
            val response =
                client.delete("/api/v1/bookmarktemplates?bookmarkid=foobar&templateid=${TEST_BOOOKMARK_TEMPLATE.bookmarkId}")
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    companion object {
        val TEST_BOOOKMARK_TEMPLATE = BookmarkTemplateRest(
            bookmarkId = 10,
            templateId = 15,
        )
    }
}
