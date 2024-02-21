package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PublicationTypeRest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
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
import io.ktor.util.InternalAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException
import java.time.LocalDate

@InternalAPI
class MetadataRoutesKtTest {

    @Test
    fun testMetadataPostCreated() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { metadataContainsId(TEST_METADATA.metadataId) } returns false
            every { insertMetadataElement(any()) } returns "foo"
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/metadata") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_METADATA))
            }
            assertThat(
                "Should return Accepted",
                response.status,
                `is`(HttpStatusCode.Created)
            )
        }
    }

    @Test
    fun testMetadataPostConflict() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { metadataContainsId(TEST_METADATA.metadataId) } returns true
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/metadata") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_METADATA))
            }
            assertThat(
                "Should return Conflict",
                response.status,
                `is`(HttpStatusCode.Conflict)
            )
        }
    }

    @Test
    fun testMetadataPostBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/metadata") {
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
    fun testMetadataPostInternal() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { metadataContainsId(TEST_METADATA.metadataId) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.post("/api/v1/metadata") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_METADATA))
            }
            assertThat(
                "Should return Internal Server Error",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    @Test
    fun testDeleteRightOK() {
        // given
        val metadataId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsMetadata(metadataId) } returns false
            every { deleteMetadata(metadataId) } returns 1
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.delete("/api/v1/metadata/$metadataId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
            verify(exactly = 1) { backend.itemContainsMetadata(metadataId) }
            verify(exactly = 1) { backend.deleteMetadata(metadataId) }
        }
    }

    @Test
    fun testDeleteRightConflict() {
        // given
        val metadataId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsMetadata(metadataId) } returns true
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.delete("/api/v1/metadata/$metadataId")
            assertThat("Should return Conflict", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testDeleteRightInternal() {
        // given
        val metadataId = "123"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { itemContainsMetadata(metadataId) } throws SQLException()
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

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.delete("/api/v1/metadata/$metadataId")
            assertThat(
                "Should return Internal Server Error",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    @Test
    fun testGetMetadataOK() {
        // given
        val testId = "someId"
        val expected = TEST_METADATA
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getMetadataElementsByIds(listOf(testId)) } returns listOf(expected.toBusiness())
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/$testId")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<MetadataRest>() {}.type
            val received: MetadataRest = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testGetMetadataNotFound() {
        // given
        val testId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getMetadataElementsByIds(listOf(testId)) } returns emptyList()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/$testId")
            assertThat(
                "Should return NotFound",
                response.status,
                `is`(HttpStatusCode.NotFound)
            )
        }
    }

    @Test
    fun testGetMetadataInternal() {
        // given
        val testId = "someId"
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getMetadataElementsByIds(listOf(testId)) } throws SQLException()
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/$testId")
            assertThat(
                "Should return InternalServerError",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    @Test
    fun testMetadataPutNoContent() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { metadataContainsId(TEST_METADATA.metadataId) } returns true
            every { upsertMetadataElements(any()) } returns IntArray(1) { 1 }
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.put("/api/v1/metadata") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_METADATA))
            }
            assertThat(
                "Should return NoContent",
                response.status,
                `is`(HttpStatusCode.NoContent)
            )
        }
    }

    @Test
    fun testMetadataPutCreated() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { metadataContainsId(TEST_METADATA.metadataId) } returns false
            every { insertMetadataElement(any()) } returns "foo"
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.put("/api/v1/metadata") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_METADATA))
            }
            assertThat(
                "Should return Created",
                response.status,
                `is`(HttpStatusCode.Created)
            )
        }
    }

    @Test
    fun testMetadataPutBadRequest() {
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.put("/api/v1/metadata") {
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
    fun testMetadataPutInternal() {
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { metadataContainsId(any()) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.put("/api/v1/metadata") {
                header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(jsonAsString(TEST_METADATA))
            }
            assertThat(
                "Should return InternalServerError",
                response.status,
                `is`(HttpStatusCode.InternalServerError)
            )
        }
    }

    @Test
    fun testMetadataGetList() {
        // given
        val offset = 2
        val limit = 5
        val expectedMetadata = TEST_METADATA
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getMetadataList(limit, offset) } returns listOf(expectedMetadata.toBusiness())
        }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/list?limit=$limit&offset=$offset")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ArrayList<MetadataRest>>() {}.type
            val received: ArrayList<MetadataRest> = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received.toList(), `is`(listOf(expectedMetadata)))
        }
        verify(exactly = 1) { backend.getMetadataList(limit, offset) }
    }

    @Test
    fun testMetadataGetListDefault() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val expectedMetadata = TEST_METADATA
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every {
                getMetadataList(
                    defaultLimit,
                    defaultOffset
                )
            } returns listOf(expectedMetadata.toBusiness())
        }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/list")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ArrayList<MetadataRest>>() {}.type
            val received: ArrayList<MetadataRest> = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received.toList(), `is`(listOf(expectedMetadata)))
        }
        verify(exactly = 1) { backend.getMetadataList(defaultLimit, defaultOffset) }
    }

    @Test
    fun testMetadataGetListBadRequestLimit() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/list?limit=0")
            assertThat(
                "Should return BadRequest",
                response.status,
                `is`(HttpStatusCode.BadRequest),
            )
        }
    }

    @Test
    fun testMetadataGetListBadRequestOffset() {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true)
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/list?offset=-1")
            assertThat(
                "Should return BadRequest",
                response.status,
                `is`(HttpStatusCode.BadRequest),
            )
        }
    }

    @Test
    fun testMetadataGetListInternal() {
        // given
        val offset = 2
        val limit = 5
        val backend = mockk<LoriServerBackend>(relaxed = true) {
            every { getMetadataList(limit, offset) } throws SQLException()
        }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication()
            )
            val response = client.get("/api/v1/metadata/list?limit=$limit&offset=$offset")
            assertThat(
                "Should return InternalServerError",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    companion object {
        val CONFIG = LoriConfiguration(
            grpcPort = 9092,
            httpPort = 8080,
            sqlUser = "postgres",
            sqlPassword = "postgres",
            sqlUrl = "jdbc:someurl",
            digitalArchiveAddress = "https://archiveaddress",
            digitalArchiveUsername = "testuser",
            digitalArchivePassword = "password",
            digitalArchiveBasicAuth = "basicauth",
            jwtAudience = "0.0.0.0:8080/ui",
            jwtIssuer = "0.0.0.0:8080",
            jwtRealm = "Lori ui",
            jwtSecret = "foobar",
            duoSenderEntityId = "someId",
            sessionSignKey = "8BADF00DDEADBEAFDEADBAADDEADBAAD",
            sessionEncryptKey = "CAFEBABEDEADBEAFDEADBAADDEFEC8ED",
        )

        val TEST_METADATA = MetadataRest(
            metadataId = "foo",
            author = "Colbjørnsen, Terje",
            band = "band",
            collectionName = "collectionName",
            communityName = "communityName",
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            paketSigel = "sigel",
            ppn = "ppn",
            publicationType = PublicationTypeRest.book,
            publicationDate = LocalDate.of(2022, 9, 26),
            rightsK10plus = "some rights",
            storageDate = NOW.minusDays(3),
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zdbId = null,
        )

        fun jsonAsString(any: Any): String = RightRoutesKtTest.GSON.toJson(any)
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
