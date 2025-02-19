package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant

class SearchKeyTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private fun getInitialMetadata() =
        listOf(
            METADATA_TEST,
            METADATA_TEST_2,
            METADATA_TEST_3,
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            getInitialMetadata().forEach {
                backend.insertMetadataElement(it)
            }
        }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_SEARCH_QUERY)
    fun createDataForSearchQuery() =
        arrayOf(
            arrayOf(
                "!${FilterType.ZDB_ID.keyAlias}:'${METADATA_TEST.zdbIdJournal}'",
                10,
                0,
                setOf(METADATA_TEST_2, METADATA_TEST_3),
                "find all items that have a different ZDB-ID or no value at all",
            ),
            arrayOf(
                "${FilterType.HANDLE.keyAlias}:'${METADATA_TEST.handle}'",
                10,
                0,
                setOf(METADATA_TEST),
                "search for specific handle",
            ),
            arrayOf(
                "${FilterType.HANDLE.keyAlias}:'$NO_VALID_HANDLE' | (${FilterType.HANDLE.keyAlias}:'${METADATA_TEST.handle}')",
                10,
                0,
                setOf(METADATA_TEST),
                "search for specific handle with complex query",
            ),
            arrayOf(
                "${FilterType.HANDLE.keyAlias}:'$NO_VALID_HANDLE' | (${FilterType.HANDLE.keyAlias}:'${METADATA_TEST.handle}'" +
                    " & !${FilterType.TITLE.keyAlias}:'stupid title')",
                10,
                0,
                setOf(METADATA_TEST),
                "search for specific metadata id with even complexer query",
            ),
            arrayOf(
                "${FilterType.HANDLE.keyAlias}:'$NO_VALID_HANDLE'",
                10,
                0,
                emptySet<ItemMetadata>(),
                "fail to find a metadata id",
            ),
            arrayOf(
                "${FilterType.PAKET_SIGEL.keyAlias}:'${METADATA_TEST.paketSigel!!.joinToString(
                    separator = ",",
                )}' & (!${FilterType.HANDLE.keyAlias}:'nonse'" +
                    " & !${FilterType.TITLE.keyAlias}:'stupid title')",
                10,
                0,
                setOf(METADATA_TEST),
                "search for specific metadata id with even complexer query ensuring parantheses works as expected",
            ),
            arrayOf(
                "${FilterType.PAKET_SIGEL.keyAlias}:'${METADATA_TEST.paketSigel!!.joinToString(separator = ",")}'" +
                    " & !(${FilterType.HANDLE.keyAlias}:'nonse'" +
                    " | ${FilterType.TITLE.keyAlias}:'stupid title')",
                10,
                0,
                setOf(METADATA_TEST),
                "negation around parantheses",
            ),
            arrayOf(
                "${FilterType.LICENCE_URL.keyAlias}:'${METADATA_TEST.licenceUrlFilter}'",
                10,
                0,
                setOf(METADATA_TEST),
                "search for licence url",
            ),
            arrayOf(
                "${FilterType.LICENCE_URL.keyAlias}:'other'",
                10,
                0,
                setOf(METADATA_TEST_2, METADATA_TEST_3),
                "search for licence url 'other'",
            ),
            arrayOf(
                "${FilterType.SUB_COMMUNITY_NAME.keyAlias}:'${METADATA_TEST_3.subCommunityName}'",
                10,
                0,
                setOf(METADATA_TEST_3),
                "search by subcommunity name",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_QUERY)
    fun testSearchQuery(
        searchTerm: String,
        limit: Int,
        offset: Int,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchQueryResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(searchTerm, limit, offset)
            }

        // then
        assertThat(
            description,
            searchQueryResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    companion object {
        const val DATA_FOR_SEARCH_QUERY = "DATA_FOR_SEARCH_QUERY"
        const val NO_VALID_HANDLE = "INVALID"
        private const val TEST_HANDLE = "some handle"
        val METADATA_TEST =
            TEST_Metadata.copy(
                handle = TEST_HANDLE,
            )
        val METADATA_TEST_2 =
            TEST_Metadata.copy(
                handle = "second",
                zdbIdJournal = null,
                licenceUrl = "foobar.baz",
                licenceUrlFilter = "other",
                paketSigel = listOf("someothersigel2"),
            )
        val METADATA_TEST_3 =
            TEST_Metadata.copy(
                handle = "third",
                zdbIdJournal = "someotherzdbid",
                licenceUrl = "foobar",
                licenceUrlFilter = "other",
                paketSigel = listOf("someothersigel"),
                subCommunityName = "department 3",
            )
    }
}
