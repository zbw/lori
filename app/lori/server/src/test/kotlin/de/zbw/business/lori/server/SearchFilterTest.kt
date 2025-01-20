package de.zbw.business.lori.server

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.TemporalValidity
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
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val publicationDateFilter =
        listOf(
            TEST_Metadata.copy(
                collectionName = "subject1 subject2 subject3",
                handle = "publicationDate2022",
                publicationDate = LocalDate.of(2022, 1, 1),
            ),
        )

    private val publicationTypeFilter =
        listOf(
            TEST_Metadata.copy(
                collectionName = "subject4",
                handle = "publicationTypeArticle",
                publicationType = PublicationType.PROCEEDING,
                publicationDate = LocalDate.of(2022, 1, 1),
            ),
            TEST_Metadata.copy(
                collectionName = "subject4",
                handle = "publicationTypeWorkingPaper",
                publicationType = PublicationType.WORKING_PAPER,
                publicationDate = LocalDate.of(2020, 1, 1),
            ),
        )

    private val zdbIdFilterItems =
        listOf(
            TEST_Metadata.copy(
                handle = "journalId only",
                zdbIdJournal = "555nase",
                zdbIdSeries = null,
            ),
            TEST_Metadata.copy(
                handle = "seriesId only",
                zdbIdJournal = null,
                zdbIdSeries = "444nase",
            ),
            TEST_Metadata.copy(
                handle = "both zdb ids",
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
                listOf(PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                setOf(publicationTypeFilter[0]),
                1,
                "search with publication type filter for articles",
            ),
            arrayOf(
                "col:'subject4'",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
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
                            PublicationType.PROCEEDING,
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
            runBlocking {
                backend.searchQuery(
                    searchTerm,
                    10,
                    0,
                    searchFilter,
                )
            }

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
                            PublicationType.PROCEEDING,
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
            runBlocking {
                backend.searchQuery(
                    null,
                    10,
                    0,
                    searchFilter,
                )
            }

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
            runBlocking {
                backend.searchQuery(
                    null,
                    10,
                    0,
                    searchFilter,
                )
            }

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

    @DataProvider(name = DATA_FOR_TO_STRING)
    fun createDataForToString() =
        arrayOf(
            arrayOf(
                listOf(QueryParameterParser.parseAccessStateOnDate("OPEN+2025-01-02")),
                "acd:\"OPEN+2025-01-02\"",
                "ACD",
            ),
            arrayOf(
                emptyList<SearchFilter?>(),
                "",
                "nothing",
            ),
            arrayOf(
                emptyList<SearchFilter?>(),
                "",
                "EmptyList",
            ),
            arrayOf(
                listOf<SearchFilter>(
                    CollectionNameFilter("collection"),
                ),
                "col:\"collection\"",
                "One search filter, no search term",
            ),
            arrayOf(
                listOf<SearchFilter?>(null),
                "",
                "List with nulls",
            ),
            arrayOf(
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED)),
                    CollectionNameFilter("collection"),
                    CollectionHandleFilter("12345/nase"),
                    CommunityNameFilter("community"),
                    CommunityHandleFilter("12345/hut"),
                    QueryParameterParser.parseEndDateFilter("2022-06-01"),
                    QueryParameterParser.parseStartDateFilter("2022-06-01"),
                    TitleFilter("some title"),
                    TitleFilter("another"),
                    PublicationDateFilter(2000, 2020),
                    NoRightInformationFilter(),
                    SeriesFilter(listOf("series1", "series2")),
                    PublicationTypeFilter(listOf(PublicationType.PROCEEDING, PublicationType.BOOK_PART)),
                    FormalRuleFilter(
                        listOf(
                            FormalRule.LICENCE_CONTRACT,
                            FormalRule.ZBW_USER_AGREEMENT,
                            FormalRule.OPEN_CONTENT_LICENCE,
                        ),
                    ),
                    TemporalValidityFilter(
                        listOf(
                            TemporalValidity.PAST,
                            TemporalValidity.PRESENT,
                            TemporalValidity.FUTURE,
                        ),
                    ),
                    TemplateNameFilter(listOf("555nase")),
                    QueryParameterParser.parseAccessStateOnDate("RESTRICTED+2025-01-21"),
                    QueryParameterParser.parseManualRightFilter("true"),
                ),
                "acc:\"OPEN,RESTRICTED\"" +
                    " & col:\"collection\"" +
                    " & hdlcol:\"12345/nase\"" +
                    " & com:\"community\"" +
                    " & hdlcom:\"12345/hut\"" +
                    " & zge:\"2022-06-01\"" +
                    " & zgb:\"2022-06-01\"" +
                    " & tit:\"some title\"" +
                    " & tit:\"another\"" +
                    " & jah:2000-2020" +
                    " & nor:on" +
                    " & ser:\"series1,series2\"" +
                    " & typ:\"PROCEEDING,BOOK_PART\"" +
                    " & reg:\"LICENCE_CONTRACT,ZBW_USER_AGREEMENT,OPEN_CONTENT_LICENCE\"" +
                    " & zga:\"PAST,PRESENT,FUTURE\"" +
                    " & tpl:\"555nase\"" +
                    " & acd:\"RESTRICTED+2025-01-21\"" +
                    " & man:on",
                "All filters",
            ),
        )

    @Test(dataProvider = DATA_FOR_TO_STRING)
    fun testToString(
        filters: List<SearchFilter?>,
        expected: String,
        reason: String,
    ) {
        val searchTermReceived = SearchFilter.filtersToString(filters.filterNotNull())
        assertThat(
            reason,
            searchTermReceived,
            `is`(expected),
        )

        val roundTripFilters =
            LoriServerBackend.parseSearchTermToFilters(
                searchTermReceived.replace(" & ", " "),
            )
        assertThat(
            reason,
            roundTripFilters.toString(),
            `is`(
                filters.filterNotNull().toString(),
            ),
        )
    }

    @DataProvider(name = DATA_FOR_BOOKMARK_TO_STRING)
    fun createDataForBookmarkToString() =
        arrayOf(
            arrayOf(
                Bookmark(
                    bookmarkName = "test1",
                    bookmarkId = 1,
                    searchTerm = "hdl:1234/5 & tit:infrastructure",
                    publicationTypeFilter =
                        PublicationTypeFilter(
                            listOf(PublicationType.PROCEEDING),
                        ),
                    validOnFilter = RightValidOnFilter(LocalDate.of(2023, 11, 10)),
                ),
                "(hdl:1234/5 & tit:infrastructure) & (typ:\"PROCEEDING\" & zgp:\"2023-11-10\")",
                "Searchterm in combination with filter",
            ),
            arrayOf(
                Bookmark(
                    bookmarkName = "test1",
                    bookmarkId = 1,
                    searchTerm = "",
                    publicationTypeFilter =
                        PublicationTypeFilter(
                            listOf(PublicationType.PROCEEDING),
                        ),
                    validOnFilter = RightValidOnFilter(LocalDate.of(2023, 11, 10)),
                ),
                "typ:\"PROCEEDING\" & zgp:\"2023-11-10\"",
                "No Searchterm",
            ),
            arrayOf(
                Bookmark(
                    bookmarkName = "test1",
                    bookmarkId = 1,
                    searchTerm = "hdl:1234/5 & tit:infrastructure",
                ),
                "hdl:1234/5 & tit:infrastructure",
                "Searchterm only",
            ),
        )

    @Test(dataProvider = DATA_FOR_BOOKMARK_TO_STRING)
    fun testBookmarkToString(
        bookmark: Bookmark,
        expected: String,
        reason: String,
    ) {
        assertThat(
            reason,
            SearchFilter.bookmarkToString(bookmark),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_PUBLICATION_DATE = "DATA_FOR_PUBLICATION_DATE"
        const val DATA_FOR_NO_SEARCH_TERM = "DATA_FOR_NO_SEARCH_TERM"
        const val DATA_FOR_ZDB_ID = "DATA_FOR_ZDB_ID"
        const val DATA_FOR_PREPARE_VALUE = "DATA_FOR_PREPARE_VALUE"
        const val DATA_FOR_TO_STRING = "DATA_FOR_TO_STRING"
        const val DATA_FOR_BOOKMARK_TO_STRING = "DATA_FOR_BOOKMARK_TO_STRING"
    }
}
