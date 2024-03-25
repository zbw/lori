package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.SearchKey
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant

class SearchKeyTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialMetadata() = listOf(
        METADATA_TEST
    )

    @BeforeClass
    fun fillDB() {
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

    @DataProvider(name = DATA_FOR_METADATA_ID_KEY)
    fun createDataForMetadataId() = arrayOf(
        arrayOf(
            "metadataid:'${METADATA_TEST.metadataId}'",
            10,
            0,
            setOf(METADATA_TEST),
            1,
            "search for specific metadata id"
        ),
        arrayOf(
            "metadataid:'$NO_VALID_METADATA_ID' | (hdl:'${METADATA_TEST.handle}')",
            10,
            0,
            setOf(METADATA_TEST),
            1,
            "search for specific metadata id with complex query"
        ),
        arrayOf(
            "metadataid:'$NO_VALID_METADATA_ID' | (hdl:'${METADATA_TEST.handle}' & !tit:'stupid title')",
            10,
            0,
            setOf(METADATA_TEST),
            1,
            "search for specific metadata id with even complexer query"
        ),
        arrayOf(
            "metadataid:'$NO_VALID_METADATA_ID'",
            10,
            0,
            emptySet<ItemMetadata>(),
            0,
            "fail to find a metadata id"
        ),
        arrayOf(
            "sig:'${METADATA_TEST.paketSigel}' & (!hdl:'nonse' & !tit:'stupid title')",
            10,
            0,
            setOf(METADATA_TEST),
            1,
            "search for specific metadata id with even complexer query ensuring parantheses works as expected"
        ),
        arrayOf(
            "sig:'${METADATA_TEST.paketSigel}' & !(hdl:'nonse' | tit:'stupid title')",
            10,
            0,
            setOf(METADATA_TEST),
            1,
            "negation around parantheses"
        ),
        arrayOf(
            "lur:'${METADATA_TEST.licenceUrl}'",
            10,
            0,
            setOf(METADATA_TEST),
            1,
            "search for licence url"
        ),
    )

    @Test(dataProvider = DATA_FOR_METADATA_ID_KEY)
    fun findMetadataId(
        searchTerm: String,
        limit: Int,
        offset: Int,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val searchQueryResult: SearchQueryResult = backend.searchQuery(searchTerm, limit, offset)

        // then
        assertThat(
            description,
            searchQueryResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            description,
            searchQueryResult.numberOfResults,
            `is`(expectedNumberOfResults),
        )
    }

    @Test
    fun testBijectivity() {
        SearchKey.values().map { key ->
            assertThat(
                SearchKey.toEnum(key.fromEnum()),
                `is`(key),
            )
        }
    }

    companion object {
        const val DATA_FOR_METADATA_ID_KEY = "DATA_FOR_METADATA_ID_KEY "
        const val NO_VALID_METADATA_ID = "INVALID"
        private const val TEST_METADATA_ID = "some metadata id"
        val METADATA_TEST = TEST_Metadata.copy(
            metadataId = TEST_METADATA_ID,
        )
    }
}
