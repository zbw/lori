package de.zbw.api.lori.server.connector

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.DABitstream
import de.zbw.api.lori.server.type.DAChecksum
import de.zbw.api.lori.server.type.DACollection
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.DAItem
import de.zbw.api.lori.server.type.DAMetadata
import de.zbw.api.lori.server.type.DAObject
import de.zbw.api.lori.server.type.DAResourcePolicy
import de.zbw.api.lori.server.type.RestConverterTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ItemMetadata
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

/**
 * Tests for [DAConnector].
 *
 * Created on 02-15-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DAConnectorTest {
    @Test
    fun testLogin() {
        runBlocking {
            // given
            val expectedToken = "token-foo"
            val mockEngine =
                MockEngine {
                    respond(
                        content = ByteReadChannel(expectedToken),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                }
            val daConnector =
                DAConnector(
                    config =
                        mockk {
                            every { digitalArchiveAddress } returns "http://primula-qs.zbw-nett.zbw-kiel.de/econis-archiv"
                            every { digitalArchiveUsername } returns "some-user"
                            every { digitalArchivePassword } returns "some-password"
                            every { digitalArchiveBasicAuth } returns "pw"
                        },
                    engine = mockEngine,
                    backend = mockk(),
                )

            // when + then
            assertThat(daConnector.login(), `is`(expectedToken))
        }
    }

    @Test
    fun testGetCommunity() {
        runBlocking {
            // given
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            ByteReadChannel(
                                """
                        {"id":156,"name":"Test Christian","handle":"11159/4266","type":"community","link":"/econis-archiv/rest/communities/156","expand":[],"logo":null,"parentCommunity":null,"copyrightText":"","introductoryText":"","shortDescription":"TS","sidebarText":"","countItems":6,"subcommunities":[{"id":78,"name":"Centre for European Studies, Alexandru Ioan Cuza University of Iași","handle":"11159/1114","type":"community","link":"/econis-archiv/rest/communities/78","expand":["parentCommunity","collections","subCommunities","logo","all"],"logo":null,"parentCommunity":null,"copyrightText":"","introductoryText":"","shortDescription":"","sidebarText":"","countItems":9,"subcommunities":[],"collections":[]}],"collections":[{"id":249,"name":"Test Lori","handle":"11159/4267","type":"collection","link":"/econis-archiv/rest/collections/249","expand":["parentCommunityList","parentCommunity","items","license","logo","all"],"logo":null,"parentCommunity":null,"parentCommunityList":[],"items":[],"license":null,"copyrightText":"","introductoryText":"","shortDescription":"","sidebarText":"","numberItems":6}]}
                                """.trimMargin(),
                            ),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val daConnector =
                DAConnector(
                    config =
                        mockk {
                            every { digitalArchiveAddress } returns "http://primula-qs.zbw-nett.zbw-kiel.de/econis-archiv"
                            every { digitalArchiveBasicAuth } returns "pw"
                        },
                    engine = mockEngine,
                    backend = mockk(),
                )
            val expected = TEST_COMMUNITY

            // when
            val received = daConnector.getCommunityById("sometoken", 240)

            // then
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testStartFullImport() {
        runBlocking {
            // given
            val backend =
                spyk(
                    LoriServerBackend(
                        mockk {
                            coEvery { metadataDB.upsertMetadataBatch(any()) } returns IntArray(1) { 1 }
                        },
                        mockk<LoriConfiguration>(),
                    ),
                )
            val daConnector =
                spyk(
                    DAConnector(
                        config =
                            mockk {
                                every { digitalArchiveAddress } returns "http://primula-qs.zbw-nett.zbw-kiel.de/econis-archiv"
                            },
                        backend = backend,
                    ),
                ) {
                    coEvery { importCollection(any(), any(), any()) } returns 1
                }
            // when
            val receivedItems =
                daConnector.startFullImport(
                    "token",
                    TEST_COMMUNITY.copy(
                        collections =
                            listOf(
                                TEST_COLLECTION,
                                TEST_COLLECTION,
                                TEST_COLLECTION,
                            ),
                    ),
                )

            // then
            assertThat(receivedItems, `is`(listOf(1, 1, 1)))
            coVerify(exactly = 3) { daConnector.importCollection("token", any(), any()) }
        }
    }

    @Test
    fun testImportCollection() {
        runBlocking {
            // given
            val givenCollectionId = 6
            val mockEngine =
                MockEngine { request ->
                    if (request.url.toString().startsWith("$REST_URL/rest/collections/$givenCollectionId/items")) {
                        respond(
                            content =
                                ByteReadChannel(
                                    """
                                    [{"id":4594,"name":"National climate change mitigation legislation, strategy and targets: a global update",
                                    "handle":"11159/4268",
                                    "type":"item",
                                    "link":"/econis-archiv/rest/items/4594",
                                    "expand":[],
                                    "lastModified":"2022-01-20 03:00:22.582",
                                    "parentCollection":null,
                                    "parentCollectionList":[],
                                    "parentCommunityList":[],
                                    "metadata":[{"key":"dc.contributor.author","value":"Iacobuta, Gabriela","language":"DE_de"}],
                                    "bitstreams":[],
                                     "archived":"true",
                                     "withdrawn":"false"}] 
                                    """.trimIndent(),
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content =
                                ByteReadChannel(
                                    """
                                    {
                                      "id": 137,
                                      "name": "Monografien",
                                      "handle": "11159/1801",
                                      "type": "collection",
                                      "link": "/econis-archiv/rest/collections/137",
                                      "expand": [
                                        "parentCommunityList",
                                        "parentCommunity",
                                        "items",
                                        "license",
                                        "logo",
                                        "all"
                                      ],
                                      "logo": null,
                                      "parentCommunity": null,
                                      "parentCommunityList": [],
                                      "items": [],
                                      "license": null,
                                      "copyrightText": "",
                                      "introductoryText": "",
                                      "shortDescription": "",
                                      "sidebarText": "",
                                      "numberItems": 39
                                    }
                                    """.trimIndent(),
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            val daConnector =
                DAConnector(
                    config =
                        mockk {
                            every { digitalArchiveAddress } returns REST_URL
                            every { digitalArchiveBasicAuth } returns "pw"
                        },
                    engine = mockEngine,
                    backend =
                        mockk {
                            coEvery { upsertMetadata(any()) } returns IntArray(1) { _ -> 1 }
                        },
                )
            val expected = 1

            // when
            val received: Int =
                daConnector.importCollection(
                    "sometoken",
                    givenCollectionId,
                    TEST_COMMUNITY,
                )
            // then
            assertThat(received, `is`(expected))
        }
    }

    @Test
    fun testImportCollectionZeroEntries() {
        runBlocking {
            // given
            val givenCommunityId = 6
            val mockEngine =
                MockEngine { request ->
                    if (request.url.toString().startsWith("$REST_URL/rest/collections/$givenCommunityId/items")) {
                        respond(
                            content =
                                ByteReadChannel(
                                    """
                                    [{"id":4594,"name":"National climate change mitigation legislation, strategy and targets: a global update",
                                    "handle":"11159/4268",
                                    "type":"item",
                                    "link":"/econis-archiv/rest/items/4594",
                                    "expand":[],
                                    "lastModified":"2022-01-20 03:00:22.582",
                                    "parentCollection":null,
                                    "parentCollectionList":[],
                                    "parentCommunityList":[],
                                    "metadata":[{"key":"dc.contributor.author","value":"Iacobuta, Gabriela","language":"DE_de"}],
                                    "bitstreams":[{
                                         "id":13358,
                                         "name":"National climate change mitigation legislation strategy and targets a global update.pdf.txt",
                                         "handle":null,
                                         "type":"bitstream",
                                         "link":"/econis-archiv/rest/bitstreams/13358",
                                         "expand":["parent","policies","all"],
                                         "bundleName":"TEXT",
                                         "description":"Extracted Text",
                                         "format":"Text",
                                         "mimeType":"text/plain",
                                         "sizeBytes":60303,
                                         "parentObject":null,
                                         "retrieveLink":"/econis-archiv/rest/bitstreams/13358/retrieve",
                                         "checkSum":{"value":"ff6f2af4afdf3b8afabf85de4a77be97","checkSumAlgorithm":"MD5"},
                                         "sequenceId":3,
                                         "policies":null
                                         }],
                                     "archived":"true",
                                     "withdrawn":"false"}] 
                                    """.trimIndent(),
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    } else {
                        respond(
                            content =
                                ByteReadChannel(
                                    """
                                    {
                                      "id": 137,
                                      "name": "Monografien",
                                      "handle": "11159/1801",
                                      "type": "collection",
                                      "link": "/econis-archiv/rest/collections/137",
                                      "expand": [
                                        "parentCommunityList",
                                        "parentCommunity",
                                        "items",
                                        "license",
                                        "logo",
                                        "all"
                                      ],
                                      "logo": null,
                                      "parentCommunity": null,
                                      "parentCommunityList": [],
                                      "items": [],
                                      "license": null,
                                      "copyrightText": "",
                                      "introductoryText": "",
                                      "shortDescription": "",
                                      "sidebarText": "",
                                      "numberItems": 0
                                    }
                                    """.trimIndent(),
                                ),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            val daConnector =
                DAConnector(
                    config =
                        mockk {
                            every { digitalArchiveAddress } returns REST_URL
                            every { digitalArchiveBasicAuth } returns "pw"
                        },
                    engine = mockEngine,
                    backend = mockk(),
                )
            val expected = 0

            // when
            val received: Int = daConnector.importCollection("sometoken", givenCommunityId, TEST_COMMUNITY)
            // then
            assertThat(received, `is`(expected))
        }
    }

    @DataProvider(name = DATA_FOR_SERIALIZATION)
    fun createSerializerData() =
        arrayOf(
            arrayOf(
                TEST_ITEM,
            ),
            arrayOf(
                TEST_ITEM.copy(
                    name = null,
                    handle = null,
                    type = null,
                    lastModified = null,
                    parentCollection =
                        TEST_COLLECTION.copy(
                            handle = null,
                            type = null,
                            logo =
                                TEST_BITSTREAM.copy(
                                    handle = "foo",
                                    type = null,
                                    bundleName = null,
                                    description = null,
                                    format = null,
                                    mimeType = null,
                                    sizeBytes = null,
                                    parentObject =
                                        DAObject(
                                            id = 5,
                                            name = "foo",
                                            handle = "bla",
                                            type = "foo",
                                            link = "link",
                                            expand = listOf("nul"),
                                        ),
                                    retrieveLink = null,
                                    checkSum = null,
                                    sequenceId = null,
                                    policies =
                                        DAResourcePolicy(
                                            action = "foo",
                                            endDate = "date",
                                            epersonId = 5,
                                            groupid = 6,
                                            id = 111,
                                            resourceId = 7,
                                            resourceTypeId = "type",
                                            rpDescription = "desc",
                                            rpName = "name",
                                            rpType = "type",
                                            startDate = "date",
                                        ),
                                ),
                        ),
                    parentCollectionList = listOf(TEST_COLLECTION),
                ),
            ),
        )

    @Test(dataProvider = DATA_FOR_SERIALIZATION)
    fun testSerializationItem(item: DAItem) {
        assertThat(
            Json.decodeFromString<DAItem>(Json.encodeToString(item)),
            `is`(item),
        )
    }

    @Test
    fun testShortenHandle() {
        val given: ItemMetadata =
            TEST_METADATA.copy(
                handle = "http://hdl.handle.net/11159/42",
            )

        val received = DAConnector.shortenHandle(given)
        assertThat(
            received,
            `is`(given.copy(handle = "11159/42")),
        )
    }

    companion object {
        const val DATA_FOR_SERIALIZATION = "DATA_FOR_SERIALIZATION"
        const val REST_URL = "http://test-archive.de"

        val TEST_SUBCOMMUNITY =
            DACommunity(
                id = 78,
                name = "Centre for European Studies, Alexandru Ioan Cuza University of Iași",
                handle = "11159/1114",
                type = "community",
                link = "/econis-archiv/rest/communities/78",
                expand =
                    listOf(
                        "parentCommunity",
                        "collections",
                        "subCommunities",
                        "logo",
                        "all",
                    ),
                logo = null,
                parentCommunity = null,
                copyrightText = "",
                introductoryText = "",
                shortDescription = "",
                sidebarText = "",
                countItems = 9,
                subcommunities = emptyList(),
                collections = emptyList(),
            )
        val TEST_COLLECTION =
            DACollection(
                id = 249,
                name = "Test Lori",
                handle = "11159/4267",
                type = "collection",
                link = "/econis-archiv/rest/collections/249",
                expand =
                    listOf(
                        "parentCommunityList",
                        "parentCommunity",
                        "items",
                        "license",
                        "logo",
                        "all",
                    ),
                logo = null,
                parentCommunity = null,
                parentCommunityList = emptyList(),
                items = emptyList(),
                license = null,
                copyrightText = "",
                introductoryText = "",
                shortDescription = "",
                sidebarText = "",
                numberItems = 6,
            )

        val TEST_COMMUNITY =
            DACommunity(
                id = 156,
                name = "Test Christian",
                handle = "11159/4266",
                type = "community",
                countItems = 6,
                link = "/econis-archiv/rest/communities/156",
                expand = emptyList(),
                logo = null,
                parentCommunity = null,
                copyrightText = "",
                introductoryText = "",
                shortDescription = "TS",
                sidebarText = "",
                subcommunities = listOf(TEST_SUBCOMMUNITY),
                collections = listOf(TEST_COLLECTION),
            )

        val TEST_BITSTREAM =
            DABitstream(
                id = 13358,
                name = "National climate change mitigation legislation strategy and targets a global update.pdf.txt",
                handle = null,
                type = "bitstream",
                link = "/econis-archiv/rest/bitstreams/13358",
                expand = listOf("parent", "policies", "all"),
                bundleName = "TEXT",
                description = "Extracted Text",
                format = "Text",
                mimeType = "text/plain",
                sizeBytes = 60303,
                parentObject = null,
                retrieveLink = "/econis-archiv/rest/bitstreams/13358/retrieve",
                checkSum =
                    DAChecksum(
                        value = "ff6f2af4afdf3b8afabf85de4a77be97",
                        checkSumAlgorithm = "MD5",
                    ),
                sequenceId = 3,
                policies = null,
            )

        val TEST_ITEM =
            DAItem(
                id = 4594,
                name = "National climate change mitigation legislation, strategy and targets: a global update",
                handle = "11159/4268",
                type = "item",
                link = "/econis-archiv/rest/items/4594",
                expand = emptyList(),
                lastModified = "2022-01-20 03:00:22.582",
                parentCollection = null,
                parentCollectionList = emptyList(),
                parentCommunityList = emptyList(),
                metadata =
                    listOf(
                        DAMetadata(
                            key = "dc.contributor.author",
                            value = "Iacobuta, Gabriela",
                            language = "DE_de",
                        ),
                    ),
                bitstreams = listOf(TEST_BITSTREAM),
                archived = "true",
                withdrawn = "false",
            )
    }
}
