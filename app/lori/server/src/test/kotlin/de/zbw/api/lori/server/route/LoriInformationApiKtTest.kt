package de.zbw.api.lori.server.route

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.ActionRest
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.RestrictionRest
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
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

class LoriInformationApiKtTest {

    @Test
    fun testItemPostCreated() {

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertItem(any()) } returns "foo"
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ITEM_REST))
                }
            ) {
                assertThat("Should return Accepted", response.status(), `is`(HttpStatusCode.Created))
                verify(exactly = 1) { backend.insertItem(ITEM_REST.toBusiness()) }
            }
        }
    }

    @Test
    fun testItemPostBadContentType() {

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertItem(any()) } returns "foo"
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.contentType)
                    setBody(jsonAsString(ITEM_REST))
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
    fun testItemPostConflictId() {

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { containsAccessRightId(ITEM_REST.id) } returns true
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ITEM_REST))
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

    @Test
    fun testItemPostInternalError() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertItem(any()) } throws SQLException("foo")
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(
                        jsonAsString(
                            ITEM_REST
                        )
                    )
                }
            ) {
                assertThat(
                    "Should return 500 because of internal SQL exception",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
                verify(exactly = 1) { backend.insertItem(ITEM_REST.toBusiness()) }
            }
        }
    }

    @Test
    fun testPostItemBadJSON() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertItem(any()) } returns "foo"
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
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
    fun testPutItemCreate() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { insertItem(any()) } returns "foo"
            every { containsAccessRightId(any()) } returns false
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ITEM_REST))
                }
            ) {
                assertThat("Should return CREATED", response.status(), `is`(HttpStatusCode.Created))
                verify(exactly = 1) { backend.insertItem(ITEM_REST.toBusiness()) }
            }
        }
    }

    @Test
    fun testPutUpdate() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { upsertItems(any()) } returns Unit
            every { containsAccessRightId(any()) } returns true
        }
        val servicePool = ServicePoolWithProbes(
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

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(ITEM_REST))
                }
            ) {
                assertThat("Should return NO_CONTENT", response.status(), `is`(HttpStatusCode.NoContent))
                verify(exactly = 1) { backend.upsertItems(listOf(ITEM_REST.toBusiness())) }
            }
        }
    }

    @Test
    fun testPutItemBadJSON() {
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            config = CONFIG,
            backend = mockk(),
            tracer = tracer,
        )

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Put, "/api/v1/item") {
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
    fun testGetItems() {
        // given
        val testId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getItems(listOf(testId)) } returns listOf(ITEM_REST.toBusiness())
        }
        val servicePool = ServicePoolWithProbes(
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
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/$testId")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ItemRest>() {}.type
                val received: ItemRest = Gson().fromJson(content, groupListType)
                assertThat(received, `is`(ITEM_REST))
            }
        }
    }

    @Test
    fun testGetItemsInternalError() {
        // given
        val testId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getItems(listOf(testId)) } throws SQLException()
        }
        val servicePool = ServicePoolWithProbes(
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
        // when + then
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Get, "/api/v1/item/$testId")
            ) {
                assertThat(
                    "Should return 500 because of internal SQL exception",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
            }
        }
    }

    @Test
    fun testGetList() {
        // given
        val offset = 2
        val limit = 5
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getAccessRightList(limit, offset) } returns listOf(ITEM_REST.toBusiness())
        }
        val servicePool = ServicePoolWithProbes(
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
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/list?limit=$limit&offset=$offset")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ArrayList<ItemRest>>() {}.type
                val received: ArrayList<ItemRest> = Gson().fromJson(content, groupListType)
                assertThat(received.toList(), `is`(listOf(ITEM_REST)))
            }
        }
        verify(exactly = 1) { backend.getAccessRightList(limit, offset) }
    }

    @Test
    fun testGetListDefault() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                getAccessRightList(
                    defaultLimit,
                    defaultOffset
                )
            } returns listOf(ITEM_REST.toBusiness())
        }
        val servicePool = ServicePoolWithProbes(
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
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/list")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ArrayList<ItemRest>>() {}.type
                val received: ArrayList<ItemRest> = Gson().fromJson(content, groupListType)
                assertThat(received.toList(), `is`(listOf(ITEM_REST)))
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
            backend = mockk(),
            tracer = tracer,
        )
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/list?limit=$limit&offset=$offset")) {
                assertThat(msg, response.status(), `is`(HttpStatusCode.BadRequest))
            }
        }
    }

    @Test
    fun testGETItemMissingParameter() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = ServicePoolWithProbes(
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
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/")) {
                assertThat(
                    "Should return 404 because of missing get parameter",
                    response.status(),
                    `is`(HttpStatusCode.NotFound)
                )
            }
        }
    }

    @Test
    fun testDELETEItemHappyPath() {
        // given
        val givenDeleteId = "toBeDeleted"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
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
            backend = backend,
            tracer = tracer,
        )

        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Delete, "/api/v1/item/$givenDeleteId")) {
                assertThat(
                    "Should return 200 indicating a successful operation",
                    response.status(),
                    `is`(HttpStatusCode.OK)
                )
            }
        }
    }

    @Test
    fun testDELETEItemNotFound() {
        // given
        val givenDeleteId = "toBeDeleted"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
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
            backend = backend,
            tracer = tracer,
        )

        // when + then
        withTestApplication(servicePool.application()) {
            handleRequest(HttpMethod.Delete, "/api/v1/item/$givenDeleteId")
        }
    }

    @Test
    fun testDELETEItemInternalError() {
        // given
        val givenDeleteId = "toBeDeleted"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
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
            backend = backend,
            tracer = tracer,
        )

        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Delete, "/api/v1/item/$givenDeleteId")) {
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

        val CONFIG = LoriConfiguration(
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

        val RESTRICTION_REST =
            RestrictionRest(
                restrictiontype = RestrictionRest.Restrictiontype.date,
                attributetype = RestrictionRest.Attributetype.fromdate,
                attributevalues = listOf("2022-01-0sav1"),
            )
        val ITEM_REST = ItemRest(
            id = "foo",
            accessState = ItemRest.AccessState.open,
            band = "band",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            paketSigel = "sigel",
            ppn = "ppn",
            ppnEbook = "ppn ebook",
            publicationType = ItemRest.PublicationType.book,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = null,
            actions = listOf(
                ActionRest(
                    permission = true,
                    actiontype = ActionRest.Actiontype.read,
                    restrictions = listOf(
                        RESTRICTION_REST
                    )
                )
            )
        )

        fun jsonAsString(any: Any): String = Gson().toJson(any)
        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")
    }
}
