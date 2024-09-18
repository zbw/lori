package de.zbw.api.lori.server.route

import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.ServicePoolWithProbes
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.Either
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.lori.model.ItemCountByRight
import de.zbw.lori.model.ItemEntry
import de.zbw.lori.model.ItemInformation
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PublicationTypeRest
import de.zbw.lori.model.RightRest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.lang.reflect.Type
import java.sql.SQLException
import java.time.LocalDate

/**
 * Test [ItemRoutes].
 *
 * Created on 02-08-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ItemRoutesKtTest {
    @Test
    fun testItemPostCreated() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { itemContainsEntry(givenMetadataId, givenRightId) } returns false
                coEvery { insertItemEntry(givenMetadataId, givenRightId, any()) } returns Either.Right("foo")
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/item") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM_ENTRY))
                }
            assertThat("Should return Created", response.status, `is`(HttpStatusCode.Created))
        }
    }

    @Test
    fun testItemPostConflict() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { itemContainsEntry(givenMetadataId, givenRightId) } returns true
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/item") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM_ENTRY))
                }
            assertThat("Should return Conflict", response.status, `is`(HttpStatusCode.Conflict))
        }
    }

    @Test
    fun testItemBadRequest() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { itemContainsEntry(givenMetadataId, givenRightId) } returns true
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/item") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM))
                }
            assertThat("Should return BadRequest", response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testItemPostInternal() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { itemContainsEntry(givenMetadataId, givenRightId) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.post("/api/v1/item") {
                    header(HttpHeaders.Accept, ContentType.Text.Plain.contentType)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(jsonAsString(TEST_ITEM_ENTRY))
                }
            assertThat(
                "Should return InternalServerError",
                response.status,
                `is`(HttpStatusCode.InternalServerError),
            )
        }
    }

    @Test
    fun testItemEntryDeleteOKLastRelation() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntry(givenMetadataId, givenRightId) } returns 1
                coEvery { countItemByRightId(givenRightId) } returns 0
                coEvery { deleteRight(givenRightId) } returns 1
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/$givenMetadataId/$givenRightId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
            coVerify(exactly = 1) { backend.deleteRight(rightId = givenRightId) }
        }
    }

    @Test
    fun testItemEntryDeleteOKRemainingRelations() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntry(givenMetadataId, givenRightId) } returns 1
                coEvery { countItemByRightId(givenRightId) } returns 1
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/$givenMetadataId/$givenRightId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
            coVerify(exactly = 0) { backend.deleteRight(rightId = givenRightId) }
        }
    }

    @Test
    fun testItemEntryDeleteInternal() {
        // given
        val givenMetadataId = "meta"
        val givenRightId = "right"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntry(givenMetadataId, givenRightId) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/$givenMetadataId/$givenRightId")
            assertThat("Should return Internal Error", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testItemGetRightsByMetadataOK() {
        // given
        val givenMetadataId = "meta"
        val expected = listOf(TEST_RIGHT)
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getRightEntriesByMetadataId(givenMetadataId) } returns expected.map { it.toBusiness() }
                coEvery { metadataContainsId(givenMetadataId) } returns true
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/metadata/$givenMetadataId")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ArrayList<RightRest>>() {}.type
            val received: ArrayList<RightRest> = GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testItemGetRightsByMetadataNotFound() {
        // given
        val givenMetadataId = "meta"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { metadataContainsId(givenMetadataId) } returns false
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/metadata/$givenMetadataId")
            assertThat("Should return 404", response.status, `is`(HttpStatusCode.NotFound))
        }
    }

    @Test
    fun testItemGetRightsByMetadataInternal() {
        // given
        val givenMetadataId = "meta"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { metadataContainsId(givenMetadataId) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/metadata/$givenMetadataId")
            assertThat("Should return Internal Error", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testItemDeleteByMetadataOK() {
        // given
        val givenMetadataId = "meta"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntriesByMetadataId(givenMetadataId) } returns 1
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/metadata/$givenMetadataId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
        }
    }

    @Test
    fun testItemDeleteByMetadataInternal() {
        // given
        val givenMetadataId = "meta"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntriesByMetadataId(givenMetadataId) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/metadata/$givenMetadataId")
            assertThat("Should return Internal Error", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testItemDeleteByRightOK() {
        // given
        val givenRightId = "meta"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntriesByRightId(givenRightId) } returns 1
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/right/$givenRightId")
            assertThat("Should return OK", response.status, `is`(HttpStatusCode.OK))
        }
    }

    @Test
    fun testItemDeleteByRightInternal() {
        // given
        val givenRightId = "meta"

        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { deleteItemEntriesByRightId(givenRightId) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.delete("/api/v1/item/right/$givenRightId")
            assertThat("Should return Internal Error", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testGetList() {
        // given
        val offset = 2
        val limit = 5
        val pageSize = 25
        val expectedInformation =
            ItemInformation(
                totalPages = 5,
                itemArray =
                    listOf(
                        ItemRest(
                            metadata = ITEM_METADATA,
                            rights = emptyList(),
                        ),
                    ),
                numberOfResults = 101,
            )
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { getItemList(limit, offset) } returns
                    expectedInformation
                        .itemArray
                        .map { it.toBusiness() }
                coEvery { countMetadataEntries() } returns expectedInformation.numberOfResults
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/list?limit=$limit&offset=$offset&pageSize=$pageSize")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ItemInformation>() {}.type
            val received: ItemInformation = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expectedInformation))
        }
        coVerify(exactly = 1) { backend.getItemList(limit, offset) }
    }

    @Test
    fun testGetListDefault() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val expectedInformation =
            ItemInformation(
                totalPages = 0,
                itemArray =
                    listOf(
                        ItemRest(
                            metadata = ITEM_METADATA,
                            rights = emptyList(),
                        ),
                    ),
                numberOfResults = 0,
            )
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    getItemList(
                        defaultLimit,
                        defaultOffset,
                    )
                } returns
                    expectedInformation
                        .itemArray
                        .map { it.toBusiness() }
            }
        val servicePool =
            ServicePoolWithProbes(
                services =
                    listOf(
                        mockk {
                            every { isReady() } returns true
                            every { isHealthy() } returns true
                        },
                    ),
                config = CONFIG,
                backend = backend,
                tracer = tracer,
            )
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/list")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ItemInformation>() {}.type
            val received: ItemInformation = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expectedInformation))
        }
        coVerify(exactly = 1) { backend.getItemList(defaultLimit, defaultOffset) }
    }

    @DataProvider(name = DATA_FOR_INVALID_LIST_PARAM)
    fun createInvalidListParams() =
        arrayOf(
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
        val servicePool =
            ServicePoolWithProbes(
                services =
                    listOf(
                        mockk {
                            every { isReady() } returns true
                            every { isHealthy() } returns true
                        },
                    ),
                config = CONFIG,
                backend = mockk(),
                tracer = tracer,
                samlUtils = mockk(),
            )
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/list?limit=$limit&offset=$offset")
            assertThat(msg, response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testCountItemByRightId() {
        // given
        val expectedAnswer =
            ItemCountByRight(
                rightId = "123",
                count = 5,
            )
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    countItemByRightId(expectedAnswer.rightId)
                } returns expectedAnswer.count
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/count/right/${expectedAnswer.rightId}")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ItemCountByRight>() {}.type
            val received: ItemCountByRight = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expectedAnswer))
        }
    }

    @Test
    fun testCountItemByRightIdInternal() {
        // given
        val rightId = "123"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    countItemByRightId(rightId)
                } throws SQLException()
            }
        val servicePool = getServicePool(backend)
        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/count/right/$rightId")
            assertThat("Should return Internal Error", response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testMetadataGetSearchResult() {
        // given
        val searchTerm = "com:foobar"
        val offset = 2
        val limit = 5
        val pageSize = 25
        val filterPublicationDate = "2000-2022"
        val filterPublicationType = "ARTICLE,THESIS"
        val expectedInformation =
            ItemInformation(
                totalPages = 5,
                itemArray =
                    listOf(
                        ItemRest(
                            metadata = ITEM_METADATA,
                            rights = emptyList(),
                        ),
                    ),
                accessStateWithCount = emptyList(),
                numberOfResults = 101,
                paketSigelWithCount = emptyList(),
                publicationTypeWithCount = emptyList(),
                zdbIdWithCount = emptyList(),
                hasOpenContentLicence = false,
                hasLicenceContract = false,
                hasZbwUserAgreement = false,
                templateNameWithCount = emptyList(),
                isPartOfSeriesCount = emptyList(),
                searchBarEquivalent = "",
            )
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    searchQuery(
                        searchTerm,
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns (
                    SearchQueryResult(
                        numberOfResults = expectedInformation.numberOfResults,
                        results =
                            expectedInformation
                                .itemArray
                                .map { it.toBusiness() },
                        paketSigels = emptyMap(),
                        zdbIds = emptyMap(),
                        isPartOfSeries = emptyMap(),
                        publicationType = emptyMap(),
                        accessState = emptyMap(),
                        hasOpenContentLicence = false,
                        hasLicenceContract = false,
                        hasZbwUserAgreement = false,
                        templateNamesToOcc = emptyMap(),
                        searchBarEquivalent = "",
                    )
                )
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.get(
                    "/api/v1/item/search?searchTerm=$searchTerm&limit=$limit&offset=$offset" +
                        "&pageSize=$pageSize&filterPublicationDate=$filterPublicationDate&" +
                        "filterPublicationType=$filterPublicationType",
                )
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ItemInformation>() {}.type
            val received: ItemInformation = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expectedInformation))
            assertThat(response.status, `is`(HttpStatusCode.OK))
        }
    }

    @Test
    fun testItemGetSearchResultNoSearchTerm() {
        // given
        val defaultLimit = 25
        val defaultOffset = 0
        val expectedInformation =
            ItemInformation(
                totalPages = 1,
                itemArray =
                    listOf(
                        ItemRest(
                            metadata = ITEM_METADATA,
                            rights = emptyList(),
                        ),
                    ),
                accessStateWithCount = emptyList(),
                numberOfResults = 1,
                paketSigelWithCount = emptyList(),
                zdbIdWithCount = emptyList(),
                publicationTypeWithCount = emptyList(),
                hasOpenContentLicence = false,
                hasLicenceContract = false,
                hasZbwUserAgreement = false,
                templateNameWithCount = emptyList(),
                isPartOfSeriesCount = emptyList(),
                searchBarEquivalent = "",
            )
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery {
                    searchQuery(
                        any(),
                        defaultLimit,
                        defaultOffset,
                        emptyList(),
                        emptyList(),
                    )
                } returns (
                    SearchQueryResult(
                        results =
                            expectedInformation
                                .itemArray
                                .map { it.toBusiness() },
                        numberOfResults = 1,
                        paketSigels = emptyMap(),
                        publicationType = emptyMap(),
                        zdbIds = emptyMap(),
                        accessState = emptyMap(),
                        hasOpenContentLicence = false,
                        hasLicenceContract = false,
                        hasZbwUserAgreement = false,
                        templateNamesToOcc = emptyMap(),
                        isPartOfSeries = emptyMap(),
                        searchBarEquivalent = "",
                    )
                )
                coEvery { countMetadataEntries() } returns expectedInformation.numberOfResults
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/search")
            val content: String = response.bodyAsText()
            val groupListType: Type = object : TypeToken<ItemInformation>() {}.type
            val received: ItemInformation = RightRoutesKtTest.GSON.fromJson(content, groupListType)
            assertThat(received, `is`(expectedInformation))
            assertThat(response.status, `is`(HttpStatusCode.OK))
        }
    }

    @DataProvider(name = DATA_FOR_SEARCH_BAD_REQUEST)
    fun createrDataForSearchBadRequest() =
        arrayOf(
            arrayOf(
                "200",
                "10",
                "10",
            ),
            arrayOf(
                "99",
                "-10",
                "10",
            ),
            arrayOf(
                "99",
                "10",
                "-10",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_BAD_REQUEST)
    fun testItemGetSearchResultBadRequest(
        limit: String,
        offset: String,
        pageSize: String,
    ) {
        // given
        val backend = mockk<LoriServerBackend>(relaxed = true) { }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response =
                client.get("/api/v1/item/search?searchTerm=foobar&limit=$limit&offset=$offset&pageSize=$pageSize")
            assertThat(response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    @Test
    fun testItemGetSearchResultInternal() {
        // given
        val searchTerm = "com:foobar"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { searchQuery(searchTerm, any(), any()) } throws SQLException()
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/search?searchTerm=$searchTerm")
            assertThat(response.status, `is`(HttpStatusCode.InternalServerError))
        }
    }

    @Test
    fun testItemGetSearchResultParseError() {
        // given
        val searchTerm = "com:foobar"
        val backend =
            mockk<LoriServerBackend>(relaxed = true) {
                coEvery { searchQuery(searchTerm, any(), any()) } throws ParsingException("some parsing error")
            }
        val servicePool = getServicePool(backend)

        // when + then
        testApplication {
            moduleAuthForTests()
            application(
                servicePool.testApplication(),
            )
            val response = client.get("/api/v1/item/search?searchTerm=$searchTerm")
            assertThat(response.status, `is`(HttpStatusCode.BadRequest))
        }
    }

    companion object {
        const val DATA_FOR_INVALID_LIST_PARAM = "DATA_FOR_INVALID_LIST_PARAM"
        const val DATA_FOR_SEARCH_BAD_REQUEST = "DATA_FOR_SEARCH_BAD_REQUEST"

        val CONFIG =
            LoriConfiguration(
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
                stage = "dev",
                handleURL = "https://testdarch.zbw.eu/econis-archiv/handle/",
            )

        val ITEM_METADATA =
            MetadataRest(
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
                zdbIdJournal = null,
                zdbIdSeries = null,
            )

        val TEST_ITEM =
            ItemRest(
                metadata = ITEM_METADATA,
                rights = emptyList(),
            )

        val TEST_ITEM_ENTRY =
            ItemEntry(
                metadataId = "meta",
                rightId = "right",
            )

        val TEST_RIGHT = RightRoutesKtTest.TEST_RIGHT

        val GSON = RightRoutesKtTest.GSON

        fun jsonAsString(any: Any): String = RightRoutesKtTest.GSON.toJson(any)

        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")

        fun getServicePool(backend: LoriServerBackend) =
            ServicePoolWithProbes(
                services =
                    listOf(
                        mockk {
                            every { isReady() } returns true
                            every { isHealthy() } returns true
                        },
                    ),
                config = CONFIG,
                backend = backend,
                tracer = tracer,
                samlUtils = mockk(),
            )
    }
}
