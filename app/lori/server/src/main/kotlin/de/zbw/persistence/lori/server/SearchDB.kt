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
import de.zbw.business.lori.server.utils.SearchExpressionResolution.conjungateSearchExpressions
import de.zbw.business.lori.server.utils.SearchExpressionResolution.negateSearchExpression
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
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_LAST_UPDATED_BY
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_LAST_UPDATED_ON
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_LICENCE_URL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PPN
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_RIGHTS_K10PLUS
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_STORAGE_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_SUBCOMMUNITIES_HANDLES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_TITLE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_TITLE_JOURNAL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_TITLE_SERIES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID
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
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataFilter: List<MetadataSearchFilter>,
        exceptionRightSearchFilter: List<RightSearchFilter>,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
    ): FacetTransientSet {
        val prepStmt = connection.prepareStatement(
            buildSearchQueryForFacets(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                exceptionSearchExpression,
                exceptionMetadataFilter,
                exceptionRightSearchFilter,
                exceptionNoRightInformationFilter,
                true,
            )
        ).apply {
            var counter = 1
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            exceptionRightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            exceptionMetadataFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            val searchPairs = searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
            searchPairs.forEach { entry ->
                this.setString(counter++, entry.values)
            }
            val excSearchPairs =
                exceptionSearchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
            excSearchPairs.forEach { entry ->
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
                    zdbId = rs.getString(3),
                    accessState = rs.getString(4)?.let { AccessState.valueOf(it) },
                    licenceContract = rs.getString(5),
                    nonStandardsOCL = rs.getBoolean(6),
                    nonStandardsOCLUrl = rs.getString(7),
                    oclRestricted = rs.getBoolean(8),
                    ocl = rs.getString(9),
                    zbwUserAgreement = rs.getBoolean(10),
                    templateName = rs.getString(11),
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
                exceptionMetadataFilter = exceptionMetadataFilter,
                exceptionRightSearchFilter = exceptionRightSearchFilter,
                exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
                exceptionSearchExpression = exceptionSearchExpression,
            ),
            paketSigels = searchOccurrences(
                givenValues = received.mapNotNull { it.paketSigel }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_PAKET_SIGEL,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                exceptionMetadataFilter = exceptionMetadataFilter,
                exceptionRightSearchFilter = exceptionRightSearchFilter,
                exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
                exceptionSearchExpression = exceptionSearchExpression,
            ),
            publicationType = getPublicationTypeOccurrences(
                givenPublicationType = received.map { it.publicationType }.toSet(),
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                exceptionMetadataFilter = exceptionMetadataFilter,
                exceptionRightSearchFilter = exceptionRightSearchFilter,
                exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
                exceptionSearchExpression = exceptionSearchExpression,
            ),
            zdbIds = searchOccurrences(
                givenValues = received.mapNotNull { it.zdbId }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_ZDB_ID,
                searchExpression = searchExpression,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                exceptionMetadataFilter = exceptionMetadataFilter,
                exceptionRightSearchFilter = exceptionRightSearchFilter,
                exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
                exceptionSearchExpression = exceptionSearchExpression,
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
                exceptionMetadataFilter = exceptionMetadataFilter,
                exceptionRightSearchFilter = exceptionRightSearchFilter,
                exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
                exceptionSearchExpression = exceptionSearchExpression,
            ),
        )
    }

    private fun getAccessStateOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataFilter: List<MetadataSearchFilter>,
        exceptionRightSearchFilter: List<RightSearchFilter>,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
        givenAccessState: Set<AccessState>,
    ): Map<AccessState, Int> {
        return searchOccurrences(
            givenValues = givenAccessState.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_RIGHT_ACCESS_STATE,
            searchExpression = searchExpression,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            exceptionMetadataFilter = exceptionMetadataFilter,
            exceptionRightSearchFilter = exceptionRightSearchFilter,
            exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
            exceptionSearchExpression = exceptionSearchExpression,
        ).toList().associate { Pair(AccessState.valueOf(it.first), it.second) }
    }

    private fun getPublicationTypeOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataFilter: List<MetadataSearchFilter>,
        exceptionRightSearchFilter: List<RightSearchFilter>,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
        givenPublicationType: Set<PublicationType>,
    ): Map<PublicationType, Int> {
        return searchOccurrences(
            givenValues = givenPublicationType.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_METADATA_PUBLICATION_TYPE,
            searchExpression = searchExpression,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            exceptionMetadataFilter = exceptionMetadataFilter,
            exceptionRightSearchFilter = exceptionRightSearchFilter,
            exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
            exceptionSearchExpression = exceptionSearchExpression,
        ).toList().associate { Pair(PublicationType.valueOf(it.first), it.second) }.toMutableMap()
    }

    private fun searchOccurrences(
        searchExpression: SearchExpression?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataFilter: List<MetadataSearchFilter>,
        exceptionRightSearchFilter: List<RightSearchFilter>,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
        givenValues: Set<String>,
        occurrenceForColumn: String,
    ): Map<String, Int> {
        if (givenValues.isEmpty()) {
            return emptyMap()
        }
        val prepStmt = connection.prepareStatement(
            buildSearchQueryOccurrence(
                createValuesForSql(givenValues),
                occurrenceForColumn,
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                exceptionSearchExpression,
                exceptionMetadataFilter,
                exceptionRightSearchFilter,
                exceptionNoRightInformationFilter,
            )
        ).apply {
            var counter = 1
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            exceptionRightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            exceptionMetadataFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            val searchPairs = searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
            searchPairs.forEach { pair ->
                this.setString(counter++, pair.values)
            }
            val excSearchPairs = exceptionSearchExpression
                ?.let { SearchExpressionResolution.getSearchPairs(it) }
                ?: emptyList()
            excSearchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
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
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataFilter: List<MetadataSearchFilter>,
        exceptionRightSearchFilter: List<RightSearchFilter>,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
    ): Int {
        val prepStmt = connection.prepareStatement(
            buildCountSearchQuery(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                exceptionSearchExpression,
                exceptionMetadataFilter,
                exceptionRightSearchFilter,
                exceptionNoRightInformationFilter,
            )
        )
            .apply {
                var counter = 1
                val searchPairs = searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) } ?: emptyList()
                searchPairs.forEach { pair ->
                    this.setString(counter++, pair.getValuesAsString())
                }
                val excSearchPairs = exceptionSearchExpression
                    ?.let { SearchExpressionResolution.getSearchPairs(it) }
                    ?: emptyList()
                excSearchPairs.forEach { pair ->
                    this.setString(counter++, pair.getValuesAsString())
                }
                rightSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                exceptionRightSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                metadataSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                exceptionMetadataFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                searchPairs.forEach { pair ->
                    this.setString(counter++, pair.getValuesAsString())
                }
                excSearchPairs.forEach { pair ->
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
        exceptionSearchExpression: SearchExpression?,
        exceptionMetadataFilter: List<MetadataSearchFilter>,
        exceptionRightSearchFilter: List<RightSearchFilter>,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
    ): List<ItemMetadata> {
        val prepStmt = connection.prepareStatement(
            buildSearchQuery(
                searchExpression = searchExpression,
                metadataSearchFilters = metadataSearchFilter,
                rightSearchFilters = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                withLimit = limit != null,
                withOffset = offset != null,
                exceptionSearchExpression = exceptionSearchExpression,
                exceptionMetadataFilter = exceptionMetadataFilter,
                exceptionRightSearchFilter = exceptionRightSearchFilter,
                exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
            )
        ).apply {
            var counter = 1
            val searchPairs = searchExpression
                ?.let { SearchExpressionResolution.getSearchPairs(it) }
                ?: emptyList()
            searchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
            }
            val excSearchPairs = exceptionSearchExpression
                ?.let { SearchExpressionResolution.getSearchPairs(it) }
                ?: emptyList()
            excSearchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            exceptionRightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            exceptionMetadataFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            searchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
            }
            excSearchPairs.forEach { pair ->
                this.setString(counter++, pair.getValuesAsString())
            }
            if (limit != null) {
                this.setInt(counter++, limit)
            }

            if (offset != null) {
                this.setInt(counter, offset)
            }
        }
        val span = tracer.spanBuilder("searchMetadataWithRightsFilter").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                extractMetadataRS(rs)
            } else null
        }.takeWhile { true }.toList()
    }

    companion object {
        private const val STATEMENT_SELECT_ALL_FACETS =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,$COLUMN_METADATA_ISSN," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITIES_HANDLES," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_TEMPLATE_NAME," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$COLUMN_RIGHT_TEMPLATE_NAME," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_ACCESS =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id, $TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_TEMPLATE_NAME =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_ID, $TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_TEMPLATE_NAME) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$COLUMN_RIGHT_TEMPLATE_NAME," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}"

        const val STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES =
            "SELECT $COLUMN_METADATA_ID,$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,$COLUMN_METADATA_ISSN," +
                "$COLUMN_METADATA_CREATED_ON,$COLUMN_METADATA_LAST_UPDATED_ON,$COLUMN_METADATA_CREATED_BY," +
                "$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITIES_HANDLES," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL"

        private const val STATEMENT_SELECT_FACET =
            "SELECT" +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_TEMPLATE_NAME"

        const val STATEMENT_SELECT_ALL_METADATA_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_HANDLE,$COLUMN_METADATA_PPN,$COLUMN_METADATA_TITLE," +
                "$COLUMN_METADATA_TITLE_JOURNAL,$COLUMN_METADATA_TITLE_SERIES,$COLUMN_METADATA_PUBLICATION_DATE," +
                "$COLUMN_METADATA_BAND,$COLUMN_METADATA_PUBLICATION_TYPE,$COLUMN_METADATA_DOI,$COLUMN_METADATA_ISBN," +
                "$COLUMN_METADATA_RIGHTS_K10PLUS,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,$COLUMN_METADATA_ISSN," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_ON,$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_CREATED_BY," +
                "$TABLE_NAME_ITEM_METADATA.$COLUMN_METADATA_LAST_UPDATED_BY,$COLUMN_METADATA_AUTHOR,$COLUMN_METADATA_COLLECTION_NAME," +
                "$COLUMN_METADATA_COMMUNITY_NAME,$COLUMN_METADATA_STORAGE_DATE,$COLUMN_METADATA_SUBCOMMUNITIES_HANDLES," +
                "$COLUMN_METADATA_COMMUNITY_HANDLE,$COLUMN_METADATA_COLLECTION_HANDLE,$COLUMN_METADATA_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "${MetadataDB.TS_COLLECTION},${MetadataDB.TS_COMMUNITY}," +
                "${MetadataDB.TS_SIGEL},${MetadataDB.TS_TITLE},${MetadataDB.TS_ZDB_ID}," +
                "${MetadataDB.TS_COLLECTION_HANDLE},${MetadataDB.TS_COMMUNITY_HANDLE},${MetadataDB.TS_SUBCOMMUNITY_HANDLE}," +
                "${MetadataDB.TS_HANDLE},${MetadataDB.TS_METADATA_ID},${MetadataDB.TS_LICENCE_URL}"

        fun buildSearchQuery(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            exceptionSearchExpression: SearchExpression?,
            exceptionMetadataFilter: List<MetadataSearchFilter>,
            exceptionRightSearchFilter: List<RightSearchFilter>,
            exceptionNoRightInformationFilter: NoRightInformationFilter?,
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
                metadataSearchFilters.isEmpty() &&
                exceptionSearchExpression == null &&
                exceptionRightSearchFilter.isEmpty() &&
                exceptionMetadataFilter.isEmpty() &&
                exceptionNoRightInformationFilter == null
            ) {
                buildSearchQuerySelect(hasRightSearchFilter = false) + " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        exceptionMetadataFilter,
                    )
            } else if (
                rightSearchFilters.isEmpty() &&
                metadataSearchFilters.isEmpty() &&
                noRightInformationFilter == null &&
                exceptionRightSearchFilter.isEmpty() &&
                exceptionMetadataFilter.isEmpty() &&
                exceptionNoRightInformationFilter == null
            ) {
                TABLE_NAME_ITEM_METADATA
            } else if (
                rightSearchFilters.isEmpty() &&
                noRightInformationFilter == null &&
                exceptionRightSearchFilter.isEmpty() &&
                exceptionNoRightInformationFilter == null
            ) {
                "(" +
                    buildSearchQuerySelect(hasRightSearchFilter = false) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        exceptionMetadataFilter,
                    ) +
                    ")"
            } else if (searchExpression != null || exceptionSearchExpression != null) {
                "(" + buildSearchQuerySelect(hasRightSearchFilter = rightSearchFilters.isNotEmpty()) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                        exceptionMetadataFilter,
                        exceptionRightSearchFilter,
                        exceptionNoRightInformationFilter,
                    ) + ")"
            } else {
                buildSearchQuerySelect(hasRightSearchFilter = rightSearchFilters.isNotEmpty()) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                        exceptionMetadataFilter,
                        exceptionRightSearchFilter,
                        exceptionNoRightInformationFilter,
                    )
            }

            return if (searchExpression == null && exceptionSearchExpression == null) {
                "$subquery ORDER BY item_metadata.metadata_id ASC$limit$offset"
            } else {
                val searchExprWExc = conjungateSearchExpressions(
                    searchExpression,
                    negateSearchExpression(exceptionSearchExpression)
                )!!
                val trgmWhere = resolveSearchExpression(searchExprWExc)
                val coalesceScore =
                    SearchExpressionResolution.resolveSearchExpressionCoalesce(searchExprWExc, "score")
                "$STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES,$coalesceScore" +
                    " FROM $subquery as ${SearchKey.SUBQUERY_NAME}" +
                    " WHERE $trgmWhere" +
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
            exceptionSearchExpression: SearchExpression?,
            exceptionMetadataFilter: List<MetadataSearchFilter>,
            exceptionRightSearchFilter: List<RightSearchFilter>,
            exceptionNoRightInformationFilter: NoRightInformationFilter?,
        ): String =
            "SELECT COUNT(*) FROM (" +
                buildSearchQuery(
                    searchExpression,
                    metadataSearchFilter,
                    rightSearchFilter,
                    noRightInformationFilter,
                    exceptionSearchExpression,
                    exceptionMetadataFilter,
                    exceptionRightSearchFilter,
                    exceptionNoRightInformationFilter,
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
            exceptionSearchExpression: SearchExpression?,
            exceptionMetadataFilter: List<MetadataSearchFilter>,
            exceptionRightSearchFilter: List<RightSearchFilter>,
            exceptionNoRightInformationFilter: NoRightInformationFilter?,
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
                    exceptionMetadataFilter = exceptionMetadataFilter,
                    exceptionRightSearchFilter = exceptionRightSearchFilter,
                    exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
                    collectFacets = true,
                )

            val trgmWhere =
                if (searchExpression == null && exceptionSearchExpression == null) {
                    ""
                } else {
                    " WHERE " + resolveSearchExpression(
                        conjungateSearchExpressions(
                            searchExpression,
                            negateSearchExpression(exceptionSearchExpression)
                        )!!
                    )
                }

            return "SELECT A.$columnName, COUNT(${SearchKey.SUBQUERY_NAME}.$columnName)" +
                " FROM($values) as A($columnName)" +
                " LEFT JOIN ($subquery) AS ${SearchKey.SUBQUERY_NAME}" +
                " ON A.$columnName = ${SearchKey.SUBQUERY_NAME}.$columnName " +
                trgmWhere +
                " GROUP BY A.$columnName"
        }

        internal fun createValuesForSql(given: Set<String>): String =
            "VALUES " + given.joinToString(separator = ",") { "('$it')" }

        internal fun <T> createGenericValuesForSql(given: Set<T>): String =
            "VALUES " + given.joinToString(separator = ",") { "($it)" }

        fun buildSearchQueryForFacets(
            searchExpression: SearchExpression?,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            exceptionSearchExpression: SearchExpression?,
            exceptionMetadataFilter: List<MetadataSearchFilter>,
            exceptionRightSearchFilter: List<RightSearchFilter>,
            exceptionNoRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean,
        ): String {
            val subquery = if (rightSearchFilters.isEmpty() && !collectFacets) {
                buildSearchQuerySelect(hasRightSearchFilter = false) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        exceptionMetadataFilter,
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
                        exceptionMetadataFilter,
                        exceptionRightSearchFilter,
                        exceptionNoRightInformationFilter,
                        collectFacets,
                    )
            }
            val trgmWhere =
                if (searchExpression == null && exceptionSearchExpression == null) {
                    ""
                } else {
                    " WHERE " + resolveSearchExpression(
                        conjungateSearchExpressions(
                            searchExpression,
                            negateSearchExpression(exceptionSearchExpression)
                        )!!
                    )
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
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_TEMPLATE_NAME;"
        }

        private fun buildSearchQueryHelper(
            metadataSearchFilter: List<MetadataSearchFilter>,
            exceptionMetadataFilter: List<MetadataSearchFilter>,
        ): String {

            val metadataFilters = metadataSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?: ""
            val exceptionMetadataFilters: String = exceptionMetadataFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?.let { " NOT ($it)" }
                ?: ""

            return listOf(
                metadataFilters,
                exceptionMetadataFilters,
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
            exceptionMetadataFilter: List<MetadataSearchFilter>,
            exceptionRightSearchFilter: List<RightSearchFilter>,
            exceptionNoRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean = false,
        ): String {
            val metadataFilters: String = metadataSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }
            val exceptionMetadataFilters: String = exceptionMetadataFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?.let { " NOT ($it)" }
                ?: ""
            val noRightInformationFilterClause: String = noRightInformationFilter?.toWhereClause() ?: ""
            val exceptionNoRightInformationFilterClause: String = exceptionNoRightInformationFilter?.let {
                " NOT (${it.toWhereClause()})"
            } ?: ""
            val whereClause =
                listOf(
                    metadataFilters,
                    exceptionMetadataFilters,
                    noRightInformationFilterClause,
                    exceptionNoRightInformationFilterClause
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
            val exceptionRightFilters = exceptionRightSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }
                .takeIf { it.isNotBlank() }
                ?.let { " NOT ($it)" }
                ?: ""

            val extendedRightFilter = listOf(rightFilters, exceptionRightFilters)
                .filter { it.isNotBlank() }
                .joinToString(separator = " AND ")
                .takeIf { it.isNotBlank() }
                ?.let { " AND $it" }
                ?: ""

            val joinItemRight =
                if (
                    (collectFacets && rightSearchFilter.isEmpty()) ||
                    noRightInformationFilterClause.isNotBlank() ||
                    exceptionNoRightInformationFilterClause.isNotBlank()
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
