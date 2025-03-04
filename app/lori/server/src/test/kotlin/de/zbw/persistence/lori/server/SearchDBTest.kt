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
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ZDB_ID_JOURNAL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ZDB_ID_SERIES
import de.zbw.persistence.lori.server.SearchDB.Companion.ALIAS_ITEM_RIGHT
import de.zbw.persistence.lori.server.SearchDB.Companion.buildSearchQueryForFacets
import de.zbw.persistence.lori.server.SearchDB.Companion.buildSearchQueryOccurrence
import de.zbw.persistence.lori.server.SearchDB.Companion.buildSearchQueryOccurrenceRight
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
            val testZDB = TEST_Metadata.copy(handle = "searchZBD", zdbIdJournal = "zbdId")
            dbConnector.metadataDB.insertMetadata(testZDB)

            // when
            val searchPairsZDB =
                SEVariable(
                    ZDBIdFilter(listOf(testZDB.zdbIdJournal!!)),
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
                            SEVariable(ZDBIdFilter(listOf(testZDB.zdbIdJournal))),
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
            val testZDB2 = TEST_Metadata.copy(handle = "searchZBD2", zdbIdJournal = "zbdId")
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
                null,
                emptyList<MetadataSearchFilter>(),
                SELECT_ALL_WITH_TS +
                    " FROM item_metadata" +
                    " WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
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
                null,
                emptyList<MetadataSearchFilter>(),
                SELECT_ALL_WITH_TS +
                    " FROM item_metadata" +
                    " WHERE ((LOWER(zdb_id_journal) = LOWER(?) AND zdb_id_journal is not null)" +
                    " OR (LOWER(zdb_id_series) = LOWER(?) AND zdb_id_series is not null))" +
                    " AND (paket_sigel @> ARRAY[?]::text[] AND paket_sigel is not null)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
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
                listOf(
                    PublicationYearFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                null,
                emptyList<MetadataSearchFilter>(),
                SELECT_ALL_WITH_TS +
                    " FROM item_metadata" +
                    " WHERE (((LOWER(zdb_id_journal) = LOWER(?) AND zdb_id_journal is not null)" +
                    " OR (LOWER(zdb_id_series) = LOWER(?) AND zdb_id_series is not null))" +
                    " AND (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null))" +
                    " OR (paket_sigel @> ARRAY[?]::text[] AND paket_sigel is not null)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?) OR LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "query for publication date and publication type filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithOnlyMetadataFilter(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataSearchFilter: List<MetadataSearchFilter>,
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
                "$SELECT_DISTINCT_ON FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL) AND" +
                    " (((access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "metadata filter search expression on rights",
            ),
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                null,
                false,
                "$SELECT_DISTINCT_ON FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
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
                    " FROM ($SELECT_DISTINCT_ON FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND" +
                    " (LOWER(publication_type) = LOWER(?) OR LOWER(publication_type) = LOWER(?))" +
                    " AND ${ALIAS_ITEM_RIGHT}.right_id IS NULL)" +
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
                    " FROM item_metadata" +
                    " WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC)" +
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
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.$COLUMN_METADATA_HANDLE = $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                    " LEFT JOIN item_right as $ALIAS_ITEM_RIGHT" +
                    " ON item.right_id = ${ALIAS_ITEM_RIGHT}.right_id" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    ") as countsearch",
                "search bar filter metadata and right",
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM (" +
                    SELECT_DISTINCT_ON +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.$COLUMN_METADATA_HANDLE = $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                    " LEFT JOIN item_right as $ALIAS_ITEM_RIGHT" +
                    " ON item.right_id = ${ALIAS_ITEM_RIGHT}.right_id" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
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
                    " FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
                    " LIMIT ? OFFSET ?",
                "query only right filter",
            ),
            arrayOf(
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                SELECT_DISTINCT_ON +
                    " FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE ASC" +
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

    @DataProvider(name = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    private fun createQueryFilterSearchForSigelAndZDB() =
        arrayOf(
            arrayOf(
                null,
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "FROM ($SELECT_ALL_PRE_TABLE FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    ") as sub",
                "query with both filters and no searchkey",
            ),
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "FROM ($SELECT_ALL_PRE_TABLE FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND ${ALIAS_ITEM_RIGHT}.right_id IS NOT NULL)" +
                    " AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    ") as sub",
                "query with search and right filter",
            ),
            arrayOf(
                null,
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                emptyList<RightSearchFilter>(),
                "FROM ($SELECT_ALL_PRE_TABLE FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    ") as sub",
                "query with metadata filter only",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    fun testBuildQueryForFacetSearch(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            buildSearchQueryForFacets(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                null,
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
                "SELECT A.publication_type, COUNT(sub.publication_type)" +
                    " FROM(SELECT DISTINCT(sub.publication_type)" +
                    " FROM (SELECT item_metadata.handle,ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band," +
                    "publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn,item_metadata.created_on," +
                    "item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name," +
                    "community_name,storage_date,sub_community_handle,community_handle,collection_handle,licence_url," +
                    "sub_community_name,is_part_of_series,zdb_id_series,licence_url_filter,deleted,o.access_state,o.licence_contract," +
                    "o.non_standard_open_content_licence,o.non_standard_open_content_licence_url,o.open_content_licence," +
                    "o.restricted_open_content_licence,o.zbw_user_agreement,o.template_name,ts_collection,ts_community," +
                    "ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_subcom_name" +
                    " FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null))" +
                    " AND o.right_id IS NOT NULL) AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?))) as sub" +
                    " WHERE sub.publication_type IS NOT NULL" +
                    " GROUP BY sub.publication_type) as A(publication_type)" +
                    " LEFT JOIN (SELECT DISTINCT ON (item_metadata.handle) item_metadata.handle,publication_type,paket_sigel,zdb_id_journal,licence_url_filter,o.access_state,template_name,is_part_of_series,zdb_id_series,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.handle = item_metadata.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND o.right_id IS NOT NULL) AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))) AS sub ON A.publication_type = sub.publication_type  GROUP BY A.publication_type",
                "Publication Type occurence",
            ),
            arrayOf(
                COLUMN_METADATA_ZDB_ID_SERIES,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.zdb_id_series, COUNT(sub.zdb_id_series) FROM(SELECT DISTINCT(sub.zdb_id_series) FROM (SELECT item_metadata.handle,ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_community_handle,community_handle,collection_handle,licence_url,sub_community_name,is_part_of_series,zdb_id_series,licence_url_filter,deleted,o.access_state,o.licence_contract,o.non_standard_open_content_licence,o.non_standard_open_content_licence_url,o.open_content_licence,o.restricted_open_content_licence,o.zbw_user_agreement,o.template_name,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.handle = item_metadata.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND o.right_id IS NOT NULL) AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))) as sub WHERE sub.zdb_id_series IS NOT NULL GROUP BY sub.zdb_id_series) as A(zdb_id_series) LEFT JOIN (SELECT DISTINCT ON (item_metadata.handle) item_metadata.handle,publication_type,paket_sigel,zdb_id_journal,licence_url_filter,o.access_state,template_name,is_part_of_series,zdb_id_series,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.handle = item_metadata.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND o.right_id IS NOT NULL) AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))) AS sub ON A.zdb_id_series = sub.zdb_id_series  GROUP BY A.zdb_id_series",
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
        val facetBaseQuery =
            buildSearchQueryForFacets(
                searchExpression = searchExpression,
                metadataSearchFilters = metadataSearchFilters,
                rightSearchFilters = rightSearchFilters,
                noRightInformationFilter = noRightInformationFilter,
            )

        val finalQuery =
            buildSearchQueryOccurrence(
                columnName = columnName,
                baseQuery = facetBaseQuery,
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
                "SELECT o.access_state, COUNT(o.access_state) FROM (SELECT DISTINCT(sub.handle) FROM (SELECT item_metadata.handle,ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_community_handle,community_handle,collection_handle,licence_url,sub_community_name,is_part_of_series,zdb_id_series,licence_url_filter,deleted,o.access_state,o.licence_contract,o.non_standard_open_content_licence,o.non_standard_open_content_licence_url,o.open_content_licence,o.restricted_open_content_licence,o.zbw_user_agreement,o.template_name,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.handle = item_metadata.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND o.right_id IS NOT NULL) AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))) as sub GROUP BY sub.handle) as A(handle) LEFT JOIN item ON item.handle = A.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE o.access_state IS NOT NULL GROUP BY o.access_state",
                "Access state occurrence",
            ),
            arrayOf(
                COLUMN_RIGHT_TEMPLATE_NAME,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationYearFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDING))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT o.template_name, COUNT(o.template_name) FROM (SELECT DISTINCT(sub.handle) FROM (SELECT item_metadata.handle,ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_community_handle,community_handle,collection_handle,licence_url,sub_community_name,is_part_of_series,zdb_id_series,licence_url_filter,deleted,o.access_state,o.licence_contract,o.non_standard_open_content_licence,o.non_standard_open_content_licence_url,o.open_content_licence,o.restricted_open_content_licence,o.zbw_user_agreement,o.template_name,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.handle = item_metadata.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE (((access_state = ? AND access_state is not null) OR (access_state = ? AND access_state is not null)) AND o.right_id IS NOT NULL) AND (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND ($COLUMN_METADATA_PUBLICATION_YEAR >= ? AND $COLUMN_METADATA_PUBLICATION_YEAR <= ? AND $COLUMN_METADATA_PUBLICATION_YEAR is not null) AND (LOWER(publication_type) = LOWER(?))) as sub GROUP BY sub.handle) as A(handle) LEFT JOIN item ON item.handle = A.handle LEFT JOIN item_right as o ON item.right_id = o.right_id WHERE o.template_name IS NOT NULL GROUP BY o.template_name",
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
        val facetBaseQuery =
            buildSearchQueryForFacets(
                searchExpression = searchExpression,
                metadataSearchFilters = metadataSearchFilters,
                rightSearchFilters = rightSearchFilters,
                noRightInformationFilter = noRightInformationFilter,
            )

        val finalQuery =
            buildSearchQueryOccurrenceRight(
                rightColumn = columnName,
                baseQuery = facetBaseQuery,
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
                "isbn,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle " +
                "FROM $TABLE_NAME_ITEM_METADATA"
        const val SELECT_ALL_PRE_TABLE =
            "SELECT $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE,ppn,title,title_journal,title_series," +
                "$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn," +
                "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "collection_handle,licence_url,sub_community_name,is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED," +
                "${ALIAS_ITEM_RIGHT}.access_state,${ALIAS_ITEM_RIGHT}.licence_contract,${ALIAS_ITEM_RIGHT}.non_standard_open_content_licence," +
                "${ALIAS_ITEM_RIGHT}.non_standard_open_content_licence_url,${ALIAS_ITEM_RIGHT}.open_content_licence," +
                "${ALIAS_ITEM_RIGHT}.restricted_open_content_licence,${ALIAS_ITEM_RIGHT}.zbw_user_agreement,${ALIAS_ITEM_RIGHT}.template_name," +
                "ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_subcom_name"
        const val SELECT_ALL_WITH_TS =
            "SELECT $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE,ppn,title,title_journal,title_series," +
                "$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn," +
                "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "collection_handle,licence_url,sub_community_name,is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_subcom_name"
        const val SELECT_ALL =
            "SELECT $COLUMN_METADATA_HANDLE,ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band," +
                "publication_type,doi,isbn,paket_sigel,zdb_id_journal,issn,created_on,last_updated_on," +
                "created_by,last_updated_by,author,collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle,collection_handle,licence_url,sub_community_name," +
                "is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED"
        const val SELECT_DISTINCT_ON =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE) $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE," +
                "ppn,title,title_journal,title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,publication_type,doi,isbn," +
                "paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle,collection_handle,licence_url,sub_community_name," +
                "is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES,${COLUMN_METADATA_LICENCE_URL_FILTER}," +
                "$COLUMN_METADATA_DELETED,${ALIAS_ITEM_RIGHT}.access_state," +
                "${ALIAS_ITEM_RIGHT}.licence_contract," +
                "${ALIAS_ITEM_RIGHT}.non_standard_open_content_licence,${ALIAS_ITEM_RIGHT}.non_standard_open_content_licence_url," +
                "${ALIAS_ITEM_RIGHT}.open_content_licence,${ALIAS_ITEM_RIGHT}.restricted_open_content_licence,${ALIAS_ITEM_RIGHT}.zbw_user_agreement," +
                "ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_subcom_name"

        const val LEFT_JOIN_RIGHT =
            "LEFT JOIN item" +
                " ON item.$COLUMN_METADATA_HANDLE = $TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                " LEFT JOIN item_right as $ALIAS_ITEM_RIGHT ON item.right_id = ${ALIAS_ITEM_RIGHT}.right_id"
    }
}
