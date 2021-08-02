package de.zbw.api.access.server.route

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.access.model.AccessInformation
import de.zbw.access.model.Action
import de.zbw.access.model.Restriction
import de.zbw.api.access.server.ServicePoolWithProbes
import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.api.access.server.type.toBusiness
import de.zbw.business.access.server.AccessServerBackend
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

class ApiRoutingTest {

    @Test
    fun testAccessInformationPostCreated() {

        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { insertAccessRightEntry(any()) } returns "foo"
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/accessinformation") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ACCESS_INFORMATION_REST))
                }
            ) {
                assertThat("Should return Accecpted", response.status(), `is`(HttpStatusCode.Created))
                verify(exactly = 1) { backend.insertAccessRightEntry(ACCESS_INFORMATION_REST.toBusiness()) }
            }
        }
    }

    @Test
    fun testAccessInformationPostBadContentType() {

        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { insertAccessRightEntry(any()) } returns "foo"
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/accessinformation") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.contentType)
                    setBody(jsonAsString(ACCESS_INFORMATION_REST))
                }
            ) {
                assertThat(
                    "Should return 400 because of bad content type",
                    response.status(),
                    `is`(HttpStatusCode.BadRequest)
                )
            }
        }
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testAccessInformationPostInternalError() {
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { insertAccessRightEntry(any()) } throws SQLException("foo")
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/accessinformation") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        jsonAsString(
                            ACCESS_INFORMATION_REST
                        )
                    )
                }
            ) {
                assertThat(
                    "Should return 500 because of internal SQL exception",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
                verify(exactly = 1) { backend.insertAccessRightEntry(ACCESS_INFORMATION_REST.toBusiness()) }
            }
        }
    }

    @Test
    fun testAccessInformationPostBadJSON() {
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { insertAccessRightEntry(any()) } returns "foo"
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/accessinformation") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.contentType)
                    setBody(
                        jsonAsString(
                            RESTRICTION_REST
                        )
                    )
                }
            ) {
                assertThat(
                    "Should return 400 because of json content",
                    response.status(),
                    `is`(HttpStatusCode.BadRequest)
                )
            }
        }
    }

    @Test
    fun testAccessInformationGet() {
        // given
        val testId = "someId"
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { getAccessRightEntries(listOf(testId)) } returns listOf(ACCESS_INFORMATION_REST.toBusiness())
        }
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/accessinformation/$testId")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ArrayList<AccessInformation>>() {}.type
                val received: ArrayList<AccessInformation> = Gson().fromJson(content, groupListType)
                assertThat(received.toList(), `is`(listOf(ACCESS_INFORMATION_REST)))
            }
        }
    }

    @Test
    fun testAccessInformationGetMissingParameter() {
        // given
        val backend = mockk<AccessServerBackend>(relaxed = true)
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = backend
        )
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/accessinformation/")) {
                assertThat(
                    "Should return 404 because of missing get parameter",
                    response.status(),
                    `is`(HttpStatusCode.NotFound)
                )
            }
        }
    }

    companion object {
        val CONFIG = AccessConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
        )

        val RESTRICTION_REST =
            Restriction(
                restrictiontype = Restriction.Restrictiontype.date,
                attributetype = Restriction.Attributetype.fromdate,
                attributevalues = listOf("2022-01-01"),
            )
        val ACCESS_INFORMATION_REST = AccessInformation(
            id = "foo",
            tenant = "bla",
            usageGuide = "guide",
            template = null,
            mention = true,
            sharealike = false,
            commercialuse = true,
            copyright = true,
            actions = listOf(
                Action(
                    permission = true,
                    actiontype = Action.Actiontype.read,
                    restrictions = listOf(
                        RESTRICTION_REST
                    )
                )
            )
        )

        fun jsonAsString(any: Any): String = Gson().toJson(any)
    }
}
