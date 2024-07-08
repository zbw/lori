package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchKey
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
import java.lang.Integer.max
import java.sql.Connection
import java.sql.ResultSet

/**
 * Execute SQL queries strongly related to search.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SearchDB(
    val connection: Connection,
    private val tracer: Tracer,
) {
    fun searchForFacets(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): FacetTransientSet {
        val prepStmt = connection.prepareStatement(
            buildSearchQueryForFacets(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                true,
            )
        ).apply {
            var counter = 1
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            val searchPairs = searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
            searchPairs.forEach { entry ->
                this.setString(counter++, entry.values)
            }
        }
        val span = tracer.spanBuilder("searchMetadataWithRightsFilterForZDBAndSigel").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }

        val received: List<FacetTransient> = generateSequence {
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
            } else null
        }.takeWhile { true }.toList()
        return FacetTransientSet(
            accessState = getAccessStateOccurrences(
                givenAccessState = received.mapNotNull { it.accessState }.toSet(),
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            paketSigels = searchOccurrences(
                givenValues = received.mapNotNull { it.paketSigel }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_PAKET_SIGEL,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            publicationType = getPublicationTypeOccurrences(
                givenPublicationType = received.map { it.publicationType }.toSet(),
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            zdbIdsJournal = searchOccurrences(
                givenValues = received.mapNotNull { it.zdbIdJournal }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_ZDB_ID_JOURNAL,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            zdbIdsSeries = searchOccurrences(
                givenValues = received.mapNotNull { it.zdbIdSeries }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_ZDB_ID_SERIES,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            isPartOfSeries = searchOccurrences(
                givenValues = received.mapNotNull { it.isPartOfSeries }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_IS_PART_OF_SERIES,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            hasLicenceContract = received.any { it.licenceContract?.isNotBlank() ?: false },
            hasZbwUserAgreement = received.any { it.zbwUserAgreement },
            hasOpenContentLicence = listOf(
                received.any { it.ocl?.isNotBlank() ?: false },
                received.any { it.nonStandardsOCL },
                received.any { it.nonStandardsOCLUrl?.isNotBlank() ?: false },
                received.any { it.oclRestricted },
            ).any { it },
            templateIdToOccurence = searchOccurrences(
                givenValues = received.mapNotNull { it.templateName }.toSet(),
                occurrenceForColumn = COLUMN_RIGHT_TEMPLATE_NAME,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
        )
    }

    private fun getAccessStateOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenAccessState: Set<AccessState>,
    ): Map<AccessState, Int> {
        return searchOccurrences(
            givenValues = givenAccessState.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_RIGHT_ACCESS_STATE,
            searchExpression = searchExpression,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
        ).toList().associate { Pair(AccessState.valueOf(it.first), it.second) }
    }

    private fun getPublicationTypeOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenPublicationType: Set<PublicationType>,
    ): Map<PublicationType, Int> {
        return searchOccurrences(
            givenValues = givenPublicationType.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_METADATA_PUBLICATION_TYPE,
            searchExpression = searchExpression,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
        ).toList().associate { Pair(PublicationType.valueOf(it.first), it.second) }.toMutableMap()
    }

    private fun searchOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenValues: Set<String>,
        occurrenceForColumn: String,
    ): Map<String, Int> {
        if (givenValues.isEmpty()) {
            return emptyMap()
        }
        val prepStmt = connection.prepareStatement(
            buildSearchQueryOccurrence(
                createValuesForSql(givenValues.size),
                occurrenceForColumn,
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
            )
        ).apply {
            var counter = 1
            givenValues.forEach { v ->
                this.setString(counter++, v)
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            val searchPairs = searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
            searchPairs.forEach { pair ->
                this.setString(counter++, pair.values)
            }
        }
        val span = tracer.spanBuilder("searchForOccurrence").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }

        val received: List<Pair<String, Int>> = generateSequence {
            if (rs.next()) {
                Pair(
                    rs.getString(1),
                    rs.getInt(2),
                )
            } else null
        }.takeWhile { true }.toList()
        val occurrenceMap = received.toMap().toMutableMap()
        // Make sure that every given value still exist in the resulting map. That should
        // never be necessary but in case of any expected value has no counts, the frontend
        // will still display it.
        return addDefaultEntriesToMap(occurrenceMap, givenValues, 0) { a, b -> max(a, b) }
    }

    /**
     * Search related queries.
     */
    fun countSearchMetadata(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
        noRightInformationFilter: NoRightInformationFilter?,
    ): Int {
        val prepStmt = connection.prepareStatement(
            buildCountSearchQuery(
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                hasMetadataIdsToIgnore = false,
            )
        )
            .apply {
                var counter = 1
                val searchPairs = searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
                searchPairs.forEach { pair ->
                    this.setString(counter++, pair.getValuesAsString())
                }
                rightSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                metadataSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                searchPairs.forEach { pair ->
                    this.setString(counter++, pair.getValuesAsString())
                }
            }
        val span = tracer.spanBuilder("countMetadataSearch").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }
        if (rs.next()) {
            return rs.getInt(1)
        } else throw IllegalStateException("No count found.")
    }

    fun searchMetadata(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        metadataIdsToIgnore: List<String>,
    ): ResultSet {
        val prepStmt = connection.prepareStatement(
            buildSearchQuery(
                searchExpression = searchExpression,
                metadataSearchFilters = metadataSearchFilter,
                rightSearchFilters = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                hasMetadataIdsToIgnore = metadataIdsToIgnore.isNotEmpty(),
                withLimit = limit != null,
                withOffset = offset != null,
            )
        ).apply {
            var counter = 1
            val searchPairs = searchExpression
                ?.let { SearchExpressionResolution.getSearchPairs(it) }
                ?: emptyList()
            searchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            searchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
            }
            if (metadataIdsToIgnore.isNotEmpty()) {
                this.setArray(counter++, connection.createArrayOf("text", metadataIdsToIgnore.toTypedArray()))
            }
            if (limit != null) {
                this.setInt(counter++, limit)
            }
            if (offset != null) {
                this.setInt(counter, offset)
            }
        }
        val span = tracer.spanBuilder("searchMetadataWithRightsFilter").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }
    }

    fun searchMetadataItems(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        metadataIdsToIgnore: List<String> = emptyList(),
    ): List<ItemMetadata> {
        val rs = searchMetadata(
            searchExpression = searchExpression,
            limit = limit,
            offset = offset,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            metadataIdsToIgnore = metadataIdsToIgnore,
        )
        return generateSequence {
            if (rs.next()) {
                extractMetadataRS(rs)
            } else null
        }.takeWhile { true }.toList()
    }

    fun searchForMetadataIds(
        searchExpression: SearchExpression?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        metadataIdsToIgnore: List<String>
    ): List<String> {
        val rs = searchMetadata(
            searchExpression = searchExpression,
            limit = limit,
            offset = offset,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            metadataIdsToIgnore = metadataIdsToIgnore,
        )
        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else null
        }.takeWhile { true }.toList()
    }

    companion object {
        private const val STATEMENT_SELECT_ALL_FACETS =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,$COLUMN_METADATA_ISSN," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_TEMPLATE_NAME," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID_JOURNAL}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                "${MetadataDB.TS_SUBCOMMUNITY_NAME},${MetadataDB.TS_IS_PART_OF_SERIES},${MetadataDB.TS_ZDB_ID_SERIES}"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$COLUMN_RIGHT_TEMPLATE_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID_JOURNAL}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                "${MetadataDB.TS_SUBCOMMUNITY_NAME},${MetadataDB.TS_IS_PART_OF_SERIES},${MetadataDB.TS_ZDB_ID_SERIES}"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_ACCESS =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id, $TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID_JOURNAL}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                "${MetadataDB.TS_SUBCOMMUNITY_NAME},${MetadataDB.TS_IS_PART_OF_SERIES},${MetadataDB.TS_ZDB_ID_SERIES}"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_TEMPLATE_NAME =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_ID, $TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_TEMPLATE_NAME) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$COLUMN_RIGHT_TEMPLATE_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID_JOURNAL}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                "${MetadataDB.TS_SUBCOMMUNITY_NAME},${MetadataDB.TS_IS_PART_OF_SERIES},${MetadataDB.TS_ZDB_ID_SERIES}"

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
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_TEMPLATE_NAME," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_IS_PART_OF_SERIES," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_SERIES"

        const val STATEMENT_SELECT_ALL_METADATA_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,$COLUMN_METADATA_ISSN," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID_JOURNAL}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}," +
                "${MetadataDB.TS_SUBCOMMUNITY_NAME},${MetadataDB.TS_IS_PART_OF_SERIES},${MetadataDB.TS_ZDB_ID_SERIES}"

        fun buildSearchQuery(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            hasMetadataIdsToIgnore: Boolean,
            withLimit: Boolean = true,
            withOffset: Boolean = true,
        ): String {
            val limit = if (withLimit) {
                " LIMIT ?"
            } else ""
            val offset = if (withOffset) {
                " OFFSET ?"
            } else ""

            val subquery = if (
                searchExpression == null &&
                rightSearchFilters.isEmpty() &&
                noRightInformationFilter == null &&
                metadataSearchFilters.isEmpty()
            ) {
                buildSearchQuerySelect(hasRightSearchFilter = false) + " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    )
            } else if (
                rightSearchFilters.isEmpty() &&
                metadataSearchFilters.isEmpty() &&
                noRightInformationFilter == null
            ) {
                TABLE_NAME_ITEM_METADATA
            } else if (
                rightSearchFilters.isEmpty() &&
                noRightInformationFilter == null
            ) {
                "(" +
                    buildSearchQuerySelect(hasRightSearchFilter = false) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    ) +
                    ")"
            } else if (searchExpression != null) {
                "(" + buildSearchQuerySelect(hasRightSearchFilter = rightSearchFilters.isNotEmpty()) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                    ) + ")"
            } else {
                buildSearchQuerySelect(hasRightSearchFilter = rightSearchFilters.isNotEmpty()) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                    )
            }

            return if (searchExpression == null && hasMetadataIdsToIgnore) {
                val filterMetadataIds = "WHERE NOT metadata_id = ANY(?)"
                STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES +
                    " FROM $subquery as ${SearchKey.SUBQUERY_NAME}" +
                    " $filterMetadataIds ORDER BY metadata_id ASC$limit$offset"
            } else if (searchExpression == null) {
                "$subquery ORDER BY item_metadata.metadata_id ASC$limit$offset"
            } else {
                val filterMetadataIds = if (hasMetadataIdsToIgnore) {
                    "NOT sub.metadata_id = ANY(?)"
                } else {
                    ""
                }
                val trgmWhere = listOf(resolveSearchExpression(searchExpression), filterMetadataIds)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " AND ")
                    .takeIf { it.isNotBlank() }
                    ?.let { " WHERE $it" }
                    ?: ""
                val coalesceScore =
                    SearchExpressionResolution.resolveSearchExpressionCoalesce(searchExpression, "score")
                "$STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES,$coalesceScore" +
                    " FROM $subquery as ${SearchKey.SUBQUERY_NAME}" +
                    trgmWhere +
                    " ORDER BY score DESC" +
                    limit +
                    offset
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
            val subquery = buildSearchQuerySelect(
                hasRightSearchFilter = rightSearchFilters.isNotEmpty(),
                collectOccurrences = true,
                collectOccurrencesAccessRight = columnName == COLUMN_RIGHT_ACCESS_STATE,
                collectOccurrencesTemplateName = columnName == COLUMN_RIGHT_TEMPLATE_NAME,
            ) + " FROM $TABLE_NAME_ITEM_METADATA" +
                buildSearchQueryHelper(
                    metadataSearchFilter = metadataSearchFilters,
                    rightSearchFilter = rightSearchFilters,
                    noRightInformationFilter = noRightInformationFilter,
                    collectFacets = true,
                )

            val trgmWhere =
                if (searchExpression == null) {
                    ""
                } else {
                    " WHERE " + resolveSearchExpression(searchExpression)
                }

            return "SELECT A.$columnName, COUNT(${SearchKey.SUBQUERY_NAME}.$columnName)" +
                " FROM($values) as A($columnName)" +
                " LEFT JOIN ($subquery) AS ${SearchKey.SUBQUERY_NAME}" +
                " ON A.$columnName = ${SearchKey.SUBQUERY_NAME}.$columnName " +
                trgmWhere +
                " GROUP BY A.$columnName"
        }

        internal fun createValuesForSql(given: Int): String =
            "VALUES " + (1..given).joinToString(separator = ",") { "(?)" }

        fun buildSearchQueryForFacets(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean,
        ): String {
            val subquery = if (rightSearchFilters.isEmpty() && !collectFacets) {
                buildSearchQuerySelect(hasRightSearchFilter = false) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    )
            } else {
                buildSearchQuerySelect(
                    hasRightSearchFilter = rightSearchFilters.isNotEmpty(),
                    forceRightTableJoin = collectFacets
                ) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                        collectFacets,
                    )
            }
            val trgmWhere =
                if (searchExpression == null) {
                    ""
                } else {
                    " WHERE " + resolveSearchExpression(searchExpression)
                }

            return STATEMENT_SELECT_FACET +
                " FROM ($subquery) as ${SearchKey.SUBQUERY_NAME}" +
                trgmWhere +
                " GROUP BY" +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_TEMPLATE_NAME," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_IS_PART_OF_SERIES," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID_SERIES;"
        }

        private fun buildSearchQueryHelper(
            metadataSearchFilter: List<MetadataSearchFilter>,
        ): String {

            val metadataFilters = metadataSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?: ""
            return listOf(
                metadataFilters,
            )
                .filter { it.isNotBlank() }
                .joinToString(separator = " AND ")
                .takeIf { it.isNotBlank() }
                ?.let { " WHERE $it" }
                ?: ""
        }

        private fun buildSearchQueryHelper(
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean = false,
        ): String {
            val metadataFilters: String = metadataSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }
            val noRightInformationFilterClause: String = noRightInformationFilter?.toWhereClause() ?: ""
            val whereClause =
                listOf(
                    metadataFilters,
                    noRightInformationFilterClause,
                )
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " AND ")
                    .takeIf { it.isNotBlank() }
                    ?.let { " WHERE $it" }
                    ?: ""

            /**
             * metadataFilter: String?
             * metadataExceptionFilter: String?
             * noRightFilter: String?
             * noRightExceptionFilter: String?
             */
            val rightFilters = rightSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }
            val extendedRightFilter = listOf(rightFilters)
                .filter { it.isNotBlank() }
                .joinToString(separator = " AND ")
                .takeIf { it.isNotBlank() }
                ?.let { " AND $it" }
                ?: ""

            val joinItemRight =
                if (
                    (collectFacets && rightSearchFilter.isEmpty()) ||
                    noRightInformationFilterClause.isNotBlank()
                ) {
                    "LEFT JOIN"
                } else {
                    "JOIN"
                }
            return " LEFT JOIN $TABLE_NAME_ITEM" +
                " ON $TABLE_NAME_ITEM.metadata_id = $TABLE_NAME_ITEM_METADATA.metadata_id" +
                " $joinItemRight $TABLE_NAME_ITEM_RIGHT" +
                " ON $TABLE_NAME_ITEM.right_id = $TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ID" +
                extendedRightFilter +
                whereClause
        }

        private fun buildSearchQuerySelect(
            hasRightSearchFilter: Boolean = false,
            forceRightTableJoin: Boolean = false,
            collectOccurrences: Boolean = false,
            collectOccurrencesAccessRight: Boolean = false,
            collectOccurrencesTemplateName: Boolean = false,
        ): String {
            return if (collectOccurrencesAccessRight) {
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
}
