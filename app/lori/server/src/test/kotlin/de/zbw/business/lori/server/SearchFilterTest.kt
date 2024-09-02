package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
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
import java.lang.Integer.max
import java.time.Instant
import java.time.LocalDate

/**
 * Test search for multiple words.
 *
 * Created on 09-19-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SearchFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connection = dataSource.connection,
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val publicationDateFilter =
        listOf(
            TEST_Metadata.copy(
                collectionName = "subject1 subject2 subject3",
                metadataId = "publicationDate2022",
                publicationDate = LocalDate.of(2022, 1, 1),
            ),
        )

    private val publicationTypeFilter =
        listOf(
            TEST_Metadata.copy(
                collectionName = "subject4",
                metadataId = "publicationTypeArticle",
                publicationType = PublicationType.PROCEEDINGS,
                publicationDate = LocalDate.of(2022, 1, 1),
            ),
            TEST_Metadata.copy(
                collectionName = "subject4",
                metadataId = "publicationTypeWorkingPaper",
                publicationType = PublicationType.WORKING_PAPER,
                publicationDate = LocalDate.of(2020, 1, 1),
            ),
        )

    private val zdbIdFilterItems =
        listOf(
            TEST_Metadata.copy(
                metadataId = "journalId only",
                zdbIdJournal = "555nase",
                zdbIdSeries = null,
            ),
            TEST_Metadata.copy(
                metadataId = "seriesId only",
                zdbIdJournal = null,
                zdbIdSeries = "444nase",
            ),
            TEST_Metadata.copy(
                metadataId = "both zdb ids",
                zdbIdJournal = "444nase",
                zdbIdSeries = "333nase",
            ),
        )

    private fun getInitialMetadata() =
        listOf(
            publicationDateFilter,
            publicationTypeFilter,
            zdbIdFilterItems,
        ).flatten()

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

    @DataProvider(name = DATA_FOR_PUBLICATION_DATE)
    fun createDataForPublicationDate() =
        arrayOf(
            arrayOf(
                "col:subject1 | col:subject4",
                listOf(PublicationDateFilter(2021, 2023)),
                listOf(publicationDateFilter[0], publicationTypeFilter[0]).toSet(),
                2,
                "search with filter in range",
            ),
            arrayOf(
                "col:'subject4'",
                listOf(PublicationDateFilter(2020, 2021)),
                setOf(publicationTypeFilter[1]),
                1,
                "search with filter out of range",
            ),
            arrayOf(
                "col:'subject4'",
                listOf(PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                setOf(publicationTypeFilter[0]),
                1,
                "search with publication type filter for articles",
            ),
            arrayOf(
                "col:'subject4'",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                            PublicationType.WORKING_PAPER,
                        ),
                    ),
                ),
                publicationTypeFilter.toSet(),
                2,
                "search with publication type filter for articles and working paper",
            ),
            arrayOf(
                "col:'subject4'",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                            PublicationType.WORKING_PAPER,
                        ),
                    ),
                    PublicationDateFilter(fromYear = 2022, toYear = 2022),
                ),
                setOf(publicationTypeFilter[0]),
                1,
                "search with publication type and publication date combined",
            ),
        )

    @Test(dataProvider = DATA_FOR_PUBLICATION_DATE)
    fun testFilterByPublicationDate(
        searchTerm: String,
        searchFilter: List<MetadataSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val (numberOfResults, searchResult) =
            backend.searchQuery(
                searchTerm,
                10,
                0,
                searchFilter,
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

    @DataProvider(name = DATA_FOR_NO_SEARCH_TERM)
    fun createDataForNoSearchTerm() =
        arrayOf(
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                            PublicationType.WORKING_PAPER,
                        ),
                    ),
                    PublicationDateFilter(fromYear = 2022, toYear = 2022),
                ),
                setOf(publicationTypeFilter[0]),
                "Filter for publication type and publication date",
            ),
        )

    @Test(dataProvider = DATA_FOR_NO_SEARCH_TERM)
    fun testFilterNoSearchTerm(
        searchFilter: List<MetadataSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            backend.searchQuery(
                null,
                10,
                0,
                searchFilter,
            )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    @DataProvider(name = DATA_FOR_ZDB_ID)
    fun createDataForZDBID() =
        arrayOf(
            arrayOf(
                listOf(
                    ZDBIdFilter(
                        listOf(
                            "555nase",
                        ),
                    ),
                ),
                setOf(zdbIdFilterItems[0]),
                "Only journal id",
            ),
            arrayOf(
                listOf(
                    ZDBIdFilter(
                        listOf(
                            "444nase",
                        ),
                    ),
                ),
                setOf(zdbIdFilterItems[1], zdbIdFilterItems[2]),
                "Series id and journal id",
            ),
            arrayOf(
                listOf(
                    ZDBIdFilter(
                        listOf(
                            "333nase",
                        ),
                    ),
                ),
                setOf(zdbIdFilterItems[2]),
                "Only series id",
            ),
        )

    @Test(dataProvider = DATA_FOR_ZDB_ID)
    fun testFilterZDBId(
        searchFilter: List<MetadataSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            backend.searchQuery(
                null,
                10,
                0,
                searchFilter,
            )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    @Test
    fun testHelperAddDefaultEntriesToMap() {
        val expected1 = mapOf(Pair("foobar", 1), Pair("baz", 0))
        assertThat(
            DatabaseConnector.addDefaultEntriesToMap(
                mapOf(Pair("foobar", 1)),
                setOf("foobar", "baz"),
                0,
            ) { a, b -> max(a, b) },
            `is`(expected1),
        )
    }

    @DataProvider(name = DATA_FOR_PREPARE_VALUE)
    fun createDataForPrepareValue() =
        arrayOf(
            arrayOf(
                "(FOO & BAR | BAZ)",
                "\\(FOO & \\& & BAR & \\| & BAZ\\)",
                "Correct escaping of operators",
            ),
        )

    @Test(dataProvider = DATA_FOR_PREPARE_VALUE)
    fun testPrepareTSVectorValues(
        input: String,
        expected: String,
        reason: String,
    ) {
        assertThat(
            reason,
            TSVectorMetadataSearchFilter.prepareValue(input),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_PUBLICATION_DATE = "DATA_FOR_PUBLICATION_DATE"
        const val DATA_FOR_NO_SEARCH_TERM = "DATA_FOR_NO_SEARCH_TERM"
        const val DATA_FOR_ZDB_ID = "DATA_FOR_ZDB_ID"
        const val DATA_FOR_PREPARE_VALUE = "DATA_FOR_PREPARE_VALUE"
    }
}
