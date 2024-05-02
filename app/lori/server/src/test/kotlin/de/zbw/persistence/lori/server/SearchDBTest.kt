package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SEAnd
import de.zbw.business.lori.server.type.SEOr
import de.zbw.business.lori.server.type.SEPar
import de.zbw.business.lori.server.type.SEVariable
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchKey
import de.zbw.business.lori.server.type.SearchPair
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import de.zbw.persistence.lori.server.MetadataDB.Companion.STATEMENT_SELECT_ALL_METADATA
import de.zbw.persistence.lori.server.SearchDB.Companion.STATEMENT_SELECT_ALL_METADATA_DISTINCT
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
class SearchDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
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
        val testZDB = TEST_Metadata.copy(metadataId = "searchZBD", zdbId = "zbdId")
        dbConnector.metadataDB.insertMetadata(testZDB)

        // when
        val searchPairsZDB = SEVariable(
            SearchPair(
                key = SearchKey.ZDB_ID,
                values = testZDB.zdbId!!
            )
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
        val numberResultZDB = dbConnector.searchDB.countSearchMetadata(
            searchExpression = searchPairsZDB,
            metadataSearchFilter = emptyList(),
            noRightInformationFilter = null,
        )
        // then
        assertThat(resultZDB[0], `is`(testZDB))
        assertThat(numberResultZDB, `is`(1))
        // when
        val searchPairsAll = SEAnd(
            SEVariable(SearchPair(SearchKey.COLLECTION, testZDB.collectionName!!)),
            SEAnd(
                SEVariable(SearchPair(SearchKey.COMMUNITY, testZDB.communityName!!)),
                SEAnd(
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, testZDB.paketSigel!!)),
                    SEVariable(SearchPair(SearchKey.ZDB_ID, testZDB.zdbId!!))
                )
            )
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
        val numberResultAll = dbConnector.searchDB.countSearchMetadata(
            searchExpression = searchPairsAll,
            metadataSearchFilter = emptyList(),
            noRightInformationFilter = null,
        )
        // then
        assertThat(resultAll.toSet(), `is`(setOf(testZDB)))
        assertThat(numberResultAll, `is`(1))

        // Add second metadata with same zbdID
        val testZDB2 = TEST_Metadata.copy(metadataId = "searchZBD2", zdbId = "zbdId")
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
        val numberResultZDB2 = dbConnector.searchDB.countSearchMetadata(
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
            resultZDB2Offset.size, `is`(1)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    private fun createBuildSearchQueryDataMetadataFilter() =
        arrayOf(
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                emptyList<MetadataSearchFilter>(),
                null,
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM item_metadata as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "No right or metadatafilter. One search pair.",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "bar")),
                ),
                emptyList<MetadataSearchFilter>(),
                null,
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM item_metadata as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "query for multiple searchkeys",
            ),
            arrayOf(
                SEVariable(
                    SearchPair(SearchKey.ZDB_ID, "foo & bar"),
                ),
                emptyList<MetadataSearchFilter>(),
                null,
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1))/1 as score FROM item_metadata as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "query for multiple words in one searchkey",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "bar")),
                ),
                emptyList<MetadataSearchFilter>(),
                null,
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM item_metadata as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "query for multiple words in multiple searchkeys"
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "bar")),
                ),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                null,
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "query for publication date filter"
            ),
            arrayOf(
                SEOr(
                    SEPar(
                        SEAnd(
                            SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                            SEVariable(SearchPair(SearchKey.HDL, "baz")),
                        ),
                    ),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "bar")),
                ),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                null,
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_hdl, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/3 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE ((ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null)) OR (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "query for publication date and publication type filter"
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
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun createDataForBuildSearchQueryBoth() =
        arrayOf(
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                null,
                false,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "right filter only",
            ),
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                null,
                false,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "right filter combined with metadatafilter",
            ),
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                emptyList<RightSearchFilter>(),
                NoRightInformationFilter(),
                false,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id LEFT JOIN item_right ON item.right_id = item_right.right_id WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) AND item_right.right_id IS NULL) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "return only items without any right information",
            ),
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                emptyList<RightSearchFilter>(),
                NoRightInformationFilter(),
                false,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id LEFT JOIN item_right ON item.right_id = item_right.right_id WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) AND item_right.right_id IS NULL) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "return only items without any right information",
            ),
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                null,
                false,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC LIMIT ? OFFSET ?",
                "right filter with exception right filter only",
            ),
            arrayOf(
                null,
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                NoRightInformationFilter(),
                true,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url FROM SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id LEFT JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) AND item_right.right_id IS NULL as sub WHERE NOT metadata_id = ANY(?) ORDER BY metadata_id ASC LIMIT ? OFFSET ?",
                "all the filters except search expression plus metadata ids to filter",
            ),
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                NoRightInformationFilter(),
                true,
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id LEFT JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) AND item_right.right_id IS NULL) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) AND NOT item_metadata.metadata_id = ANY(?) ORDER BY score DESC LIMIT ? OFFSET ?",
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
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    private fun createBuildSearchCountQueryData() =
        arrayOf(
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_collection, to_tsquery(?)),1))/1 as score FROM item_metadata as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) ORDER BY score DESC) as countsearch",
                "count query filter with one searchkey",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "foo")),
                ),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM item_metadata as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC) as countsearch",
                "count query filter with two searchkeys",
            ),
            arrayOf(
                SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1))/1 as score FROM item_metadata as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) ORDER BY score DESC) as countsearch",
                "count query filter with multiple words for one key",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "baz")),
                ),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM item_metadata as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC) as countsearch",
                "count query with multiple words for multiple keys",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "baz")),
                ),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC) as countsearch",
                "count query with one filter",
            ),
            arrayOf(
                null,
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM ((SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys but with filter",
            ),
            arrayOf(
                null,
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM ((SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys but with filter",
            ),
            arrayOf(
                null,
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.RESTRICTED, AccessState.CLOSED))),
                null,
                "SELECT COUNT(*) FROM (" +
                    STATEMENT_SELECT_ALL_METADATA_DISTINCT +
                    " FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right" +
                    " ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query only with right search filter",
            ),
            arrayOf(
                null,
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.RESTRICTED, AccessState.CLOSED))),
                null,
                "SELECT COUNT(*) FROM ($STATEMENT_SELECT_ALL_METADATA_DISTINCT" +
                    " FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right" +
                    " ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys but with both filter",
            ),
            arrayOf(
                null,
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                NoRightInformationFilter(),
                "SELECT COUNT(*) FROM ($STATEMENT_SELECT_ALL_METADATA" +
                    " FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " LEFT JOIN item_right" +
                    " ON item.right_id = item_right.right_id" +
                    " WHERE item_right.right_id IS NULL" +
                    " ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys, metadata filter and norightinformation filter",
            ),
            arrayOf(
                null,
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                emptyList<RightSearchFilter>(),
                NoRightInformationFilter(),
                "SELECT COUNT(*) FROM ($STATEMENT_SELECT_ALL_METADATA" +
                    " FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " LEFT JOIN item_right" +
                    " ON item.right_id = item_right.right_id" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)" +
                    " AND item_right.right_id IS NULL" +
                    " ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys, metadata and right filter. Only norightinformation filter",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "baz")),
                ),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                emptyList<RightSearchFilter>(),
                NoRightInformationFilter(),
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,ts_community,ts_collection,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id LEFT JOIN item_right ON item.right_id = item_right.right_id WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) AND item_right.right_id IS NULL) as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC) as countsearch",
                "count query with keys and metadata filter and norightinformation filter",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(SearchPair(SearchKey.ZDB_ID, "foo & bar")),
                    SEVariable(SearchPair(SearchKey.PAKET_SIGEL, "baz")),
                ),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.RESTRICTED, AccessState.CLOSED))),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,(coalesce(ts_rank_cd(ts_zdb_id, to_tsquery(?)),1) + coalesce(ts_rank_cd(ts_sigel, to_tsquery(?)),1))/2 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE (ts_zdb_id @@ to_tsquery(?) AND ts_zdb_id is not null) AND (ts_sigel @@ to_tsquery(?) AND ts_sigel is not null) ORDER BY score DESC) as countsearch",
                "count query with keys and with both filter",
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
            `is`(expectedWhereClause)
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
                        )
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
                "SELECT COUNT(*) FROM" +
                    " (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement," +
                    "ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id," +
                    "ts_licence_url" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "both filter"
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM" +
                    " (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement," +
                    "ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id," +
                    "ts_licence_url" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "only right filter"
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
                "SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal," +
                    "title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn," +
                    "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by," +
                    "item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state," +
                    "item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement," +
                    "ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id," +
                    "ts_licence_url" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC LIMIT ? OFFSET ?",
                "query only right filter",
            ),
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal," +
                    "title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn," +
                    "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by," +
                    "item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state," +
                    "item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement," +
                    "ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id," +
                    "ts_licence_url" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC LIMIT ? OFFSET ?",
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
            `is`(expectedSQLQuery)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    private fun createQueryFilterSearchForSigelAndZDB() =
        arrayOf(
            arrayOf(
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id, sub.access_state, sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.template_name FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,item_right.template_name,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id, sub.template_name;",
                "query with search and right filter",
            ),
            arrayOf(
                null,
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id, sub.access_state, sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.template_name FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,item_right.template_name,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) as sub GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id, sub.template_name;",
                "query with both filters and no searchkey",
            ),
            arrayOf(
                null,
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                emptyList<RightSearchFilter>(),
                "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id, sub.access_state, sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.template_name FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle,licence_url,item_right.access_state,item_right.licence_contract,item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url,item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement,item_right.template_name,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id LEFT JOIN item_right ON item.right_id = item_right.right_id WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) as sub GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url, sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id, sub.template_name;",
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
            `is`(expectedSQLQuery)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun createDataForOccurrenceQuery() =
        arrayOf(
            arrayOf(
                setOf(
                    PublicationType.ARTICLE.toString(),
                    PublicationType.PROCEEDINGS.toString(),
                    PublicationType.PERIODICAL_PART.toString()
                ),
                COLUMN_METADATA_PUBLICATION_TYPE,
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.publication_type, COUNT(sub.publication_type) FROM(VALUES ('ARTICLE'),('PROCEEDINGS'),('PERIODICAL_PART')) as A(publication_type) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id,item_right.access_state,template_name,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) AS sub ON A.publication_type = sub.publication_type  WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) GROUP BY A.publication_type",
            ),
            arrayOf(
                setOf(
                    AccessState.OPEN.toString(),
                    AccessState.CLOSED.toString(),
                    AccessState.RESTRICTED.toString(),
                ),
                DatabaseConnector.COLUMN_RIGHT_ACCESS_STATE,
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.access_state, COUNT(sub.access_state) FROM(VALUES ('OPEN'),('CLOSED'),('RESTRICTED')) as A(access_state) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id, item_right.access_state) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id,item_right.access_state,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) AS sub ON A.access_state = sub.access_state  WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) GROUP BY A.access_state",
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
                SearchDB.createValuesForSql(values),
                columnName,
                searchExpression,
                metadataSearchFilters,
                rightSearchFilters,
                noRightInformationFilter,
            ),
            `is`(expectedQuery)
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
                SEVariable(SearchPair(SearchKey.COLLECTION, "foo")),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.template_name, COUNT(sub.template_name) FROM(VALUES (OPEN),(CLOSED),(RESTRICTED)) as A(template_name) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id, item_right.template_name) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id,item_right.access_state,template_name,ts_collection,ts_community,ts_sigel,ts_title,ts_zdb_id,ts_col_hdl,ts_com_hdl,ts_subcom_hdl,ts_hdl,ts_metadata_id,ts_licence_url FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) AS sub ON A.template_name = sub.template_name  WHERE (ts_collection @@ to_tsquery(?) AND ts_collection is not null) GROUP BY A.template_name",
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
                SearchDB.createGenericValuesForSql(values),
                columnName,
                searchExpression,
                metadataSearchFilters,
                rightSearchFilters,
                noRightInformationFilter,
            ),
            `is`(expectedQuery)
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
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle " +
                "FROM $TABLE_NAME_ITEM_METADATA"
    }
}
