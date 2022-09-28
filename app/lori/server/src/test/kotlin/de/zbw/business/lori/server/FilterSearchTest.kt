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
import java.time.LocalDate

/**
 * Test search for multiple words.
 *
 * Created on 09-19-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class FilterSearchTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private val publicationDateFilter = DatabaseConnectorTest.TEST_Metadata.copy(
        collectionName = "subject1 subject2 subject3",
        metadataId = "publicationDate2022",
        publicationDate = LocalDate.of(2022, 1, 1)
    )

    private fun getInitialMetadata() = listOf(
        publicationDateFilter,
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns DatabaseConnectorTest.NOW.toInstant()
        getInitialMetadata().forEach {
            backend.insertMetadataElement(it)
        }
    }

    @DataProvider(name = DATA_FOR_PUBLICATION_DATE)
    fun createDataForPublicationDate() = arrayOf(
        arrayOf(
            "col:'subject1 subject2'",
            PublicationDateFilter(2021, 2023),
            setOf(publicationDateFilter),
            1,
            "search with filter in range",
        ),
        arrayOf(
            "col:'subject1 subject2'",
            PublicationDateFilter(2020, 2021),
            emptySet<ItemMetadata>(),
            0,
            "search with filter out of range",
        ),
    )

    @Test(dataProvider = DATA_FOR_PUBLICATION_DATE)
    fun testFilterByPublicationDate(
        searchTerm: String,
        searchFilter: SearchFilter,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val (numberOfResults, searchResult) = backend.searchQuery(
            searchTerm,
            10,
            0,
            listOf(searchFilter),
        )

        // then
        assertThat(
            description,
            searchResult.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            description,
            numberOfResults,
            `is`(expectedNumberOfResults),
        )
    }

    companion object {
        const val DATA_FOR_PUBLICATION_DATE = "DATA_FOR_MULTIPLE_WORDS"
    }
}
