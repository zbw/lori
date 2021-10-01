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
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

class AccessInformationApiKtTest {

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
                assertThat("Should return Accepted", response.status(), `is`(HttpStatusCode.Created))
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

    @Test
    fun testAccessInformationPostConflictId() {

        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { containsAccessRightId(ACCESS_INFORMATION_REST.id) } returns true
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
                assertThat(
                    "Should return 409 due to a conflict",
                    response.status(),
                    `is`(HttpStatusCode.Conflict)
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
    fun testPostAccessInformationBadJSON() {
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
    fun testGetAccessInformation() {
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
                val groupListType: Type = object : TypeToken<AccessInformation>() {}.type
                val received: AccessInformation = Gson().fromJson(content, groupListType)
                assertThat(received, `is`(ACCESS_INFORMATION_REST))
            }
        }
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetAccessInformationInternalError() {
        // given
        val testId = "someId"
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { getAccessRightEntries(listOf(testId)) } throws SQLException()
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
            handleRequest(HttpMethod.Get, "/api/v1/accessinformation/$testId")
            // exception
        }
    }

    @Test
    fun testGetList() {
        // given
        val offset = 2
        val limit = 5
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { getAccessRightList(limit, offset) } returns listOf(ACCESS_INFORMATION_REST.toBusiness())
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
            with(handleRequest(HttpMethod.Get, "/api/v1/accessinformation/list?limit=$limit&offset=$offset")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ArrayList<AccessInformation>>() {}.type
                val received: ArrayList<AccessInformation> = Gson().fromJson(content, groupListType)
                assertThat(received.toList(), `is`(listOf(ACCESS_INFORMATION_REST)))
            }
        }
        verify(exactly = 1) { backend.getAccessRightList(limit, offset) }
    }

    @Test
    fun testGetListDefault() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every {
                getAccessRightList(
                    defaultLimit,
                    defaultOffset
                )
            } returns listOf(ACCESS_INFORMATION_REST.toBusiness())
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
            with(handleRequest(HttpMethod.Get, "/api/v1/accessinformation/list")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ArrayList<AccessInformation>>() {}.type
                val received: ArrayList<AccessInformation> = Gson().fromJson(content, groupListType)
                assertThat(received.toList(), `is`(listOf(ACCESS_INFORMATION_REST)))
            }
        }
        verify(exactly = 1) { backend.getAccessRightList(defaultLimit, defaultOffset) }
    }

    @DataProvider(name = DATA_FOR_INVALID_LIST_PARAM)
    fun createInvalidListParams() = arrayOf(
        arrayOf(
            "201",
            "50",
            "Limit: Out of range",
        ),
        arrayOf(
            "0",
            "50",
            "Limit: Out of range",
        ),
        arrayOf(
            "100",
            "-1000",
            "Offset: Out of range",
        ),
        arrayOf(
            "foobar",
            "50",
            "Limit: Invalid value",
        ),
        arrayOf(
            "201",
            "foobar",
            "Offset: Invalid value",
        ),
    )

    @Test(dataProvider = DATA_FOR_INVALID_LIST_PARAM)
    fun testGetListInvalidParameter(
        limit: String,
        offset: String,
        msg: String,
    ) {
        // given
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = mockk()
        )
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/accessinformation/list?limit=$limit&offset=$offset")) {
                assertThat(msg, response.status(), `is`(HttpStatusCode.BadRequest))
            }
        }
    }

    @Test
    fun testGETAccessInformationMissingParameter() {
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

    @Test
    fun testDELETEAccessInformationHappyPath() {
        // given
        val givenDeleteId = "toBeDeleted"
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { containsAccessRightId(givenDeleteId) } returns true
            every { deleteAccessRightEntries(listOf(givenDeleteId)) } returns 1
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
            with(handleRequest(HttpMethod.Delete, "/api/v1/accessinformation/$givenDeleteId")) {
                assertThat(
                    "Should return 200 indicating a successful operation",
                    response.status(),
                    `is`(HttpStatusCode.OK)
                )
            }
        }
    }

    @Test
    fun testDELETEAccessInformationNotFound() {
        // given
        val givenDeleteId = "toBeDeleted"
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { containsAccessRightId(givenDeleteId) } returns false
            every { deleteAccessRightEntries(listOf(givenDeleteId)) } returns 1
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
            handleRequest(HttpMethod.Delete, "/api/v1/accessinformation/$givenDeleteId")
        }
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testDELETEAccessInformationInternalError() {
        // given
        val givenDeleteId = "toBeDeleted"
        val backend = mockk<AccessServerBackend>(relaxed = true) {
            every { containsAccessRightId(givenDeleteId) } returns true
            every { deleteAccessRightEntries(listOf(givenDeleteId)) } throws SQLException()
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
            with(handleRequest(HttpMethod.Delete, "/api/v1/accessinformation/$givenDeleteId")) {
                assertThat(
                    "Should return 500 indicating that an internal error has occurred",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
            }
        }
    }

    companion object {
        const val DATA_FOR_INVALID_LIST_PARAM = "DATA_FOR_INVALID_LIST_PARAM"

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
                attributevalues = listOf("2022-01-0sav1"),
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
