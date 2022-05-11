package de.zbw.api.lori.server.route

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.ItemCountByRight
import de.zbw.lori.model.ItemEntry
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
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

class ItemRoutesKtTest {

    @Test
    fun testItemPostCreated() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsEntry(givenMetadataId, givenRightId) } returns false
            every { insertItemEntry(givenMetadataId, givenRightId) } returns "foo"
        }
        val servicePool = getServicePool(backend)

        // when + then
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM_ENTRY))
                }
            ) {
                assertThat("Should return Created", response.status(), `is`(HttpStatusCode.Created))
            }
        }
    }

    @Test
    fun testItemPostConflict() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsEntry(givenMetadataId, givenRightId) } returns true
        }
        val servicePool = getServicePool(backend)

        // when + then
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM_ENTRY))
                }
            ) {
                assertThat("Should return Conflict", response.status(), `is`(HttpStatusCode.Conflict))
            }
        }
    }

    @Test
    fun testItemBadRequest() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsEntry(givenMetadataId, givenRightId) } returns true
        }
        val servicePool = getServicePool(backend)

        // when + then
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM))
                }
            ) {
                assertThat("Should return BadRequest", response.status(), `is`(HttpStatusCode.BadRequest))
            }
        }
    }

    @Test
    fun testItemPostInternal() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsEntry(givenMetadataId, givenRightId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        // when + then
        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Post, "/api/v1/item") {
                    addHeader(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM_ENTRY))
                }
            ) {
                assertThat(
                    "Should return InternalServerError",
                    response.status(),
                    `is`(HttpStatusCode.InternalServerError)
                )
            }
        }
    }

    @Test
    fun testItemEntryDeleteOKLastRelation() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntry(givenMetadataId, givenRightId) } returns 1
            every { countItemByRightId(givenRightId) } returns 0
            every { deleteRight(givenRightId) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/$givenMetadataId/$givenRightId")
            ) {
                assertThat("Should return OK", response.status(), `is`(HttpStatusCode.OK))
                verify(exactly = 1) { backend.deleteRight(rightId = givenRightId) }
            }
        }
    }

    @Test
    fun testItemEntryDeleteOKRemainingRelations() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntry(givenMetadataId, givenRightId) } returns 1
            every { countItemByRightId(givenRightId) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/$givenMetadataId/$givenRightId")
            ) {
                assertThat("Should return OK", response.status(), `is`(HttpStatusCode.OK))
                verify(exactly = 0) { backend.deleteRight(rightId = givenRightId) }
            }
        }
    }

    @Test
    fun testItemEntryDeleteInternal() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntry(givenMetadataId, givenRightId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/$givenMetadataId/$givenRightId")
            ) {
                assertThat("Should return Internal Error", response.status(), `is`(HttpStatusCode.InternalServerError))
            }
        }
    }

    @Test
    fun testItemDeleteByMetadataOK() {
        // given
        val givenMetadataId = "meta"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntriesByMetadataId(givenMetadataId) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/metadata/$givenMetadataId")
            ) {
                assertThat("Should return OK", response.status(), `is`(HttpStatusCode.OK))
            }
        }
    }

    @Test
    fun testItemDeleteByMetadataInternal() {
        // given
        val givenMetadataId = "meta"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntriesByMetadataId(givenMetadataId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/metadata/$givenMetadataId")
            ) {
                assertThat("Should return Internal Error", response.status(), `is`(HttpStatusCode.InternalServerError))
            }
        }
    }

    @Test
    fun testItemDeleteByRightOK() {
        // given
        val givenRightId = "meta"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntriesByRightId(givenRightId) } returns 1
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/right/$givenRightId")
            ) {
                assertThat("Should return OK", response.status(), `is`(HttpStatusCode.OK))
            }
        }
    }

    @Test
    fun testItemDeleteByRightInternal() {
        // given
        val givenRightId = "meta"

        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { deleteItemEntriesByRightId(givenRightId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        withTestApplication(servicePool.application()) {
            with(
                handleRequest(HttpMethod.Delete, "/api/v1/item/right/$givenRightId")
            ) {
                assertThat("Should return Internal Error", response.status(), `is`(HttpStatusCode.InternalServerError))
            }
        }
    }

    @Test
    fun testGetList() {
        // given
        val offset = 2
        val limit = 5
        val expectedItem = ItemRest(
            metadata = ITEM_METADATA,
            rights = emptyList(),
        )
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getItemList(limit, offset) } returns listOf(expectedItem.toBusiness())
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
                assertThat(received.toList(), `is`(listOf(expectedItem)))
            }
        }
        verify(exactly = 1) { backend.getItemList(limit, offset) }
    }

    @Test
    fun testGetListDefault() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val expectedItem = ItemRest(
            metadata = ITEM_METADATA,
            rights = emptyList(),
        )
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                getItemList(
                    defaultLimit,
                    defaultOffset
                )
            } returns listOf(expectedItem.toBusiness())
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
                assertThat(received.toList(), `is`(listOf(expectedItem)))
            }
        }
        verify(exactly = 1) { backend.getItemList(defaultLimit, defaultOffset) }
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
    fun testCountItemByRightId() {
        // given
        val expectedAnswer = ItemCountByRight(
            rightId = "123",
            count = 5,
        )
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                countItemByRightId(expectedAnswer.rightId)
            } returns expectedAnswer.count
        }
        val servicePool = getServicePool(backend)
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/count/right/${expectedAnswer.rightId}")) {
                val content: String = response.content!!
                val groupListType: Type = object : TypeToken<ItemCountByRight>() {}.type
                val received: ItemCountByRight = Gson().fromJson(content, groupListType)
                assertThat(received, `is`(expectedAnswer))
            }
        }
    }

    @Test
    fun testCountItemByRightIdInternal() {
        // given
        val rightId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                countItemByRightId(rightId)
            } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/item/count/right/$rightId")) {
                assertThat("Should return Internal Error", response.status(), `is`(HttpStatusCode.InternalServerError))
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

        val ITEM_METADATA = MetadataRest(
            metadataId = "foo",
            band = "band",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            paketSigel = "sigel",
            ppn = "ppn",
            ppnEbook = "ppn ebook",
            publicationType = MetadataRest.PublicationType.book,
            publicationYear = 2000,
            rightsK10plus = "some rights",
            serialNumber = "12354566",
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zbdId = null,
        )

        val TEST_ITEM = ItemRest(
            metadata = ITEM_METADATA,
            rights = emptyList(),
        )

        val TEST_ITEM_ENTRY = ItemEntry(
            metadataId = "meta",
            rightId = "right",
        )

        fun jsonAsString(any: Any): String = Gson().toJson(any)
        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")

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
