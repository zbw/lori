package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
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

/**
 * Test search for multiple words.
 *
 * Created on 09-19-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class MultipleWordSearchTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connection = dataSource.connection,
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val multipleWords =
        TEST_Metadata.copy(
            metadataId = "multiple word",
            collectionName = "subject1 subject2 subject3",
        )

    private fun getInitialMetadata() =
        listOf(
            multipleWords,
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

    @DataProvider(name = DATA_FOR_MULTIPLE_WORDS)
    fun createDataForMultipleWords() =
        arrayOf(
            arrayOf(
                "col:'subject1 & subject2'",
                10,
                0,
                multipleWords,
                1,
                "search for two words next to each other",
            ),
            arrayOf(
                "col:'subject1 & subject3'",
                10,
                0,
                multipleWords,
                1,
                "search for two words separated from each other",
            ),
        )

    @Test(dataProvider = DATA_FOR_MULTIPLE_WORDS)
    fun findMultipleWords(
        searchTerm: String,
        limit: Int,
        offset: Int,
        expectedResult: ItemMetadata,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val searchQueryResult: SearchQueryResult = backend.searchQuery(searchTerm, limit, offset)

        // then
        assertThat(
            description,
            searchQueryResult.results.map { it.metadata }.toSet(),
            `is`(setOf(expectedResult)),
        )
        assertThat(
            description,
            searchQueryResult.numberOfResults,
            `is`(expectedNumberOfResults),
        )
    }

    companion object {
        const val DATA_FOR_MULTIPLE_WORDS = "DATA_FOR_MULTIPLE_WORDS"
    }
}
