package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.CollectionNameFilter
import de.zbw.business.lori.server.CommunityNameFilter
import de.zbw.business.lori.server.HandleFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SEAnd
import de.zbw.business.lori.server.type.SEOr
import de.zbw.business.lori.server.type.SEPar
import de.zbw.business.lori.server.type.SEVariable
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ACCESS_STATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_TEMPLATE_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_DELETED
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LICENCE_URL_FILTER
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PUBLICATION_YEAR
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_SUBCOMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ZDB_IDS
import de.zbw.persistence.lori.server.SearchDB.Companion.ALIAS_ITEM_METADATA
import de.zbw.persistence.lori.server.SearchDB.Companion.ALIAS_ITEM_RIGHT
import de.zbw.persistence.lori.server.SearchDB.Companion.buildSearchQueryOccurrence
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant

/**
 * Testing [SearchDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
@Suppress("ktlint:standard:max-line-length")
class SearchDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun searchMetadata() =
        runBlocking {
            // given
            val testZDB = TEST_Metadata.copy(handle = "searchZBD", zdbIds = listOf("zbdId"))
            dbConnector.metadataDB.insertMetadata(testZDB)

            // when
            val searchPairsZDB =
                SEVariable(
                    ZDBIdFilter(testZDB.zdbIds!!),
                )
            val resultZDB =
                dbConnector.searchDB.searchMetadataItems(
                    searchExpression = searchPairsZDB,
                    limit = 5,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            val numberResultZDB =
                dbConnector.searchDB.countSearchMetadata(
                    searchExpression = searchPairsZDB,
                    metadataSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            // then
            assertThat(resultZDB[0], `is`(testZDB))
            assertThat(numberResultZDB, `is`(1))
            // when
            val searchPairsAll =
                SEAnd(
                    SEVariable(CollectionNameFilter(testZDB.collectionName!!)),
                    SEAnd(
                        SEVariable(CommunityNameFilter(testZDB.communityName!!)),
                        SEAnd(
                            SEVariable(
                                PaketSigelFilter(testZDB.paketSigel!!),
                            ),
                            SEVariable(ZDBIdFilter(testZDB.zdbIds)),
                        ),
                    ),
                )
            val resultAll =
                dbConnector.searchDB.searchMetadataItems(
                    searchExpression = searchPairsAll,
                    limit = 5,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            val numberResultAll =
                dbConnector.searchDB.countSearchMetadata(
                    searchExpression = searchPairsAll,
                    metadataSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            // then
            assertThat(resultAll.toSet(), `is`(setOf(testZDB)))
            assertThat(numberResultAll, `is`(1))

            // Add second metadata with same zbdID
            val testZDB2 = TEST_Metadata.copy(handle = "searchZBD2", zdbIds = listOf("zbdId"))
            dbConnector.metadataDB.insertMetadata(testZDB2)
            // when
            val resultZBD2 =
                dbConnector.searchDB.searchMetadataItems(
                    searchExpression = searchPairsZDB,
                    limit = 5,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            val numberResultZDB2 =
                dbConnector.searchDB.countSearchMetadata(
                    searchExpression = searchPairsAll,
                    metadataSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            // then
            assertThat(resultZBD2.toSet(), `is`(setOf(testZDB, testZDB2)))
            assertThat(numberResultZDB2, `is`(2))

            // when
            val resultZDB2Offset =
                dbConnector.searchDB.searchMetadataItems(
                    searchExpression = searchPairsZDB,
                    limit = 5,
                    offset = 1,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                )
            assertThat(
                resultZDB2Offset.size,
                `is`(1),
            )
        }

    @DataProvider(name = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    private fun createBuildSearchQueryDataMetadataFilter() =
        arrayOf(
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                emptyList<MetadataSearchFilter>(),
                SELECT_ALL_WITH_TS +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "No right or metadatafilter. One search pair.",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(ZDBIdFilter(listOf("foo & bar"))),
                    SEVariable(PaketSigelFilter(listOf("bar"))),
                ),
                listOf<MetadataSearchFilter>(
                    PublicationYearFilter(fromYear = 2016, toYear = 2022),
                ),
                SELECT_ALL_WITH_TS +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " WHERE ((EXISTS (SELECT 1 FROM unnest(zdb_ids) AS element WHERE (lower(element) ILIKE ?))) AND zdb_ids is not null)" +
                    " AND ((EXISTS (SELECT 1 FROM unnest(paket_sigel) AS element WHERE (element ILIKE ?))) AND paket_sigel is not null)" +
                    " AND (publication_year >= ? AND publication_year <= ? AND publication_year is not null)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "query for publication date filter",
            ),
            arrayOf(
                SEOr(
                    SEPar(
                        SEAnd(
                            SEVariable(ZDBIdFilter(listOf("foo & bar"))),
                            SEVariable(HandleFilter("bar")),
                        ),
                    ),
                    SEVariable(PaketSigelFilter(listOf("bar"))),
                ),
                emptyList<MetadataSearchFilter>(),
                SELECT_ALL_WITH_TS +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " WHERE (((EXISTS (SELECT 1 FROM unnest(zdb_ids) AS element WHERE (lower(element) ILIKE ?))) AND zdb_ids is not null)" +
                    " AND (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null))" +
                    " OR ((EXISTS (SELECT 1 FROM unnest(paket_sigel) AS element WHERE (element ILIKE ?))) AND paket_sigel is not null)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "query for publication date and publication type filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithOnlyMetadataFilter(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildSearchQuery(
                searchExpression,
                metadataSearchFilter,
                emptyList(),
                null,
                hasHandlesToIgnore = false,
            ),
            `is`(expectedWhereClause),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun createDataForBuildSearchQueryBoth() =
        arrayOf(
            arrayOf(
                SEAnd(
                    left = SEVariable(AccessStateFilter(listOf(AccessState.OPEN))),
                    right = SEVariable(AccessStateFilter(listOf(AccessState.RESTRICTED))),
                ),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                false,
                "$SELECT_DISTINCT_ON FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL) AND" +
                    " (((access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "metadata filter search expression on rights",
            ),
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                null,
                false,
                "$SELECT_DISTINCT_ON FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "right filter with exception right filter only",
            ),
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                listOf(
                    PublicationYearFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                NoRightInformationFilter(),
                true,
                SELECT_ALL +
                    " FROM ($SELECT_DISTINCT_ON FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND" +
                    " (LOWER(publication_type) = LOWER(?) OR LOWER(publication_type) = LOWER(?))" +
                    " AND $ALIAS_ITEM_RIGHT.right_id IS NULL)" +
                    " as sub" +
                    " WHERE NOT $COLUMN_METADATA_HANDLE = ANY(?)" +
                    " ORDER BY $COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "all the filters",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithBothFilterTypes(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        hasMetadataItemToIgnore: Boolean,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildSearchQuery(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                hasMetadataItemToIgnore,
            ),
            `is`(expectedWhereClause),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    private fun createBuildSearchCountQueryData() =
        arrayOf(
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*)" +
                    " FROM ($SELECT_ALL_WITH_TS" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC)" +
                    " as countsearch",
                "count query filter with one searchkey",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    fun testBuildSearchCountQuery(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildCountSearchQuery(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                hasHandlesToIgnore = false,
            ),
            `is`(expectedWhereClause),
        )
    }

    @DataProvider(name = DATA_FOR_METASEARCH_QUERY)
    private fun createMetasearchQueryWithFilterNoSearch() =
        arrayOf(
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                "$STATEMENT_GET_METADATA_RANGE ORDER BY $COLUMN_METADATA_HANDLE ASC LIMIT ? OFFSET ?;",
                "metasearch query without filter",
            ),
            arrayOf(
                listOf(PublicationYearFilter(2000, 2019)),
                "$STATEMENT_GET_METADATA_RANGE WHERE $COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? ORDER BY $COLUMN_METADATA_HANDLE ASC LIMIT ? OFFSET ?;",
                "metasearch query with one filter",
            ),
            arrayOf(
                listOf(
                    PublicationYearFilter(2000, 2019),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                STATEMENT_GET_METADATA_RANGE +
                    " WHERE $COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND" +
                    " (publication_type = ? OR publication_type = ?) ORDER BY $COLUMN_METADATA_HANDLE ASC LIMIT ? OFFSET ?;",
                "metasearch query with multiple filter",
            ),
        )

    @DataProvider(name = DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH)
    private fun createBuildCountQueryRightFilterNoSearch() =
        arrayOf(
            arrayOf(
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM (" +
                    SELECT_DISTINCT_ON +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item ON item.$COLUMN_METADATA_HANDLE = $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                    " LEFT JOIN item_right as ir" +
                    " ON item.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    ") as countsearch",
                "search bar filter metadata and right",
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM (" +
                    SELECT_DISTINCT_ON +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item ON item.$COLUMN_METADATA_HANDLE = $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                    " LEFT JOIN item_right as ir" +
                    " ON item.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    ") as countsearch",
                "only right filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH)
    fun testBuildCountQueryBothFilterNoSearch(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildCountSearchQuery(
                null,
                metadataSearchFilter,
                rightSearchFilter,
                null,
                hasHandlesToIgnore = false,
            ),
            `is`(expectedSQLQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY)
    private fun createMetadataQueryFilterNoSearchWithRightFilter() =
        arrayOf(
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                SELECT_DISTINCT_ON +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "query only right filter",
            ),
            arrayOf(
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                SELECT_DISTINCT_ON +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "query with both filters",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY)
    fun testBuildMetadataQueryFilterNoSearchWithRightFilter(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildSearchQuery(
                null,
                metadataSearchFilter,
                rightSearchFilter,
                null,
                hasHandlesToIgnore = false,
            ),
            `is`(expectedSQLQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun createDataForOccurrenceQuery() =
        arrayOf(
            arrayOf(
                COLUMN_METADATA_PUBLICATION_TYPE,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "WITH metadata_with_rights AS (" +
                    " SELECT $ALIAS_ITEM_METADATA.publication_type, $ALIAS_ITEM_RIGHT.right_id, $ALIAS_ITEM_METADATA.handle" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item i ON i.handle = $ALIAS_ITEM_METADATA.handle" +
                    " LEFT JOIN item_right ir ON i.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE $ALIAS_ITEM_METADATA.publication_type IS NOT NULL" +
                    " AND ((ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND (publication_year >= ? AND publication_year <= ? AND publication_year is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?)) AND (((access_state = ? AND access_state is not null)" +
                    " OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)))," +
                    "metadata_unique AS (" +
                    " SELECT handle, publication_type" +
                    " FROM metadata_with_rights" +
                    " GROUP BY handle, publication_type)" +
                    " SELECT mw.publication_type, COUNT(*)" +
                    " FROM metadata_unique mw" +
                    " GROUP BY mw.publication_type;",
                "Publication Type occurence",
            ),
            arrayOf(
                COLUMN_METADATA_ZDB_IDS,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "WITH metadata_with_rights AS (" +
                    " SELECT $ALIAS_ITEM_METADATA.zdb_ids, $ALIAS_ITEM_RIGHT.right_id, $ALIAS_ITEM_METADATA.handle" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item i ON i.handle = $ALIAS_ITEM_METADATA.handle" +
                    " LEFT JOIN item_right ir ON i.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE $ALIAS_ITEM_METADATA.zdb_ids IS NOT NULL AND ((ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND (publication_year >= ? AND publication_year <= ? AND publication_year is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?)) AND (((access_state = ? AND access_state is not null)" +
                    " OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)))," +
                    "metadata_unique AS (" +
                    " SELECT handle, zdb_ids" +
                    " FROM metadata_with_rights" +
                    " GROUP BY handle, zdb_ids)" +
                    " SELECT mw.zdb_ids, COUNT(*)" +
                    " FROM metadata_unique mw" +
                    " GROUP BY mw.zdb_ids;",
                "ZDB ID Series with right filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun testBuildOccurrenceQueryMetadataFacets(
        columnName: String,
        searchExpression: SearchExpression,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedQuery: String,
        reason: String,
    ) {
        val finalQuery =
            buildSearchQueryOccurrence(
                columnName = columnName,
                searchExpression = searchExpression,
                metadataSearchFilters = metadataSearchFilters,
                rightSearchFilters = rightSearchFilters,
                noRightInformationFilter = noRightInformationFilter,
            )
        assertThat(
            reason,
            finalQuery,
            `is`(expectedQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_OCCURRENCE_RIGHT_QUERY)
    fun createDataForOccurrenceRightQuery() =
        arrayOf(
            arrayOf(
                COLUMN_RIGHT_ACCESS_STATE,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "WITH metadata_with_rights AS (" +
                    " SELECT DISTINCT ON ($ALIAS_ITEM_METADATA.handle, $ALIAS_ITEM_RIGHT.access_state) $ALIAS_ITEM_RIGHT.access_state" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item i ON i.handle = $ALIAS_ITEM_METADATA.handle" +
                    " LEFT JOIN item_right ir ON i.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE $ALIAS_ITEM_RIGHT.access_state IS NOT NULL AND" +
                    " ((ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND (publication_year >= ? AND publication_year <= ? AND publication_year is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?)) AND (((access_state = ? AND access_state is not null)" +
                    " OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)))" +
                    " SELECT mw.access_state, COUNT(*)" +
                    " FROM metadata_with_rights mw" +
                    " GROUP BY mw.access_state;",
                "Access state occurrence",
            ),
            arrayOf(
                COLUMN_RIGHT_TEMPLATE_NAME,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "WITH metadata_with_rights AS (" +
                    " SELECT DISTINCT ON ($ALIAS_ITEM_METADATA.handle, $ALIAS_ITEM_RIGHT.template_name) $ALIAS_ITEM_RIGHT.template_name" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item i ON i.handle = $ALIAS_ITEM_METADATA.handle" +
                    " LEFT JOIN item_right ir ON i.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE $ALIAS_ITEM_RIGHT.template_name IS NOT NULL AND" +
                    " ((ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND (publication_year >= ? AND publication_year <= ? AND publication_year is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?)) AND (((access_state = ? AND access_state is not null)" +
                    " OR (access_state = ? AND access_state is not null)) AND $ALIAS_ITEM_RIGHT.right_id IS NOT NULL)))" +
                    " SELECT mw.template_name, COUNT(*)" +
                    " FROM metadata_with_rights mw" +
                    " GROUP BY mw.template_name;",
                "Template name",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_OCCURRENCE_RIGHT_QUERY)
    fun testBuildOccurrenceQueryRightFacets(
        columnName: String,
        searchExpression: SearchExpression,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedQuery: String,
        reason: String,
    ) {
        val finalQuery =
            buildSearchQueryOccurrence(
                columnName = columnName,
                searchExpression = searchExpression,
                metadataSearchFilters = metadataSearchFilters,
                rightSearchFilters = rightSearchFilters,
                noRightInformationFilter = noRightInformationFilter,
            )
        assertThat(
            reason,
            finalQuery,
            `is`(expectedQuery),
        )
    }

    companion object {
        const val DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_SEARCH_COUNT_QUERY = "DATA_FOR_BUILD_SEARCH_COUNT_QUERY"
        const val DATA_FOR_BUILD_OCCURRENCE_QUERY = "DATA_FOR_BUILD_OCCURRENCE_QUERY"
        const val DATA_FOR_BUILD_OCCURRENCE_RIGHT_QUERY = "DATA_FOR_BUILD_OCCURRENCE_RIGHT_QUERY"
        const val DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH =
            "DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH "
        const val DATA_FOR_METASEARCH_QUERY = "DATA_FOR_METASEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY "

        const val DATA_FOR_BUILD_SIGEL_AND_ZDB = "DATA_FOR_BUILD_SIGEL_AND_ZDB"

        const val STATEMENT_GET_METADATA_RANGE =
            "SELECT $COLUMN_METADATA_HANDLE,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle " +
                "FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA"
        const val SELECT_ALL_WITH_TS =
            "SELECT $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE,ppn,title,title_journal,title_series," +
                "$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn,paket_sigel,zdb_ids,issn," +
                "$ALIAS_ITEM_METADATA.created_on,$ALIAS_ITEM_METADATA.last_updated_on,$ALIAS_ITEM_METADATA.created_by,$ALIAS_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "collection_handle,licence_url,sub_community_name,is_part_of_series,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_subcom_name"
        const val SELECT_ALL =
            "SELECT $COLUMN_METADATA_HANDLE,ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band," +
                "publication_type,doi,isbn,paket_sigel,zdb_ids,issn,created_on,last_updated_on," +
                "created_by,last_updated_by,author,collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle,collection_handle,licence_url,sub_community_name," +
                "is_part_of_series,$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED"
        const val SELECT_DISTINCT_ON =
            "SELECT DISTINCT ON ($ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE) $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE," +
                "ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn," +
                "paket_sigel,zdb_ids,issn,$ALIAS_ITEM_METADATA.created_on,$ALIAS_ITEM_METADATA.last_updated_on," +
                "$ALIAS_ITEM_METADATA.created_by,$ALIAS_ITEM_METADATA.last_updated_by,author,collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle,collection_handle,licence_url,sub_community_name," +
                "is_part_of_series,${COLUMN_METADATA_LICENCE_URL_FILTER}," +
                "$COLUMN_METADATA_DELETED,$ALIAS_ITEM_RIGHT.access_state," +
                "$ALIAS_ITEM_RIGHT.licence_contract," +
                "$ALIAS_ITEM_RIGHT.restricted_open_content_licence,$ALIAS_ITEM_RIGHT.zbw_user_agreement," +
                "ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_subcom_name"

        const val LEFT_JOIN_RIGHT =
            "LEFT JOIN item" +
                " ON item.$COLUMN_METADATA_HANDLE = $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                " LEFT JOIN item_right as ir ON item.right_id = $ALIAS_ITEM_RIGHT.right_id"
    }
}
