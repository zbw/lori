package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.utils.SearchExpressionResolution
import de.zbw.business.lori.server.utils.SearchExpressionResolution.resolveSearchExpression
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ACCESS_STATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_END_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_LICENCE_CONTRACT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_START_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_TEMPLATE_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ZBW_USER_AGREEMENT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_RIGHT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_AUTHOR
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_BAND
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COLLECTION_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COLLECTION_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_COMMUNITY_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_CREATED_BY
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_CREATED_ON
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_DELETED
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_DOI
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ISBN
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ISSN
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_IS_PART_OF_SERIES
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LAST_UPDATED_BY
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LAST_UPDATED_ON
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LICENCE_URL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_LICENCE_URL_FILTER
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PPN
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PUBLICATION_YEAR
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_STORAGE_DATE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_SUBCOMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_SUBCOMMUNITY_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_TITLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_TITLE_JOURNAL
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_TITLE_SERIES
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_ZDB_IDS
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_COLLECTION
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_COLLECTION_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_COMMUNITY
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_COMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_SUBCOMMUNITY_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_SUBCOMMUNITY_NAME
import de.zbw.persistence.lori.server.MetadataDB.Companion.TS_TITLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.extractMetadataRS
import de.zbw.persistence.lori.server.RightDB.Companion.COLUMN_HAS_LEGAL_RISK
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.sql.ResultSet

/**
 * Execute SQL queries strongly related to search.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SearchDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    suspend fun searchForFacets(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): FacetTransientSet =
        coroutineScope {
            val paketSigelFacet: Deferred<Map<List<String>, Int>> =
                async {
                    searchOccurrences(
                        occurrenceForColumn = COLUMN_METADATA_PAKET_SIGEL,
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            (rs.getArray(1)?.array as? Array<out Any?>)?.filterIsInstance<String>() ?: emptyList(),
                            rs.getInt(2),
                        )
                    }
                }

            val accessStateFacet: Deferred<Map<AccessState, Int>> =
                async {
                    searchOccurrences(
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        occurrenceForColumn = COLUMN_RIGHT_ACCESS_STATE,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getString(1),
                            rs.getInt(2),
                        )
                    }.toList().associate { Pair(AccessState.valueOf(it.first), it.second) }
                }

            val publicationTypeFacet =
                async {
                    searchOccurrences(
                        occurrenceForColumn = COLUMN_METADATA_PUBLICATION_TYPE,
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getString(1),
                            rs.getInt(2),
                        )
                    }.toList().associate { Pair(PublicationType.valueOf(it.first), it.second) }.toMutableMap()
                }

            val zdbIDsJournalFacet =
                async {
                    searchOccurrences(
                        occurrenceForColumn = COLUMN_METADATA_ZDB_IDS,
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            (rs.getArray(1)?.array as? Array<out Any?>)?.filterIsInstance<String>() ?: emptyList(),
                            rs.getInt(2),
                        )
                    }
                }

            val isPartOfSeriesFacet =
                async {
                    searchOccurrences(
                        occurrenceForColumn = COLUMN_METADATA_IS_PART_OF_SERIES,
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            (rs.getArray(1)?.array as? Array<out Any?>)?.filterIsInstance<String>() ?: emptyList(),
                            rs.getInt(2),
                        )
                    }
                }

            val templateIdFacet =
                async {
                    searchOccurrences(
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        occurrenceForColumn = COLUMN_RIGHT_TEMPLATE_NAME,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getString(1),
                            rs.getInt(2),
                        )
                    }
                }

            val licenceURLFacet =
                async {
                    searchOccurrences(
                        occurrenceForColumn = COLUMN_METADATA_LICENCE_URL_FILTER,
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getString(1),
                            rs.getInt(2),
                        )
                    }
                }

            val licenceContractFacet: Deferred<Map<String?, Int>> =
                async {
                    searchOccurrences(
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        occurrenceForColumn = COLUMN_RIGHT_LICENCE_CONTRACT,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getString(1),
                            rs.getInt(2),
                        )
                    }
                }

            val zbwUserAgreementFacet =
                async {
                    searchOccurrences(
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        occurrenceForColumn = COLUMN_RIGHT_ZBW_USER_AGREEMENT,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getBoolean(1),
                            rs.getInt(2),
                        )
                    }
                }

            val ccLicenceNoRestrictionFacet =
                async {
                    searchOccurrences(
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters =
                            rightSearchFilter +
                                listOf(FormalRuleFilter(listOf(FormalRule.CC_LICENCE_NO_RESTRICTION))),
                        occurrenceForColumn = COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getBoolean(1),
                            rs.getInt(2),
                        )
                    }
                }

            val legalRiskFacet: Deferred<Map<Boolean, Int>> =
                async {
                    searchOccurrences(
                        searchExpression = searchExpression,
                        metadataSearchFilters = metadataSearchFilter,
                        rightSearchFilters = rightSearchFilter,
                        occurrenceForColumn = COLUMN_HAS_LEGAL_RISK,
                        noRightInformationFilter = noRightInformationFilter,
                    ) { rs ->
                        Pair(
                            rs.getBoolean(1),
                            rs.getInt(2),
                        )
                    }
                }

            return@coroutineScope FacetTransientSet(
                paketSigels = paketSigelFacet.await(),
                publicationType = publicationTypeFacet.await(),
                zdbIds = zdbIDsJournalFacet.await(),
                isPartOfSeries = isPartOfSeriesFacet.await(),
                licenceUrls = licenceURLFacet.await(),
                accessState = accessStateFacet.await(),
                templateIdToOccurence = templateIdFacet.await(),
                licenceContracts =
                    licenceContractFacet
                        .await()
                        .values
                        .sum(),
                noLegalRisks =
                    legalRiskFacet
                        .await()
                        .getOrDefault(false, 0),
                zbwUserAgreements =
                    zbwUserAgreementFacet
                        .await()
                        .getOrDefault(true, 0),
                ccLicenceNoRestrictions =
                    ccLicenceNoRestrictionFacet
                        .await()
                        .getOrDefault(false, 0),
            )
        }

    private suspend fun <K, V> searchOccurrences(
        occurrenceForColumn: String,
        searchExpression: SearchExpression?,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        operation: (rs: ResultSet) -> Pair<K, V>,
    ): Map<K, V> =
        coroutineScope {
            return@coroutineScope connectionPool.useConnection("searchOccurrences") { connection ->
                val prepStmt =
                    connection
                        .prepareStatement(
                            buildSearchQueryOccurrence(
                                columnName = occurrenceForColumn,
                                searchExpression = searchExpression,
                                metadataSearchFilters = metadataSearchFilters,
                                rightSearchFilters = rightSearchFilters,
                                noRightInformationFilter = noRightInformationFilter,
                            ),
                        ).apply {
                            var counter = 1
                            val searchPairs =
                                searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                                    ?: emptyList()
                            searchPairs.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            metadataSearchFilters.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            rightSearchFilters.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                        }
                val span = tracer.spanBuilder("searchForOccurrence").startSpan()
                val rs =
                    try {
                        span.makeCurrent()
                        runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
                    } finally {
                        span.end()
                    }

                val received: List<Pair<K, V>> =
                    generateSequence {
                        if (rs.next()) {
                            operation(rs)
                        } else {
                            null
                        }
                    }.takeWhile { true }.toList()
                return@useConnection received.toMap()
                // Make sure that every given value still exist in the resulting map. That should
                // never be necessary but in case of any expected value has no counts, the frontend
                // will still display it.
                // return@useConnection addDefaultEntriesToMap(occurrenceMap, givenValues, 0) { a, b -> max(a, b) }
            }
        }

    /**
     * Search related queries.
     */
    suspend fun countSearchMetadata(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
        noRightInformationFilter: NoRightInformationFilter?,
    ): Int =
        connectionPool
            .useConnection("countSearchMetadata") { connection ->
                val prepStmt =
                    connection
                        .prepareStatement(
                            buildCountSearchQuery(
                                searchExpression = searchExpression,
                                metadataSearchFilter = metadataSearchFilter,
                                rightSearchFilter = rightSearchFilter,
                                noRightInformationFilter = noRightInformationFilter,
                                hasHandlesToIgnore = false,
                            ),
                        ).apply {
                            var counter = 1
                            val searchPairs =
                                searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
                            rightSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            searchPairs.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            metadataSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                        }
                val span = tracer.spanBuilder("countMetadataSearch").startSpan()
                val rs =
                    try {
                        span.makeCurrent()
                        runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
                    } finally {
                        span.end()
                    }
                if (rs.next()) {
                    return@useConnection rs.getInt(1)
                } else {
                    throw IllegalStateException("No count found.")
                }
            }

    private suspend fun searchMetadata(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        handlesToIgnore: List<String>,
    ): List<ItemMetadata> =
        connectionPool
            .useConnection("searchMetadata") { connection ->
                val prepStmt =
                    connection
                        .prepareStatement(
                            buildSearchQuery(
                                searchExpression = searchExpression,
                                metadataSearchFilters = metadataSearchFilter,
                                rightSearchFilters = rightSearchFilter,
                                noRightInformationFilter = noRightInformationFilter,
                                hasHandlesToIgnore = handlesToIgnore.isNotEmpty(),
                                withLimit = limit != null,
                                withOffset = offset != null,
                            ),
                        ).apply {
                            var counter = 1
                            val searchPairs =
                                searchExpression
                                    ?.let { SearchExpressionResolution.getSearchPairs(it) }
                                    ?: emptyList()
                            rightSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            searchPairs.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            metadataSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            if (handlesToIgnore.isNotEmpty()) {
                                this.setArray(
                                    counter++,
                                    connection.createArrayOf("text", handlesToIgnore.toTypedArray()),
                                )
                            }
                            if (limit != null) {
                                this.setInt(counter++, limit)
                            }
                            if (offset != null) {
                                this.setInt(counter++, offset)
                            }
                        }
                val span = tracer.spanBuilder("searchMetadataWithRightsFilter").startSpan()
                return@useConnection try {
                    span.makeCurrent()
                    val rs = runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
                    generateSequence {
                        if (rs.next()) {
                            extractMetadataRS(rs)
                        } else {
                            null
                        }
                    }.takeWhile { true }.toList()
                } finally {
                    span.end()
                }
            }

    suspend fun searchMetadataItems(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        handlesToIgnore: List<String> = emptyList(),
    ): List<ItemMetadata> =
        searchMetadata(
            searchExpression = searchExpression,
            limit = limit,
            offset = offset,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            handlesToIgnore = handlesToIgnore,
        )

    suspend fun searchForHandles(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        handlesToIgnore: List<String>,
    ): List<String> {
        val rs: List<ItemMetadata> =
            searchMetadata(
                searchExpression = searchExpression,
                limit = limit,
                offset = offset,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                handlesToIgnore = handlesToIgnore,
            )
        return rs.map { it.handle }
    }

    companion object {
        const val ALIAS_ITEM_RIGHT = "ir"
        const val ALIAS_ITEM_METADATA = "im"
        const val SUBQUERY_NAME = "sub"

        const val STATEMENT_SELECT_ALL_METADATA =
            "SELECT $ALIAS_ITEM_METADATA.handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_YEAR,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,issn," +
                "$ALIAS_ITEM_METADATA.created_on,$ALIAS_ITEM_METADATA.last_updated_on," +
                "$ALIAS_ITEM_METADATA.created_by,$ALIAS_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle," +
                "licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED," +
                "$TS_COLLECTION,$TS_COMMUNITY,$TS_TITLE,$TS_COLLECTION_HANDLE," +
                "$TS_COMMUNITY_HANDLE,$TS_SUBCOMMUNITY_HANDLE,$TS_HANDLE,$TS_SUBCOMMUNITY_NAME"

        const val STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES =
            "SELECT $COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_YEAR," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,$COLUMN_METADATA_ISSN," +
                "$COLUMN_METADATA_CREATED_ON,$COLUMN_METADATA_LAST_UPDATED_ON,$COLUMN_METADATA_CREATED_BY," +
                "$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED"

        const val STATEMENT_SELECT_ALL_METADATA_DISTINCT =
            "SELECT DISTINCT ON ($ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE) $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE," +
                "$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_YEAR," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,$COLUMN_METADATA_ISSN," +
                "$ALIAS_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$ALIAS_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON," +
                "$ALIAS_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$ALIAS_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_TITLE}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_NAME}"

        internal fun buildSearchQuery(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            hasHandlesToIgnore: Boolean,
            withLimit: Boolean = true,
            withOffset: Boolean = true,
        ): String {
            val limit =
                if (withLimit) {
                    " LIMIT ?"
                } else {
                    ""
                }
            val offset =
                if (withOffset) {
                    " OFFSET ?"
                } else {
                    ""
                }

            val subquery =
                if (
                    searchExpression == null &&
                    rightSearchFilters.isEmpty() &&
                    noRightInformationFilter == null &&
                    metadataSearchFilters.isEmpty()
                ) {
                    buildSearchQuerySelect(hasRightSearchFilter = false) + " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            null,
                            metadataSearchFilters,
                        )
                } else if (
                    rightSearchFilters.isEmpty() &&
                    noRightInformationFilter == null &&
                    SearchExpressionResolution.hasRightQueries(searchExpression).not()
                ) {
                    buildSearchQuerySelect(hasRightSearchFilter = false) +
                        " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            searchExpression,
                            metadataSearchFilters,
                        )
                } else {
                    buildSearchQuerySelect(
                        hasRightSearchFilter =
                            rightSearchFilters.isNotEmpty() ||
                                SearchExpressionResolution.hasRightQueries(
                                    searchExpression,
                                ),
                    ) +
                        " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            searchExpression,
                            metadataSearchFilters,
                            rightSearchFilters,
                            noRightInformationFilter,
                        )
                }

            return if (hasHandlesToIgnore) {
                val filterHandles = "WHERE NOT $COLUMN_METADATA_HANDLE = ANY(?)"
                STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES +
                    " FROM ($subquery) as $SUBQUERY_NAME" +
                    " $filterHandles ORDER BY $COLUMN_METADATA_HANDLE ASC$limit$offset"
            } else {
                "$subquery ORDER BY ${ALIAS_ITEM_METADATA}.$COLUMN_METADATA_HANDLE ASC$limit$offset"
            }
        }

        fun buildCountSearchQuery(
            searchExpression: SearchExpression?,
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            hasHandlesToIgnore: Boolean,
        ): String =
            "SELECT COUNT(*) FROM (" +
                buildSearchQuery(
                    searchExpression,
                    metadataSearchFilter,
                    rightSearchFilter,
                    noRightInformationFilter,
                    hasHandlesToIgnore,
                    false,
                    false,
                ) + ") as countsearch"

        fun buildSearchQueryOccurrence(
            columnName: String,
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val isRightColumn =
                columnName == COLUMN_RIGHT_TEMPLATE_NAME ||
                    columnName == COLUMN_RIGHT_ZBW_USER_AGREEMENT ||
                    columnName == COLUMN_RIGHT_ACCESS_STATE ||
                    columnName == COLUMN_RIGHT_END_DATE ||
                    columnName == COLUMN_RIGHT_LICENCE_CONTRACT ||
                    columnName == COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE ||
                    columnName == COLUMN_HAS_LEGAL_RISK ||
                    columnName == COLUMN_RIGHT_START_DATE

            return if (isRightColumn) {
                buildSearchQueryOccurrenceRight(
                    columnName,
                    searchExpression,
                    metadataSearchFilters,
                    rightSearchFilters,
                    noRightInformationFilter,
                )
            } else {
                buildSearchQueryOccurrenceMetadata(
                    columnName,
                    searchExpression,
                    metadataSearchFilters,
                    rightSearchFilters,
                    noRightInformationFilter,
                )
            }
        }

        private fun buildSearchQueryOccurrenceMetadata(
            columnName: String,
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val selectInWith =
                "SELECT $ALIAS_ITEM_METADATA.$columnName, $ALIAS_ITEM_RIGHT.$COLUMN_RIGHT_ID," +
                    " $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE"

            val subsequentSelect =
                "SELECT mw.$columnName, COUNT(*)" +
                    " FROM metadata_unique mw" +
                    " GROUP BY mw.$columnName;"

            val whereClause =
                buildWhereClause(
                    searchExpression = searchExpression,
                    metadataSearchFilter = metadataSearchFilters,
                    rightSearchFilter = rightSearchFilters,
                    noRightInformationFilter = noRightInformationFilter,
                ).takeIf { it.isNotBlank() } ?: true

            val withStatement =
                "WITH metadata_with_rights AS (" +
                    " $selectInWith" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN $TABLE_NAME_ITEM i ON i.$COLUMN_METADATA_HANDLE = $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                    " LEFT JOIN $TABLE_NAME_ITEM_RIGHT $ALIAS_ITEM_RIGHT ON i.$COLUMN_RIGHT_ID = $ALIAS_ITEM_RIGHT.$COLUMN_RIGHT_ID" +
                    " WHERE $ALIAS_ITEM_METADATA.$columnName IS NOT NULL AND ($whereClause))"

            val metadataUnique =
                "metadata_unique AS (" +
                    " SELECT" +
                    " $COLUMN_METADATA_HANDLE," +
                    " $columnName" +
                    " FROM" +
                    " metadata_with_rights" +
                    " GROUP BY" +
                    " $COLUMN_METADATA_HANDLE, $columnName" +
                    ")"

            return "$withStatement,$metadataUnique $subsequentSelect"
        }

        private fun buildSearchQueryOccurrenceRight(
            columnName: String,
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val selectInWith =
                "SELECT DISTINCT ON ($ALIAS_ITEM_METADATA.handle, $ALIAS_ITEM_RIGHT.$columnName)" +
                    " $ALIAS_ITEM_RIGHT.$columnName"

            val count = "COUNT(*)"
            val subsequentSelect =
                "SELECT mw.$columnName, $count" +
                    " FROM metadata_with_rights mw" +
                    " GROUP BY mw.$columnName;"

            val whereClause =
                buildWhereClause(
                    searchExpression = searchExpression,
                    metadataSearchFilter = metadataSearchFilters,
                    rightSearchFilter = rightSearchFilters,
                    noRightInformationFilter = noRightInformationFilter,
                ).takeIf { it.isNotBlank() } ?: true

            val withStatement =
                "WITH metadata_with_rights AS (" +
                    " $selectInWith" +
                    " FROM $TABLE_NAME_ITEM_METADATA $ALIAS_ITEM_METADATA" +
                    " LEFT JOIN item i ON i.handle = $ALIAS_ITEM_METADATA.handle" +
                    " LEFT JOIN item_right $ALIAS_ITEM_RIGHT ON i.right_id = $ALIAS_ITEM_RIGHT.right_id" +
                    " WHERE $ALIAS_ITEM_RIGHT.$columnName IS NOT NULL AND ($whereClause))"

            return "$withStatement $subsequentSelect"
        }

        private fun buildSearchQueryHelper(
            searchExpression: SearchExpression?,
            metadataSearchFilter: List<MetadataSearchFilter>,
        ): String {
            val searchExpressionFilters: String =
                searchExpression?.let {
                    resolveSearchExpression(it)
                } ?: ""
            val metadataFilters =
                metadataSearchFilter
                    .joinToString(separator = " AND ") { f ->
                        f.toWhereClause()
                    }.takeIf { it.isNotBlank() }
                    ?: ""
            return listOf(
                searchExpressionFilters,
                metadataFilters,
            ).filter { it.isNotBlank() }
                .joinToString(separator = " AND ")
                .takeIf { it.isNotBlank() }
                ?.let { " WHERE $it" }
                ?: ""
        }

        private fun buildWhereClause(
            searchExpression: SearchExpression?,
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val metadataFilters: String =
                metadataSearchFilter.joinToString(separator = " AND ") { f ->
                    f.toWhereClause()
                }
            val noRightInformationFilterClause: String = noRightInformationFilter?.toWhereClause() ?: ""
            val searchExpressionFilters: String =
                searchExpression?.let {
                    resolveSearchExpression(it)
                } ?: ""
            val rightFilters =
                rightSearchFilter.joinToString(separator = " AND ") { f ->
                    f.toWhereClause()
                }
            val whereClauseList =
                listOf(
                    searchExpressionFilters,
                    metadataFilters,
                    rightFilters,
                    noRightInformationFilterClause,
                )

            return whereClauseList
                .filter { it.isNotBlank() }
                .joinToString(separator = " AND ")
                .takeIf { it.isNotBlank() }
                ?: ""
        }

        private fun buildSearchQueryHelper(
            searchExpression: SearchExpression?,
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val metadataFilters: String =
                metadataSearchFilter.joinToString(separator = " AND ") { f ->
                    f.toWhereClause()
                }
            val noRightInformationFilterClause: String = noRightInformationFilter?.toWhereClause() ?: ""
            val searchExpressionFilters: String =
                searchExpression?.let {
                    resolveSearchExpression(it)
                } ?: ""
            val searchExprUsesRights =
                searchExpression?.let {
                    SearchExpressionResolution.hasRightQueries(searchExpression)
                } == true
            val whereClauseList =
                if (searchExprUsesRights) {
                    listOf(
                        metadataFilters,
                        noRightInformationFilterClause,
                    )
                } else {
                    listOf(
                        searchExpressionFilters,
                        metadataFilters,
                        noRightInformationFilterClause,
                    )
                }

            /**
             * metadataFilter: String?
             * metadataExceptionFilter: String?
             * noRightFilter: String?
             * noRightExceptionFilter: String?
             */
            val rightFilters =
                rightSearchFilter.joinToString(separator = " AND ") { f ->
                    f.toWhereClause()
                }
            val rightFilterClause =
                if (searchExprUsesRights) {
                    listOf(
                        rightFilters,
                        searchExpressionFilters,
                    )
                } else {
                    listOf(rightFilters)
                }
            val extendedRightFilter =
                rightFilterClause
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " AND ")
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        " WHERE $it"
                    }
                    ?: ""

            val whereClause =
                whereClauseList
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " AND ")
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        if (extendedRightFilter.isNotBlank()) {
                            " AND $it"
                        } else {
                            " WHERE $it"
                        }
                    }
                    ?: ""

            return " LEFT JOIN $TABLE_NAME_ITEM" +
                " ON $TABLE_NAME_ITEM.$COLUMN_METADATA_HANDLE = $ALIAS_ITEM_METADATA.$COLUMN_METADATA_HANDLE" +
                " LEFT JOIN $TABLE_NAME_ITEM_RIGHT as $ALIAS_ITEM_RIGHT" +
                " ON $TABLE_NAME_ITEM.right_id = ${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ID" +
                extendedRightFilter +
                whereClause
        }

        private fun buildSearchQuerySelect(hasRightSearchFilter: Boolean = false): String =
            if (!hasRightSearchFilter) {
                STATEMENT_SELECT_ALL_METADATA
            } else {
                STATEMENT_SELECT_ALL_METADATA_DISTINCT
            }
    }
}
