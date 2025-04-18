package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.route.ItemRoutesKtTest.Companion.getServicePool
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PublicationTypeRest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.util.InternalAPI
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException

@InternalAPI
class MetadataRoutesKtTest {
    @Test
    fun testGetMetadataOK() {
        // given
        val testId = "someId"
        val expected = TEST_METADATA
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getMetadataElementsByIds(listOf(testId)) } returns listOf(expected.toBusiness())
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/metadata?handle=$testId")
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
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getMetadataElementsByIds(listOf(testId)) } returns emptyList()
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/metadata?handle=$testId")
            assertThat(
                "Should return NotFound",
                response.status,
                `is`(HttpStatusCode.NotFound),
            )
        }
    }

    @Test
    fun testGetMetadataInternal() {
        // given
        val testId = "someId"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getMetadataElementsByIds(listOf(testId)) } throws SQLException()
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/metadata?handle=$testId")
            assertThat(
                "Should return InternalServerError",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    @Test
    fun testMetadataGetList() {
        // given
        val offset = 2
        val limit = 5
        val expectedMetadata = TEST_METADATA
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getMetadataList(limit, offset) } returns listOf(expectedMetadata.toBusiness())
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/metadata/list?limit=$limit&offset=$offset")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ArrayList<MetadataRest>>() {}.type
            val received: ArrayList<MetadataRest> = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received.toList(), `is`(listOf(expectedMetadata)))
        }
        coVerify(exactly = 1) { backend.getMetadataList(limit, offset) }
    }

    @Test
    fun testMetadataGetListDefault() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val expectedMetadata = TEST_METADATA
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getMetadataList(
                        defaultLimit,
                        defaultOffset,
                    )
                } returns listOf(expectedMetadata.toBusiness())
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/metadata/list")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ArrayList<MetadataRest>>() {}.type
            val received: ArrayList<MetadataRest> = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received.toList(), `is`(listOf(expectedMetadata)))
        }
        coVerify(exactly = 1) { backend.getMetadataList(defaultLimit, defaultOffset) }
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
                servicePool.testApplication(),
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
                servicePool.testApplication(),
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
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getMetadataList(limit, offset) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
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
        val TEST_METADATA =
            MetadataRest(
                author = "Colbjørnsen, Terje",
                band = "band",
                collectionName = "collectionName",
                communityName = "communityName",
                deleted = false,
                doi = listOf("doi:example.org"),
                handle = "hdl:example.handle.net",
                isbn = listOf("1234567890123"),
                issn = "123456",
                paketSigel = listOf("sigel"),
                ppn = "ppn",
                publicationType = PublicationTypeRest.book,
                publicationYear = 2022,
                storageDate = NOW.minusDays(3),
                title = "Important title",
                titleJournal = null,
                titleSeries = null,
                zdbIds = listOf("zdbIds"),
            )
    }
}
