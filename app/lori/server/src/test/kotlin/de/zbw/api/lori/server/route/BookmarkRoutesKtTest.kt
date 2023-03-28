package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.GSON
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.getServicePool
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.jsonAsString
import de.zbw.api.lori.server.type.toRest
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.BookmarkRawRest
import de.zbw.lori.model.BookmarkRest
import de.zbw.persistence.lori.server.BookmarkDBTest.Companion.TEST_BOOKMARK
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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.postgresql.util.PSQLException
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

/**
 * Testing [BookmarkRoutes].
 *
 * Created on 03-16-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkRoutesKtTest {
    @Test
    fun testPostBookmarkCreated() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertBookmark(any()) } returns 5
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmark") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARK.toRest()))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testPostBookmarkConflict() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertBookmark(any()) } throws mockk<PSQLException> {
                every { sqlState } returns ApiError.PSQL_CONFLICT_ERR_CODE
                every { message } returns "error"
            }
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmark") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARK.toRest()))
            }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testPostBookmarkInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertBookmark(any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmark") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARK))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testDeleteByBookmarkIdOK() {
        // given
        val bookmarkId = 4
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteBookmark(bookmarkId) } returns 1
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/bookmark/$bookmarkId")
            assertThat(response.status, `is`(HttpStatusCode.OK))
        }
    }

    @Test
    fun testDeleteByBookmarkIdNotFound() {
        // given
        val bookmarkId = 4
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteBookmark(bookmarkId) } returns 0
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/bookmark/$bookmarkId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testDeleteByBookmarkIdInternalError() {
        // given
        val bookmarkId = 4
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteBookmark(bookmarkId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.delete("/api/v1/bookmark/$bookmarkId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPutBookmarkNoContent() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateBookmark(TEST_BOOKMARK.bookmarkId!!, any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/bookmark") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARK.toRest()))
            }
            assertThat("Should return 204", response.status, `is`(HttpStatusCode.NoContent))
        }
    }

    @Test
    fun testPutBookmarkBadRequest() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) { }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/bookmark") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARK.copy(bookmarkId = null)))
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPutBookmarkInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateBookmark(TEST_BOOKMARK.bookmarkId!!, any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/bookmark") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARK.toRest()))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetBookmarkByIdOK() {
        // given
        val bookmarkId = 45
        val expected = TEST_BOOKMARK.toRest()
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getBookmarkById(bookmarkId) } returns TEST_BOOKMARK
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/bookmark/$bookmarkId")
            val content: String = response.bodyAsText()
            val bookmarkListType: Type = object : TypeToken<BookmarkRest>() {}.type
            val received: BookmarkRest = GSON.fromJson(content, bookmarkListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetBookmarkByIdNotFound() {
        // given
        val bookmarkId = 45
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getBookmarkById(bookmarkId) } returns null
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/bookmark/$bookmarkId")
            assertThat(response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testGetBookmarkByIdInternalError() {
        // given
        val bookmarkId = 45
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getBookmarkById(bookmarkId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.get("/api/v1/bookmark/$bookmarkId")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPostBookmarkRawCreated() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertBookmark(any()) } returns 5
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmarkraw") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARKRAW))
            }
            assertThat("Should return 201", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testPostBookmarkRawConflict() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertBookmark(any()) } throws mockk<PSQLException> {
                every { sqlState } returns ApiError.PSQL_CONFLICT_ERR_CODE
                every { message } returns "error"
            }
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmarkraw") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARKRAW))
            }
            assertThat("Should return 409", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testPostBookmarkRawInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertBookmark(any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/bookmarkraw") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARKRAW))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testPutBookmarkRawNoContent() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateBookmark(TEST_BOOKMARKRAW.bookmarkId!!, any()) } returns 1
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/bookmarkraw") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARKRAW))
            }
            assertThat("Should return 204", response.status, `is`(HttpStatusCode.NoContent))
        }
    }

    @Test
    fun testPutBookmarkRawBadRequest() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) { }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/bookmarkraw") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARKRAW.copy(bookmarkId = null)))
            }
            assertThat("Should return 400", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testPutBookmarkRawInternalError() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { updateBookmark(TEST_BOOKMARKRAW.bookmarkId!!, any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.put("/api/v1/bookmarkraw") {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_BOOKMARKRAW))
            }
            assertThat("Should return 500", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    companion object {
        val TEST_BOOKMARKRAW: BookmarkRawRest = BookmarkRawRest(
            bookmarkId = 1,
            bookmarkName = "somename",
            description = "some description",
            searchTerm = "tit:sometitle",
            filterPublicationType = "somePublication"
        )
    }
}
