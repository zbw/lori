package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.SearchKey
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ACCESS_STATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_LICENCE_CONTRACT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE
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
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): FacetTransientSet {
        val entries: List<Map.Entry<SearchKey, List<String>>> = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildSearchQueryForFacets(
                searchTerms,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
                true,
            )
        ).apply {
            var counter = 1
            entries.forEach { entry ->
                this.setString(counter++, entry.value.joinToString(" "))
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
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
                )
            } else null
        }.takeWhile { true }.toList()
        return FacetTransientSet(
            accessState = getAccessStateOccurrences(
                givenAccessState = received.mapNotNull { it.accessState }.toSet(),
                searchTerms = searchTerms,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            paketSigels = searchOccurrences(
                givenValues = received.mapNotNull { it.paketSigel }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_PAKET_SIGEL,
                searchTerms = searchTerms,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            publicationType = getPublicationTypeOccurrences(
                givenPublicationType = received.map { it.publicationType }.toSet(),
                searchTerms = searchTerms,
                metadataSearchFilter = metadataSearchFilter,
                rightSearchFilter = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
            ),
            zdbIds = searchOccurrences(
                givenValues = received.mapNotNull { it.zdbId }.toSet(),
                occurrenceForColumn = COLUMN_METADATA_ZDB_ID,
                searchTerms = searchTerms,
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
            ).any { it }
        )
    }

    private fun getAccessStateOccurrences(
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenAccessState: Set<AccessState>,
    ): Map<AccessState, Int> {
        return searchOccurrences(
            givenValues = givenAccessState.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_RIGHT_ACCESS_STATE,
            searchTerms = searchTerms,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
        ).toList().associate { Pair(AccessState.valueOf(it.first), it.second) }
    }

    private fun getPublicationTypeOccurrences(
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenPublicationType: Set<PublicationType>,
    ): Map<PublicationType, Int> {
        return searchOccurrences(
            givenValues = givenPublicationType.map { it.toString() }.toSet(),
            occurrenceForColumn = COLUMN_METADATA_PUBLICATION_TYPE,
            searchTerms = searchTerms,
            metadataSearchFilter = metadataSearchFilter,
            rightSearchFilter = rightSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
        ).toList().associate { Pair(PublicationType.valueOf(it.first), it.second) }.toMutableMap()
    }

    private fun searchOccurrences(
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        givenValues: Set<String>,
        occurrenceForColumn: String,
    ): Map<String, Int> {
        if (givenValues.isEmpty()) {
            return emptyMap()
        }
        val entries: List<Map.Entry<SearchKey, List<String>>> = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildSearchQueryOccurrence(
                createValuesForSql(givenValues),
                occurrenceForColumn,
                searchTerms,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
            )
        ).apply {
            var counter = 1
            entries.forEach { entry ->
                this.setString(counter++, entry.value.joinToString(" "))
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
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
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
        noRightInformationFilter: NoRightInformationFilter?,
    ): Int {
        val entries = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildCountSearchQuery(
                searchTerms,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
            )
        )
            .apply {
                var counter = 1
                entries.forEach { entry ->
                    this.setString(counter++, entry.value.joinToString(" "))
                }
                rightSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                metadataSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
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
        searchTerms: Map<SearchKey, List<String>>,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): List<ItemMetadata> {
        val entries: List<Map.Entry<SearchKey, List<String>>> = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildSearchQuery(
                searchKeyMap = searchTerms,
                metadataSearchFilters = metadataSearchFilter,
                rightSearchFilters = rightSearchFilter,
                noRightInformationFilter = noRightInformationFilter,
                withLimit = limit != null,
                withOffset = offset != null,
            )
        ).apply {
            var counter = 1
            entries.forEach { entry ->
                this.setString(counter++, entry.value.joinToString(" "))
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
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
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ZBW_USER_AGREEMENT"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE"

        private const val STATEMENT_SELECT_OCCURRENCE_DISTINCT_ACCESS =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id, $TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE) $TABLE_NAME_ITEM_METADATA.metadata_id," +
                "$COLUMN_METADATA_PUBLICATION_TYPE," +
                "$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE"

        const val STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES =
            "SELECT metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "created_on,last_updated_on," +
                "created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date"

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
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ZBW_USER_AGREEMENT"

        const val STATEMENT_SELECT_ALL_METADATA_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ZBW_USER_AGREEMENT"

        fun buildSearchQuery(
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            withLimit: Boolean = true,
            withOffset: Boolean = true,
        ): String {
            val subquery = if (rightSearchFilters.isEmpty() && noRightInformationFilter == null) {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    )
            } else {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                    )
            }

            val limit = if (withLimit) {
                " LIMIT ?"
            } else ""
            val offset = if (withOffset) {
                " OFFSET ?"
            } else ""

            return if (searchKeyMap.isEmpty()) {
                "$subquery ORDER BY item_metadata.metadata_id ASC$limit$offset"
            } else {
                val trgmWhere = searchKeyMap.entries.joinToString(separator = " AND ") { entry ->
                    entry.key.toWhereClause()
                }
                val coalesceScore = searchKeyMap.entries.joinToString(
                    prefix = "(",
                    postfix = ")",
                    separator = " + ",
                ) { entry ->
                    "coalesce(${SearchKey.SUBQUERY_NAME}.${entry.key.distColumnName},1)"
                } + "/${searchKeyMap.size} as score"
                "$STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES,$coalesceScore" +
                    " FROM ($subquery) as ${SearchKey.SUBQUERY_NAME}" +
                    " WHERE $trgmWhere" +
                    " ORDER BY score" +
                    limit +
                    offset
            }
        }

        fun buildCountSearchQuery(
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String =
            "SELECT COUNT(*) FROM (" +
                buildSearchQuery(
                    searchKeyMap,
                    metadataSearchFilter,
                    rightSearchFilter,
                    noRightInformationFilter,
                    false,
                    false,
                ) + ") as foo"

        fun buildSearchQueryOccurrence(
            values: String,
            columnName: String,
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
        ): String {
            val subquery = buildSearchQuerySelect(
                searchKeyMap = searchKeyMap,
                rightSearchFilters = rightSearchFilters,
                collectOccurrences = true,
                collectOccurrencesAccessRight = columnName == COLUMN_RIGHT_ACCESS_STATE,
            ) + " FROM $TABLE_NAME_ITEM_METADATA" +
                buildSearchQueryHelper(
                    metadataSearchFilter = metadataSearchFilters,
                    rightSearchFilter = rightSearchFilters,
                    noRightInformationFilter = noRightInformationFilter,
                    collectFacets = true,
                )
            val trgmWhere = searchKeyMap.entries.joinToString(separator = " AND ") { entry ->
                entry.key.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?.let {
                    " WHERE $it"
                }
                ?: ""

            return "SELECT A.$columnName, COUNT(${SearchKey.SUBQUERY_NAME}.$columnName)" +
                " FROM($values) as A($columnName)" +
                " LEFT JOIN ($subquery) AS ${SearchKey.SUBQUERY_NAME}" +
                " ON A.$columnName = ${SearchKey.SUBQUERY_NAME}.$columnName " +
                trgmWhere +
                " GROUP BY A.$columnName"
        }

        internal fun createValuesForSql(given: Set<String>): String =
            "VALUES " + given.joinToString(separator = ",") { "('$it')" }

        fun buildSearchQueryForFacets(
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean,
        ): String {
            val subquery = if (rightSearchFilters.isEmpty() && !collectFacets) {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    )
            } else {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters, collectFacets) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        noRightInformationFilter,
                        collectFacets,
                    )
            }
            val trgmWhere = searchKeyMap.entries.joinToString(separator = " AND ") { entry ->
                entry.key.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?.let {
                    " WHERE $it"
                }
                ?: ""

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
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID;"
        }

        private fun buildSearchQueryHelper(
            searchFilter: List<MetadataSearchFilter>,
        ): String {

            val filter = searchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?: ""
            return if (filter.isBlank()) {
                ""
            } else {
                " WHERE $filter"
            }
        }

        private fun buildSearchQueryHelper(
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            noRightInformationFilter: NoRightInformationFilter?,
            collectFacets: Boolean = false,
        ): String {

            val metadataFilters = metadataSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() } ?: ""
            val noRightInformationFilterClause = noRightInformationFilter?.let {
                if (metadataFilters.isBlank()) {
                    " WHERE " + noRightInformationFilter.toWhereClause()
                } else {
                    " AND " + noRightInformationFilter.toWhereClause()
                }
            } ?: ""
            val rightFilters = rightSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }
            val whereClause = if (metadataFilters.isBlank()) {
                ""
            } else {
                " WHERE $metadataFilters"
            }
            val extendedRightFilter = if (rightFilters.isBlank()) {
                rightFilters
            } else {
                " AND $rightFilters"
            }

            val joinItemRight =
                if (
                    (collectFacets && rightSearchFilter.isEmpty()) || noRightInformationFilterClause.isNotBlank()
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
                whereClause +
                noRightInformationFilterClause
        }

        private fun buildSearchQuerySelect(
            searchKeyMap: Map<SearchKey, List<String>>,
            rightSearchFilters: List<RightSearchFilter>,
            forceRightTableJoin: Boolean = false,
            collectOccurrences: Boolean = false,
            collectOccurrencesAccessRight: Boolean = false,
        ): String {
            val trgmSelect = searchKeyMap.entries.joinToString(separator = ",") { entry ->
                entry.key.toSelectClause()
            }.takeIf { it.isNotBlank() }
                ?.let {
                    ",$it"
                } ?: ""
            return if (collectOccurrencesAccessRight) {
                "$STATEMENT_SELECT_OCCURRENCE_DISTINCT_ACCESS$trgmSelect"
            } else if (collectOccurrences) {
                "$STATEMENT_SELECT_OCCURRENCE_DISTINCT$trgmSelect"
            } else if (forceRightTableJoin) {
                "$STATEMENT_SELECT_ALL_FACETS$trgmSelect"
            } else if (rightSearchFilters.isEmpty()) {
                "$STATEMENT_SELECT_ALL_METADATA$trgmSelect"
            } else {
                "$STATEMENT_SELECT_ALL_METADATA_DISTINCT$trgmSelect"
            }
        }
    }
}
