package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.route.RightRoutesKtTest.Companion.TEST_RIGHT
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
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

    companion object {
        val TEST_TEMPLATE = TemplateRest(
            templateName = "name",
            description = "some description",
            templateId = 2,
            right = TEST_RIGHT,
        )
    }
}
