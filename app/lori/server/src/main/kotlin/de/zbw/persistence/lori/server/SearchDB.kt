package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.utils.SearchExpressionResolution
import de.zbw.business.lori.server.utils.SearchExpressionResolution.resolveSearchExpression
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_AUTHOR
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_BAND
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_COLLECTION_HANDLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_COLLECTION_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_COMMUNITY_HANDLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_COMMUNITY_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_CREATED_BY
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_CREATED_ON
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_DOI
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_HANDLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ISBN
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ISSN
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_IS_PART_OF_SERIES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_LAST_UPDATED_BY
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_LAST_UPDATED_ON
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_LICENCE_URL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PPN
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_RIGHTS_K10PLUS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_STORAGE_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_SUBCOMMUNITY_HANDLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_SUBCOMMUNITY_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_TITLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_TITLE_JOURNAL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_TITLE_SERIES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID_JOURNAL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID_SERIES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ACCESS_STATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_LICENCE_CONTRACT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_TEMPLATE_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ZBW_USER_AGREEMENT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_RIGHT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.addDefaultEntriesToMap
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.MetadataDB.Companion.STATEMENT_SELECT_ALL_METADATA
import de.zbw.persistence.lori.server.MetadataDB.Companion.extractMetadataRS
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.lang.Integer.max

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
            connectionPool.useConnection { connection ->
                val prepStmt =
                    connection
                        .prepareStatement(
                            buildSearchQueryForFacets(
                                searchExpression,
                                metadataSearchFilter,
                                rightSearchFilter,
                                noRightInformationFilter,
                                true,
                            ),
                        ).apply {
                            var counter = 1
                            rightSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            val searchPairs =
                                searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
                            searchPairs.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            metadataSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                        }
                val span = tracer.spanBuilder("searchMetadataWithRightsFilterForZDBAndSigel").startSpan()
                val rs =
                    try {
                        span.makeCurrent()
                        runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
                    } finally {
                        span.end()
                    }

                val received: List<FacetTransient> =
                    generateSequence {
                        if (rs.next()) {
                            FacetTransient(
                                paketSigel = rs.getString(1),
                                publicationType = PublicationType.valueOf(rs.getString(2)),
                                zdbIdJournal = rs.getString(3),
                                accessState = rs.getString(4)?.let { AccessState.valueOf(it) },
                                licenceContract = rs.getString(5),
                                nonStandardsOCL = rs.getBoolean(6),
                                nonStandardsOCLUrl = rs.getString(7),
                                oclRestricted = rs.getBoolean(8),
                                ocl = rs.getString(9),
                                zbwUserAgreement = rs.getBoolean(10),
                                templateName = rs.getString(11),
                                isPartOfSeries = rs.getString(12),
                                zdbIdSeries = rs.getString(13),
                            )
                        } else {
                            null
                        }
                    }.takeWhile { true }.toList()
                val accessStateFacet: Deferred<Map<AccessState, Int>> =
                    async {
                        getAccessStateOccurrences(
                            givenAccessState = received.mapNotNull { it.accessState }.toSet(),
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }

                val paketSigelFacet =
                    async {
                        searchOccurrences(
                            givenValues = received.mapNotNull { it.paketSigel }.toSet(),
                            occurrenceForColumn = COLUMN_METADATA_PAKET_SIGEL,
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }

                val publicationTypeFacet =
                    async {
                        getPublicationTypeOccurrences(
                            givenPublicationType = received.map { it.publicationType }.toSet(),
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }

                val zdbIDsJournalFacet =
                    async {
                        searchOccurrences(
                            givenValues = received.mapNotNull { it.zdbIdJournal }.toSet(),
                            occurrenceForColumn = COLUMN_METADATA_ZDB_ID_JOURNAL,
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }
                val zdbIDsSeriesFacet =
                    async {
                        searchOccurrences(
                            givenValues = received.mapNotNull { it.zdbIdSeries }.toSet(),
                            occurrenceForColumn = COLUMN_METADATA_ZDB_ID_SERIES,
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }

                val isPartOfSeriesFacet =
                    async {
                        searchOccurrences(
                            givenValues = received.mapNotNull { it.isPartOfSeries }.toSet(),
                            occurrenceForColumn = COLUMN_METADATA_IS_PART_OF_SERIES,
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }

                val templateIdFacet =
                    async {
                        searchOccurrences(
                            givenValues = received.mapNotNull { it.templateName }.toSet(),
                            occurrenceForColumn = COLUMN_RIGHT_TEMPLATE_NAME,
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                    }

                return@useConnection FacetTransientSet(
                    accessState = accessStateFacet.await(),
                    paketSigels = paketSigelFacet.await(),
                    publicationType = publicationTypeFacet.await(),
                    zdbIdsJournal = zdbIDsJournalFacet.await(),
                    zdbIdsSeries = zdbIDsSeriesFacet.await(),
                    isPartOfSeries = isPartOfSeriesFacet.await(),
                    hasLicenceContract = received.any { it.licenceContract?.isNotBlank() ?: false },
                    hasZbwUserAgreement = received.any { it.zbwUserAgreement },
                    hasOpenContentLicence =
                        listOf(
                            received.any { it.ocl?.isNotBlank() ?: false },
                            received.any { it.nonStandardsOCL },
                            received.any { it.nonStandardsOCLUrl?.isNotBlank() ?: false },
                            received.any { it.oclRestricted },
                        ).any { it },
                    templateIdToOccurence = templateIdFacet.await(),
                )
            }
        }

    private suspend fun getAccessStateOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenAccessState: Set<AccessState>,
    ): Map<AccessState, Int> =
        searchOccurrences(
            givenValues = givenAccessState.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_RIGHT_ACCESS_STATE,
            searchExpression = searchExpression,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
        ).toList().associate { Pair(AccessState.valueOf(it.first), it.second) }

    private suspend fun getPublicationTypeOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenPublicationType: Set<PublicationType>,
    ): Map<PublicationType, Int> =
        searchOccurrences(
            givenValues = givenPublicationType.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_METADATA_PUBLICATION_TYPE,
            searchExpression = searchExpression,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
        ).toList().associate { Pair(PublicationType.valueOf(it.first), it.second) }.toMutableMap()

    private suspend fun searchOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenValues: Set<String>,
        occurrenceForColumn: String,
    ): Map<String, Int> =
        coroutineScope {
            if (givenValues.isEmpty()) {
                return@coroutineScope emptyMap<String, Int>()
            }
            return@coroutineScope connectionPool.useConnection { connection ->
                val prepStmt =
                    connection
                        .prepareStatement(
                            buildSearchQueryOccurrence(
                                createValuesForSql(givenValues.size),
                                occurrenceForColumn,
                                searchExpression,
                                metadataSearchFilter,
                                rightSearchFilter,
                                noRightInformationFilter,
                            ),
                        ).apply {
                            var counter = 1
                            givenValues.forEach { v ->
                                this.setString(counter++, v)
                            }
                            rightSearchFilter.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            val searchPairs =
                                searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                                    ?: emptyList()
                            searchPairs.forEach { f ->
                                counter = f.setSQLParameter(counter, this)
                            }
                            metadataSearchFilter.forEach { f ->
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

                val received: List<Pair<String, Int>> =
                    generateSequence {
                        if (rs.next()) {
                            Pair(
                                rs.getString(1),
                                rs.getInt(2),
                            )
                        } else {
                            null
                        }
                    }.takeWhile { true }.toList()
                val occurrenceMap = received.toMap().toMutableMap()
                // Make sure that every given value still exist in the resulting map. That should
                // never be necessary but in case of any expected value has no counts, the frontend
                // will still display it.
                return@useConnection addDefaultEntriesToMap(occurrenceMap, givenValues, 0) { a, b -> max(a, b) }
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
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection
                    .prepareStatement(
                        buildCountSearchQuery(
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilter,
                            rightSearchFilter = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                            hasMetadataIdsToIgnore = false,
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
        metadataIdsToIgnore: List<String>,
    ): List<ItemMetadata> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection
                    .prepareStatement(
                        buildSearchQuery(
                            searchExpression = searchExpression,
                            metadataSearchFilters = metadataSearchFilter,
                            rightSearchFilters = rightSearchFilter,
                            noRightInformationFilter = noRightInformationFilter,
                            hasMetadataIdsToIgnore = metadataIdsToIgnore.isNotEmpty(),
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
                        if (metadataIdsToIgnore.isNotEmpty()) {
                            this.setArray(
                                counter++,
                                connection.createArrayOf("text", metadataIdsToIgnore.toTypedArray()),
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
        metadataIdsToIgnore: List<String> = emptyList(),
    ): List<ItemMetadata> =
        searchMetadata(
            searchExpression = searchExpression,
            limit = limit,
            offset = offset,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            metadataIdsToIgnore = metadataIdsToIgnore,
        )

    suspend fun searchForMetadataIds(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        metadataIdsToIgnore: List<String>,
    ): List<String> {
        val rs: List<ItemMetadata> =
            searchMetadata(
                searchExpression = searchExpression,
                limit = limit,
                offset = offset,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                metadataIdsToIgnore = metadataIdsToIgnore,
            )
        return rs.map { it.metadataId }
    }

    companion object {
        const val SUBQUERY_NAME = "sub"
        const val ALIAS_ITEM_RIGHT = "o"
        private const val STATEMENT_SELECT_ALL_FACETS =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,$COLUMN_METADATA_ISSN," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_TEMPLATE_NAME," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_TITLE}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                MetadataDB.TS_SUBCOMMUNITY_NAME

        const val STATEMENT_SELECT_OCCURRENCE_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE," +
                "$COLUMN_RIGHT_TEMPLATE_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_TITLE}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                MetadataDB.TS_SUBCOMMUNITY_NAME

        const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_ACCESS =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id," +
                " ${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_TITLE}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                MetadataDB.TS_SUBCOMMUNITY_NAME

        const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_TEMPLATE_NAME =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_ID, ${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_TEMPLATE_NAME)" +
                " $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE," +
                "$COLUMN_RIGHT_TEMPLATE_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_TITLE}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                MetadataDB.TS_SUBCOMMUNITY_NAME

        const val STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES =
            "SELECT $COLUMN_METADATA_ID,$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,$COLUMN_METADATA_ISSN," +
                "$COLUMN_METADATA_CREATED_ON,$COLUMN_METADATA_LAST_UPDATED_ON,$COLUMN_METADATA_CREATED_BY," +
                "$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES"

        private const val STATEMENT_SELECT_FACET =
            "SELECT" +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_TEMPLATE_NAME," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_IS_PART_OF_SERIES," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_SERIES"

        const val STATEMENT_SELECT_ALL_METADATA_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,$COLUMN_METADATA_ISSN," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ACCESS_STATE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_TITLE}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                MetadataDB.TS_SUBCOMMUNITY_NAME

        internal fun buildSearchQuery(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            hasMetadataIdsToIgnore: Boolean,
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
                    buildSearchQuerySelect(hasRightSearchFilter = false) + " FROM $TABLE_NAME_ITEM_METADATA" +
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
                        " FROM $TABLE_NAME_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            searchExpression,
                            metadataSearchFilters,
                        )
                } else {
                    buildSearchQuerySelect(
                        hasRightSearchFilter =
                            rightSearchFilters.isNotEmpty() || SearchExpressionResolution.hasRightQueries(searchExpression),
                    ) +
                        " FROM $TABLE_NAME_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            searchExpression,
                            metadataSearchFilters,
                            rightSearchFilters,
                            noRightInformationFilter,
                        )
                }

            return if (hasMetadataIdsToIgnore) {
                val filterMetadataIds = "WHERE NOT metadata_id = ANY(?)"
                STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES +
                    " FROM ($subquery) as $SUBQUERY_NAME" +
                    " $filterMetadataIds ORDER BY metadata_id ASC$limit$offset"
            } else {
                "$subquery ORDER BY item_metadata.metadata_id ASC$limit$offset"
            }
        }

        fun buildCountSearchQuery(
            searchExpression: SearchExpression?,
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            hasMetadataIdsToIgnore: Boolean,
        ): String =
            "SELECT COUNT(*) FROM (" +
                buildSearchQuery(
                    searchExpression,
                    metadataSearchFilter,
                    rightSearchFilter,
                    noRightInformationFilter,
                    hasMetadataIdsToIgnore,
                    false,
                    false,
                ) + ") as countsearch"

        fun buildSearchQueryOccurrence(
            values: String,
            columnName: String,
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val subquery =
                buildSearchQuerySelect(
                    hasRightSearchFilter = rightSearchFilters.isNotEmpty(),
                    collectOccurrences = true,
                    collectOccurrencesAccessRight = columnName == COLUMN_RIGHT_ACCESS_STATE,
                    collectOccurrencesTemplateName = columnName == COLUMN_RIGHT_TEMPLATE_NAME,
                ) + " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        searchExpression = searchExpression,
                        metadataSearchFilter = metadataSearchFilters,
                        rightSearchFilter = rightSearchFilters,
                        noRightInformationFilter = noRightInformationFilter,
                    )

            return "SELECT A.$columnName, COUNT(${SUBQUERY_NAME}.$columnName)" +
                " FROM($values) as A($columnName)" +
                " LEFT JOIN ($subquery) AS ${SUBQUERY_NAME}" +
                " ON A.$columnName = ${SUBQUERY_NAME}.$columnName " +
                " GROUP BY A.$columnName"
        }

        internal fun createValuesForSql(given: Int): String = "VALUES " + (1..given).joinToString(separator = ",") { "(?)" }

        fun buildSearchQueryForFacets(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean,
        ): String {
            val subquery =
                if (rightSearchFilters.isEmpty() && !collectFacets) {
                    buildSearchQuerySelect(hasRightSearchFilter = false) +
                        " FROM $TABLE_NAME_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            null,
                            metadataSearchFilters,
                        )
                } else {
                    buildSearchQuerySelect(
                        hasRightSearchFilter = rightSearchFilters.isNotEmpty(),
                        forceRightTableJoin = collectFacets,
                    ) +
                        " FROM $TABLE_NAME_ITEM_METADATA" +
                        buildSearchQueryHelper(
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilters,
                            rightSearchFilter = rightSearchFilters,
                            noRightInformationFilter = noRightInformationFilter,
                        )
                }

            return STATEMENT_SELECT_FACET +
                " FROM ($subquery) as $SUBQUERY_NAME" +
                " GROUP BY" +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                " ${SUBQUERY_NAME}.$COLUMN_RIGHT_TEMPLATE_NAME," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_IS_PART_OF_SERIES," +
                " ${SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_SERIES;"
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
                } ?: false
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
                " ON $TABLE_NAME_ITEM.metadata_id = $TABLE_NAME_ITEM_METADATA.metadata_id" +
                " LEFT JOIN $TABLE_NAME_ITEM_RIGHT as $ALIAS_ITEM_RIGHT" +
                " ON $TABLE_NAME_ITEM.right_id = ${ALIAS_ITEM_RIGHT}.$COLUMN_RIGHT_ID" +
                extendedRightFilter +
                whereClause
        }

        private fun buildSearchQuerySelect(
            hasRightSearchFilter: Boolean = false,
            forceRightTableJoin: Boolean = false,
            collectOccurrences: Boolean = false,
            collectOccurrencesAccessRight: Boolean = false,
            collectOccurrencesTemplateName: Boolean = false,
        ): String =
            if (collectOccurrencesAccessRight) {
                STATEMENT_SELECT_OCCURRENCE_DISTINCT_ACCESS
            } else if (collectOccurrencesTemplateName) {
                STATEMENT_SELECT_OCCURRENCE_DISTINCT_TEMPLATE_NAME
            } else if (collectOccurrences) {
                STATEMENT_SELECT_OCCURRENCE_DISTINCT
            } else if (forceRightTableJoin) {
                STATEMENT_SELECT_ALL_FACETS
            } else if (!hasRightSearchFilter) {
                STATEMENT_SELECT_ALL_METADATA
            } else {
                STATEMENT_SELECT_ALL_METADATA_DISTINCT
            }
    }
}
