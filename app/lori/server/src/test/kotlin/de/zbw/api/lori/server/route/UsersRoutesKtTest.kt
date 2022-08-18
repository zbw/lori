package de.zbw.api.lori.server.route

import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.UserRest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.sql.SQLException

class UsersRoutesKtTest {

    @Test
    fun testUsersRegisterOK() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { userContainsName(TEST_USER.name) } returns false
            every { insertNewUser(TEST_USER) } returns TEST_USER.name
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Accepted",
                response.status,
                `is`(HttpStatusCode.Created)
            )
        }
    }

    @Test
    fun testUsersRegisterConflict() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { userContainsName(TEST_USER.name) } returns true
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Conflict",
                response.status,
                `is`(HttpStatusCode.Conflict)
            )
        }
    }

    @Test
    fun testUsersRegisterBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(ItemRoutesKtTest.TEST_ITEM))
            }
            assertThat(
                "Should return BadRequest",
                response.status,
                `is`(HttpStatusCode.BadRequest)
            )
        }
    }

    @Test
    fun testUsersRegisterInternal() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { userContainsName(TEST_USER.name) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        testApplication {
            application(
                servicePool.application()
            )
            val response = client.post("/api/v1/users/register") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_USER))
            }
            assertThat(
                "Should return Internal Error",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    companion object {
        private val CONFIG = LoriConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
            digitalArchiveAddress = "https://archiveaddress",
            digitalArchiveCommunity = "5678",
            digitalArchiveUsername = "testuser",
            digitalArchivePassword = "password",
            digitalArchiveBasicAuth = "basicauth",
        )

        private val tracer: Tracer =
            OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.route.UsersRoutesKtTest")

        private val TEST_USER = UserRest(
            name = "Bob",
            password = "_secret_"
        )

        fun jsonAsString(any: Any): String = RightRoutesKtTest.GSON.toJson(any)
        fun getServicePool(backend: LoriServerBackend) = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend,
            tracer = tracer,
        )
    }
}