package de.zbw.business.lori.server

import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseConnectorTest
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
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
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private val multipleWords = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "multiple word",
        collectionName = "subject1 subject2 subject3"
    )

    private fun getInitialMetadata() = listOf(
        multipleWords,
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns DatabaseConnectorTest.NOW.toInstant()
        getInitialMetadata().forEach {
            backend.insertMetadataElement(it)
        }
    }

    @DataProvider(name = DATA_FOR_MULTIPLE_WORDS)
    fun createDataForMultipleWords() = arrayOf(
        arrayOf(
            "col:'subject1 subject2'",
            10,
            0,
            multipleWords,
            1,
            "search for two words next to each other"
        ),
        arrayOf(
            "col:'subject1 subject3'",
            10,
            0,
            multipleWords,
            1,
            "search for two words separated from each other"
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
        val (numberOfResults, searchResult) = backend.searchQuery(searchTerm, limit, offset)

        // then
        assertThat(
            description,
            searchResult.map { it.metadata }.toSet(),
            `is`(setOf(expectedResult)),
        )
        assertThat(
            description,
            numberOfResults,
            `is`(expectedNumberOfResults),
        )
    }

    companion object {
        const val DATA_FOR_MULTIPLE_WORDS = "DATA_FOR_MULTIPLE_WORDS"
    }
}
