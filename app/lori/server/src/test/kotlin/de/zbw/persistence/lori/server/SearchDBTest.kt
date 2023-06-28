package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.SearchKey
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType
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
        val searchTermsZDB = mapOf(Pair(SearchKey.ZDB_ID, listOf(testZDB.zdbId!!)))
        val resultZDB =
            dbConnector.searchDB.searchMetadata(
                searchTerms = searchTermsZDB,
                limit = 5,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
                noRightInformationFilter = null,
            )
        val numberResultZDB = dbConnector.searchDB.countSearchMetadata(
            searchTerms = searchTermsZDB,
            metadataSearchFilter = emptyList(),
            noRightInformationFilter = null,
        )
        // then
        assertThat(resultZDB[0], `is`(testZDB))
        assertThat(numberResultZDB, `is`(1))
        // when
        val searchTermsAll = mapOf(
            Pair(SearchKey.COLLECTION, listOf(testZDB.collectionName!!)),
            Pair(SearchKey.COMMUNITY, listOf(testZDB.communityName!!)),
            Pair(SearchKey.PAKET_SIGEL, listOf(testZDB.paketSigel!!)),
            Pair(SearchKey.ZDB_ID, listOf(testZDB.zdbId!!)),
        )
        val resultAll =
            dbConnector.searchDB.searchMetadata(
                searchTerms = searchTermsAll,
                limit = 5,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
                noRightInformationFilter = null,
            )
        val numberResultAll = dbConnector.searchDB.countSearchMetadata(
            searchTerms = searchTermsAll,
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
            dbConnector.searchDB.searchMetadata(
                searchTerms = searchTermsZDB,
                limit = 5,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
                noRightInformationFilter = null,
            )
        val numberResultZDB2 = dbConnector.searchDB.countSearchMetadata(
            searchTerms = searchTermsAll,
            metadataSearchFilter = emptyList(),
            noRightInformationFilter = null,
        )
        // then
        assertThat(resultZBD2.toSet(), `is`(setOf(testZDB, testZDB2)))
        assertThat(numberResultZDB2, `is`(2))

        // when
        val resultZDB2Offset =
            dbConnector.searchDB.searchMetadata(
                searchTerms = searchTermsZDB,
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
    private fun createBuildSearchQueryData() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_col,1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,collection_name <-> ? as dist_col FROM item_metadata) as sub WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for one string in collection",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo"), SearchKey.PAKET_SIGEL to listOf("bar")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for multiple searchkeys",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for multiple words in one searchkey",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for multiple words in multiple searchkeys"
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for publication date filter"
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for publication date and publication type filter"
            )
        )

    @Test(dataProvider = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithOnlyMetadataFilter(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildSearchQuery(
                searchKeys,
                metadataSearchFilter,
                emptyList(),
                null,
            ),
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun createDataForBuildSearchQueryBoth() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                null,
                "${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES},(coalesce(sub.dist_col,1))/1 as score" +
                    " FROM ($STATEMENT_SELECT_ALL_METADATA_DISTINCT,collection_name <-> ? as dist_col" +
                    " FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right" +
                    " ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub" +
                    " WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "right filter only",
            ),
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
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
                "${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES},(coalesce(sub.dist_col,1))/1 as score" +
                    " FROM ($STATEMENT_SELECT_ALL_METADATA_DISTINCT,collection_name <-> ? as dist_col" +
                    " FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right" +
                    " ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ?" +
                    " AND (publication_type = ? OR publication_type = ?)) as sub" +
                    " WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "right filter combined with metadatafilter",
            ),
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
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
                "${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES},(coalesce(sub.dist_col,1))/1 as score" +
                    " FROM ($STATEMENT_SELECT_ALL_METADATA,collection_name <-> ? as dist_col" +
                    " FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " LEFT JOIN item_right" +
                    " ON item.right_id = item_right.right_id" +
                    " WHERE publication_date >= ? AND publication_date <= ?" +
                    " AND (publication_type = ? OR publication_type = ?)" +
                    " AND item_right.right_id IS NULL) as sub" +
                    " WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "return only items without any right information",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithBothFilterTypes(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildSearchQuery(
                searchKeys,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
            ),
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    private fun createBuildSearchCountQueryData() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM" +
                    " (${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES},(coalesce(sub.dist_col,1))/1 as score" +
                    " FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,collection_name <-> ? as dist_col FROM item_metadata) as sub WHERE sub.dist_col < 0.9 ORDER BY score) as countsearch",
                "count query filter with one searchkey",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo"), SearchKey.PAKET_SIGEL to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES},(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as countsearch",
                "count query filter with two searchkeys",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 ORDER BY score) as countsearch",
                "count query filter with multiple words for one key",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as countsearch",
                "count query with multiple words for multiple keys",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as countsearch",
                "count query with one filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                emptyList<RightSearchFilter>(),
                null,
                "SELECT COUNT(*) FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys but with filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
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
                "SELECT COUNT(*) FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) ORDER BY item_metadata.metadata_id ASC) as countsearch",
                "count query without keys but with filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
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
                emptyMap<SearchKey, List<String>>(),
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
                emptyMap<SearchKey, List<String>>(),
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
                emptyMap<SearchKey, List<String>>(),
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
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
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
                "SELECT COUNT(*) FROM (${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES}," +
                    "(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score" +
                    " FROM ($STATEMENT_SELECT_ALL_METADATA,zdb_id <-> ? as dist_zdb," +
                    "paket_sigel <-> ? as dist_sig FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " LEFT JOIN item_right ON item.right_id = item_right.right_id" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)" +
                    " AND item_right.right_id IS NULL)" +
                    " as sub" +
                    " WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as countsearch",
                "count query with keys and metadata filter and norightinformation filter",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
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
                "SELECT COUNT(*) FROM (${SearchDB.STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES}," +
                    "(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score" +
                    " FROM ($STATEMENT_SELECT_ALL_METADATA_DISTINCT,zdb_id <-> ? as dist_zdb," +
                    "paket_sigel <-> ? as dist_sig FROM item_metadata" +
                    " LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?))" +
                    " as sub" +
                    " WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as countsearch",
                "count query with keys and with both filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    fun testBuildSearchCountQuery(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildCountSearchQuery(
                searchKeys,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
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
                    "storage_date,item_right.access_state,item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement" +
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
                    "storage_date,item_right.access_state,item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement" +
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
                emptyMap(),
                metadataSearchFilter,
                rightSearchFilter,
                null,
            ),
            `is`(expectedSQLQuery)
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
                    "item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state," +
                    "item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement" +
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
                    "item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state," +
                    "item_right.licence_contract," +
                    "item_right.non_standard_open_content_licence,item_right.non_standard_open_content_licence_url," +
                    "item_right.open_content_licence,item_right.restricted_open_content_licence,item_right.zbw_user_agreement" +
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
                emptyMap(),
                metadataSearchFilter,
                rightSearchFilter,
                null,
            ),
            `is`(expectedSQLQuery)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    private fun createQueryFilterSearchForSigelAndZDB() =
        arrayOf(
            arrayOf(
                listOf(SearchKey.COLLECTION to "foo").toMap(),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id, sub.access_state," +
                    " sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                    " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement" +
                    " FROM (SELECT item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state," +
                    "item_right.licence_contract,item_right.non_standard_open_content_licence," +
                    "item_right.non_standard_open_content_licence_url,item_right.open_content_licence," +
                    "item_right.restricted_open_content_licence,item_right.zbw_user_agreement," +
                    "collection_name <-> ? as dist_col FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub" +
                    " WHERE sub.dist_col < 0.9" +
                    " GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type," +
                    " sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                    " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id;",
                "query with search and right filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id, sub.access_state," +
                    " sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                    " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement" +
                    " FROM (SELECT item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state," +
                    "item_right.licence_contract,item_right.non_standard_open_content_licence," +
                    "item_right.non_standard_open_content_licence_url,item_right.open_content_licence," +
                    "item_right.restricted_open_content_licence,item_right.zbw_user_agreement" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) as sub" +
                    " GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type," +
                    " sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                    " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id;",
                "query with both filters and no searchkey",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                emptyList<RightSearchFilter>(),
                "SELECT sub.paket_sigel, sub.publication_type, sub.zdb_id, sub.access_state," +
                    " sub.licence_contract, sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                    " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement" +
                    " FROM (SELECT item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state," +
                    "item_right.licence_contract,item_right.non_standard_open_content_licence," +
                    "item_right.non_standard_open_content_licence_url,item_right.open_content_licence," +
                    "item_right.restricted_open_content_licence,item_right.zbw_user_agreement" +
                    " FROM item_metadata LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " LEFT JOIN item_right ON item.right_id = item_right.right_id" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) as sub" +
                    " GROUP BY sub.access_state, sub.licence_contract, sub.paket_sigel, sub.publication_type," +
                    " sub.non_standard_open_content_licence, sub.non_standard_open_content_licence_url," +
                    " sub.restricted_open_content_licence, sub.open_content_licence, sub.zbw_user_agreement, sub.zdb_id;",
                "query with metadata filter only",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    fun testBuildQueryForFacetSearch(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            SearchDB.buildSearchQueryForFacets(
                searchKeys,
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
                listOf(SearchKey.COLLECTION to "foo").toMap(),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.publication_type, COUNT(sub.publication_type) FROM(VALUES ('ARTICLE'),('PROCEEDINGS'),('PERIODICAL_PART')) as A(publication_type) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id,item_right.access_state,collection_name <-> ? as dist_col FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) AS sub ON A.publication_type = sub.publication_type  WHERE sub.dist_col < 0.9 GROUP BY A.publication_type"
            ),
            arrayOf(
                setOf(
                    AccessState.OPEN.toString(),
                    AccessState.CLOSED.toString(),
                    AccessState.RESTRICTED.toString(),
                ),
                DatabaseConnector.COLUMN_RIGHT_ACCESS_STATE,
                listOf(SearchKey.COLLECTION to "foo").toMap(),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                null,
                "SELECT A.access_state, COUNT(sub.access_state) FROM(VALUES ('OPEN'),('CLOSED'),('RESTRICTED')) as A(access_state) LEFT JOIN (SELECT DISTINCT ON (item_metadata.metadata_id, item_right.access_state) item_metadata.metadata_id,publication_type,paket_sigel,zdb_id,item_right.access_state,collection_name <-> ? as dist_col FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) AS sub ON A.access_state = sub.access_state  WHERE sub.dist_col < 0.9 GROUP BY A.access_state"
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun testBuildOccurrenceQuery(
        values: Set<String>,
        columnName: String,
        searchKeyMap: Map<SearchKey, List<String>>,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedQuery: String,
    ) {
        assertThat(
            SearchDB.buildSearchQueryOccurrence(
                SearchDB.createValuesForSql(values),
                columnName,
                searchKeyMap,
                metadataSearchFilters,
                rightSearchFilters,
                noRightInformationFilter,
            ),
            `is`(expectedQuery)
        )
    }

    companion object {
        const val DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_SEARCH_COUNT_QUERY = "DATA_FOR_BUILD_SEARCH_COUNT_QUERY"
        const val DATA_FOR_BUILD_OCCURRENCE_QUERY = "DATA_FOR_BUILD_OCCURENCE_QUERY"
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
                "author,collection_name,community_name,storage_date " +
                "FROM $TABLE_NAME_ITEM_METADATA"
    }
}
