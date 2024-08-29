package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.CollectionNameFilter
import de.zbw.business.lori.server.CommunityNameFilter
import de.zbw.business.lori.server.HandleFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SEAnd
import de.zbw.business.lori.server.type.SEOr
import de.zbw.business.lori.server.type.SEPar
import de.zbw.business.lori.server.type.SEVariable
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_SUBCOMMUNITY_HANDLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID_JOURNAL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID_SERIES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
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
            connection = dataSource.connection,
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
    fun searchMetadata() {
        // given
        val testZDB = TEST_Metadata.copy(metadataId = "searchZBD", zdbIdJournal = "zbdId")
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
                            PaketSigelFilter(listOf(testZDB.paketSigel!!)),
                        ),
                        SEVariable(ZDBIdFilter(listOf(testZDB.zdbIdJournal!!))),
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
        val testZDB2 = TEST_Metadata.copy(metadataId = "searchZBD2", zdbIdJournal = "zbdId")
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
                    " ORDER BY item_metadata.metadata_id ASC" +
                    " LIMIT ? OFFSET ?",
                "No right or metadatafilter. One search pair.",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(ZDBIdFilter(listOf("foo & bar"))),
                    SEVariable(PaketSigelFilter(listOf("bar"))),
                ),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                null,
                emptyList<MetadataSearchFilter>(),
                SELECT_ALL_WITH_TS +
                    " FROM item_metadata" +
                    " WHERE ((LOWER(zdb_id_journal) = LOWER(?) AND zdb_id_journal is not null)" +
                    " OR (LOWER(zdb_id_series) = LOWER(?) AND zdb_id_series is not null))" +
                    " AND ((LOWER(paket_sigel) = LOWER(?) AND paket_sigel is not null))" +
                    " AND (publication_date >= ? AND publication_date <= ? AND publication_date is not null)" +
                    " ORDER BY item_metadata.metadata_id ASC" +
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
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
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
                    " OR ((LOWER(paket_sigel) = LOWER(?) AND paket_sigel is not null))" +
                    " AND (publication_date >= ? AND publication_date <= ? AND publication_date is not null)" +
                    " AND (LOWER(publication_type) = LOWER(?) OR LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY item_metadata.metadata_id ASC" +
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
                hasMetadataIdsToIgnore = false,
            ),
            `is`(expectedWhereClause),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun createDataForBuildSearchQueryBoth() =
        arrayOf(
            arrayOf(
                SEVariable(AccessStateFilter(listOf(AccessState.OPEN))),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        ),
                    ),
                ),
                emptyList<RightSearchFilter>(),
                null,
                false,
                "$SELECT_ALL_WITH_TS FROM item_metadata" +
                    " $JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null)" +
                    " WHERE (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND" +
                    " (LOWER(publication_type) = LOWER(?) OR LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY item_metadata.metadata_id ASC" +
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
                    " $JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null)" +
                    " WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " ORDER BY item_metadata.metadata_id ASC" +
                    " LIMIT ? OFFSET ?",
                "right filter with exception right filter only",
            ),
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        ),
                    ),
                ),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                NoRightInformationFilter(),
                true,
                SELECT_ALL +
                    " FROM ($SELECT_DISTINCT_ON FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null)" +
                    " WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    " AND (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND" +
                    " (LOWER(publication_type) = LOWER(?) OR LOWER(publication_type) = LOWER(?))" +
                    " AND item_right.right_id IS NULL)" +
                    " as sub" +
                    " WHERE NOT metadata_id = ANY(?)" +
                    " ORDER BY metadata_id ASC" +
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
                    " ORDER BY item_metadata.metadata_id ASC)" +
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
                hasMetadataIdsToIgnore = false,
            ),
            `is`(expectedWhereClause),
        )
    }

    @DataProvider(name = DATA_FOR_METASEARCH_QUERY)
    private fun createMetasearchQueryWithFilterNoSearch() =
        arrayOf(
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                "$STATEMENT_GET_METADATA_RANGE ORDER BY metadata_id ASC LIMIT ? OFFSET ?;",
                "metasearch query without filter",
            ),
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019)),
                "$STATEMENT_GET_METADATA_RANGE WHERE publication_date >= ? AND publication_date <= ? ORDER BY metadata_id ASC LIMIT ? OFFSET ?;",
                "metasearch query with one filter",
            ),
            arrayOf(
                listOf(
                    PublicationDateFilter(2000, 2019),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        ),
                    ),
                ),
                STATEMENT_GET_METADATA_RANGE + " WHERE publication_date >= ? AND publication_date <= ? AND" +
                    " (publication_type = ? OR publication_type = ?) ORDER BY metadata_id ASC LIMIT ? OFFSET ?;",
                "metasearch query with multiple filter",
            ),
        )

    @DataProvider(name = DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH)
    private fun createBuildCountQueryRightFilterNoSearch() =
        arrayOf(
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_community_handle,community_handle,collection_handle,licence_url,sub_community_name,is_part_of_series,zdb_id_series,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) WHERE (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?)) ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "search bar filter metadata and right",
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_community_handle,community_handle,collection_handle,licence_url,sub_community_name,is_part_of_series,zdb_id_series,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) ORDER BY item_metadata.metadata_id ASC) as countsearch",
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
                hasMetadataIdsToIgnore = false,
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
                    " $JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null)" +
                    " ORDER BY item_metadata.metadata_id ASC" +
                    " LIMIT ? OFFSET ?",
                "query only right filter",
            ),
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                SELECT_DISTINCT_ON +
                    " FROM item_metadata" +
                    " $JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null)" +
                    " WHERE (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    " ORDER BY item_metadata.metadata_id ASC" +
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
                hasMetadataIdsToIgnore = false,
            ),
            `is`(expectedSQLQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    private fun createQueryFilterSearchForSigelAndZDB() =
        arrayOf(
            arrayOf(
                SEVariable(CollectionNameFilter("foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "$SELECT_SUB FROM ($SELECT_ALL_PRE_TABLE FROM item_metadata" +
                    " $JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null)" +
                    ") as sub" +
                    " $GROUP_BY_SEARCH_BAR_FILTER;",
                "query with search and right filter",
            ),
            arrayOf(
                null,
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "$SELECT_SUB FROM ($SELECT_ALL_PRE_TABLE FROM item_metadata" +
                    " $JOIN_RIGHT" +
                    " AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) WHERE (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    ") as sub" +
                    " $GROUP_BY_SEARCH_BAR_FILTER;",
                "query with both filters and no searchkey",
            ),
            arrayOf(
                null,
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                emptyList<RightSearchFilter>(),
                "$SELECT_SUB FROM ($SELECT_ALL_PRE_TABLE FROM item_metadata" +
                    " $LEFT_JOIN_RIGHT" +
                    " WHERE (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?))" +
                    ") as sub" +
                    " $GROUP_BY_SEARCH_BAR_FILTER;",
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
            SearchDB.buildSearchQueryForFacets(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                null,
                true,
            ),
            `is`(expectedSQLQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun createDataForOccurrenceQuery() =
        arrayOf(
            arrayOf(
                setOf(
                    PublicationType.ARTICLE.toString(),
                    PublicationType.PROCEEDINGS.toString(),
                    PublicationType.PERIODICAL_PART.toString(),
                ),
                COLUMN_METADATA_PUBLICATION_TYPE,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.publication_type, COUNT(sub.publication_type) FROM(VALUES (?),(?),(?)) as A(publication_type) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id_journal,item_right.access_state,template_name,is_part_of_series,zdb_id_series,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?))) AS sub ON A.publication_type = sub.publication_type  GROUP BY A.publication_type",
            ),
            arrayOf(
                setOf(
                    AccessState.OPEN.toString(),
                    AccessState.CLOSED.toString(),
                    AccessState.RESTRICTED.toString(),
                ),
                DatabaseConnector.COLUMN_RIGHT_ACCESS_STATE,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.access_state, COUNT(sub.access_state) FROM(VALUES (?),(?),(?)) as A(access_state) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id, item_right.access_state) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id_journal,item_right.access_state,is_part_of_series,zdb_id_series,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?))) AS sub ON A.access_state = sub.access_state  GROUP BY A.access_state",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun testBuildOccurrenceQuery(
        values: Set<String>,
        columnName: String,
        searchExpression: SearchExpression,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedQuery: String,
    ) {
        assertThat(
            SearchDB.buildSearchQueryOccurrence(
                SearchDB.createValuesForSql(values.size),
                columnName,
                searchExpression,
                metadataSearchFilters,
                rightSearchFilters,
                noRightInformationFilter,
            ),
            `is`(expectedQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_OCCURRENCE_TEMPLATE_NAME_QUERY)
    fun createDataForOccurrenceTemplateNameQuery() =
        arrayOf(
            arrayOf(
                setOf(
                    AccessState.OPEN.toString(),
                    AccessState.CLOSED.toString(),
                    AccessState.RESTRICTED.toString(),
                ),
                DatabaseConnector.COLUMN_RIGHT_TEMPLATE_NAME,
                SEVariable(CollectionNameFilter("foo")),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.template_name, COUNT(sub.template_name) FROM(VALUES (?),(?),(?)) as A(template_name) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id, item_right.template_name) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id_journal,item_right.access_state,template_name,is_part_of_series,zdb_id_series,ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? AND access_state is not null OR access_state = ? AND access_state is not null) WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND (publication_date >= ? AND publication_date <= ? AND publication_date is not null) AND (LOWER(publication_type) = LOWER(?))) AS sub ON A.template_name = sub.template_name  GROUP BY A.template_name",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_OCCURRENCE_TEMPLATE_NAME_QUERY)
    fun testBuildOccurrenceTemplateNameQuery(
        values: Set<String>,
        columnName: String,
        searchExpression: SearchExpression,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedQuery: String,
    ) {
        assertThat(
            SearchDB.buildSearchQueryOccurrence(
                SearchDB.createValuesForSql(values.size),
                columnName,
                searchExpression,
                metadataSearchFilters,
                rightSearchFilters,
                noRightInformationFilter,
            ),
            `is`(expectedQuery),
        )
    }

    // TODO(CB): Add exception tests

    companion object {
        const val DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_SEARCH_COUNT_QUERY = "DATA_FOR_BUILD_SEARCH_COUNT_QUERY"
        const val DATA_FOR_BUILD_OCCURRENCE_QUERY = "DATA_FOR_BUILD_OCCURRENCE_QUERY"
        const val DATA_FOR_BUILD_OCCURRENCE_TEMPLATE_NAME_QUERY = "DATA_FOR_BUILD_OCCURRENCE_TEMPLATE_NAME_QUERY"
        const val DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH =
            "DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH "
        const val DATA_FOR_METASEARCH_QUERY = "DATA_FOR_METASEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY "

        const val DATA_FOR_BUILD_SIGEL_AND_ZDB = "DATA_FOR_BUILD_SIGEL_AND_ZDB"

        const val STATEMENT_GET_METADATA_RANGE =
            "SELECT metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle " +
                "FROM $TABLE_NAME_ITEM_METADATA"
        const val SELECT_SUB =
            "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id_journal, sub.access_state," +
                " sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.template_name," +
                " sub.is_part_of_series, sub.$COLUMN_METADATA_ZDB_ID_SERIES"
        const val SELECT_ALL_PRE_TABLE =
            "SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series," +
                "publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id_journal,issn," +
                "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "collection_handle,licence_url,sub_community_name,is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence," +
                "item_right.non_standard_open_content_licence_url,item_right.open_content_licence," +
                "item_right.restricted_open_content_licence,item_right.zbw_user_agreement,item_right.template_name," +
                "ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name"
        const val SELECT_ALL_WITH_TS =
            "SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series," +
                "publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id_journal,issn," +
                "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "collection_handle,licence_url,sub_community_name,is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name"
        const val SELECT_ALL =
            "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band," +
                "publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id_journal,issn,created_on,last_updated_on," +
                "created_by,last_updated_by,author,collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle,collection_handle,licence_url,sub_community_name," +
                "is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES"
        const val SELECT_DISTINCT_ON =
            "SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle," +
                "ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                "paket_sigel,zdb_id_journal,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle,collection_handle,licence_url,sub_community_name," +
                "is_part_of_series,$COLUMN_METADATA_ZDB_ID_SERIES,item_right.access_state,item_right.licence_contract," +
                "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement," +
                "ts_collection,ts_community,ts_title,ts_col_hdl,ts_com_hdl,ts_subcom_hdl," +
                "ts_hdl,ts_metadata_id,ts_licence_url,ts_subcom_name"
        const val GROUP_BY_SEARCH_BAR_FILTER =
            "GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id_journal, sub.template_name, sub.is_part_of_series, sub.$COLUMN_METADATA_ZDB_ID_SERIES"

        const val JOIN_RIGHT =
            "LEFT JOIN item" +
                " ON item.metadata_id = item_metadata.metadata_id" +
                " JOIN item_right ON item.right_id = item_right.right_id"

        const val LEFT_JOIN_RIGHT =
            "LEFT JOIN item" +
                " ON item.metadata_id = item_metadata.metadata_id" +
                " LEFT JOIN item_right ON item.right_id = item_right.right_id"
    }
}
