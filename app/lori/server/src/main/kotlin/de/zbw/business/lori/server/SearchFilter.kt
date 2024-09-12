package de.zbw.business.lori.server

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.TemporalValidity
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID_SERIES
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_END_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_START_DATE
import de.zbw.persistence.lori.server.MetadataDB
import de.zbw.persistence.lori.server.SearchDB.Companion.ALIAS_ITEM_RIGHT
import java.sql.Date
import java.sql.PreparedStatement
import java.time.LocalDate

/**
 * Search filters.
 *
 * Created on 09-26-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
abstract class SearchFilter(
    val dbColumnName: String,
) {
    abstract fun toWhereClause(): String

    /**
     * @param preparedStatement: The prepared statement.
     * @param counter: Tracks the index of the next placeholder that should be set in the prepared statement.
     *
     * @return Updated counter.
     */
    abstract fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int

    abstract override fun toString(): String

    abstract fun toSQLString(): String

    abstract fun getFilterType(): FilterType

    companion object {
        fun toSearchFilter(
            searchKey: String,
            searchValue: String,
        ): SearchFilter? =
            try {
                when (searchKey) {
                    "acc" -> QueryParameterParser.parseAccessStateFilter(searchValue.uppercase())
                    "com" -> CommunityNameFilter(searchValue)
                    "col" -> CollectionNameFilter(searchValue)
                    "hdl" -> HandleFilter(searchValue)
                    "sig" -> QueryParameterParser.parsePaketSigelFilter(searchValue)
                    "tit" -> TitleFilter(searchValue)
                    "zdb" -> QueryParameterParser.parseZDBIdFilter(searchValue)
                    "hdlcol" -> CollectionHandleFilter(searchValue)
                    "hdlcom" -> CommunityHandleFilter(searchValue)
                    "hdlsubcom" -> SubcommunityHandleFilter(searchValue)
                    "metadataid" -> MetadataIdFilter(searchValue)
                    "lur" -> LicenceUrlFilter(searchValue)
                    "subcom" -> SubcommunityNameFilter(searchValue)
                    "ser" -> QueryParameterParser.parseSeriesFilter(searchValue)
                    "typ" ->
                        QueryParameterParser.parsePublicationTypeFilter(searchValue)
                    "jah" -> QueryParameterParser.parsePublicationDateFilter(searchValue)
                    "zgp" -> QueryParameterParser.parseRightValidOnFilter(searchValue)
                    "zgb" -> QueryParameterParser.parseStartDateFilter(searchValue)
                    "zge" -> QueryParameterParser.parseEndDateFilter(searchValue)
                    "zga" ->
                        QueryParameterParser.parseTemporalValidity(
                            searchValue
                                .uppercase()
                                .replace("VERGANGENHEIT", TemporalValidity.PAST.toString())
                                .replace("ZUKUNFT", TemporalValidity.FUTURE.toString())
                                .replace("AKTUELL", TemporalValidity.PRESENT.toString()),
                        )
                    "reg" ->
                        QueryParameterParser.parseFormalRuleFilter(
                            searchValue
                                .uppercase()
                                .replace("LIZENZVERTRAG", FormalRule.LICENCE_CONTRACT.toString())
                                .replace("OPEN-CONTENT-LICENSE", FormalRule.OPEN_CONTENT_LICENCE.toString())
                                .replace("ZBW-NUTZUNGSVEREINBARUNG", FormalRule.ZBW_USER_AGREEMENT.toString()),
                        )
                    "nor" ->
                        QueryParameterParser.parseNoRightInformationFilter(
                            when (searchValue.lowercase()) {
                                "on" -> "true"
                                else -> null
                            },
                        )
                    "tpl" ->
                        QueryParameterParser.parseTemplateNameFilter(
                            searchValue,
                        )
                    else -> null
                }
            } catch (iae: IllegalArgumentException) {
                null
            }

        fun filtersToString(
            filters: List<SearchFilter>,
            searchTerm: String? = null,
        ): String {
            val filtersAsString =
                filters
                    .joinToString(separator = " & ") { filter: SearchFilter ->
                        filter.toString()
                    }.takeIf { it.isNotBlank() }

            return listOfNotNull(
                fixPointReduce(filters, searchTerm)
                    ?.takeIf { it.isNotBlank() },
                filtersAsString,
            ).joinToString(separator = " & ")
        }

        private fun fixPointReduce(
            filters: List<SearchFilter>,
            searchTerm: String?,
        ): String? {
            if (searchTerm == null) {
                return null
            }
            val reducedSearchTerm = fixPointHelper(filters, searchTerm)
            return if (reducedSearchTerm == searchTerm) {
                searchTerm
            } else {
                fixPointReduce(
                    filters,
                    reducedSearchTerm,
                )
            }
        }

        fun fixPointHelper(
            filters: List<SearchFilter>,
            searchTerm: String,
        ): String =
            filters.fold(searchTerm) { acc, f ->
                val filterS = f.toString()
                if (acc == filterS) {
                    ""
                } else {
                    acc.substringBefore(" & " + f.toString())
                }
            }
    }
}

abstract class MetadataSearchFilter(
    dbColumnName: String,
) : SearchFilter(dbColumnName)

abstract class TSVectorMetadataSearchFilter(
    dbColumnName: String,
    private val value: String,
) : MetadataSearchFilter(dbColumnName) {
    override fun toWhereClause(): String = "($dbColumnName @@ $SQL_FUNC_TO_TS_QUERY(?) AND $dbColumnName is not null)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        preparedStatement.setString(localCounter++, prepareValue(value))
        return localCounter
    }

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun toSQLString(): String = value

    companion object {
        private const val SQL_FUNC_TO_TS_QUERY = "to_tsquery"
        private val LOGICAL_OPERATIONS = setOf("|", "&", "(", ")")

        fun prepareValue(v: String): String =
            insertDefaultAndOperator(
                escapeSpecialChars(v),
            )

        private fun escapeSpecialChars(v: String): String =
            v
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("&", "\\&")
                .replace("|", "\\|")

        private fun insertDefaultAndOperator(v: String): String {
            val tokens: List<String> = v.split("\\s+".toRegex())
            return List(tokens.size) { idx ->
                if (idx == 0) {
                    return@List listOf(tokens[0])
                }
                if (!LOGICAL_OPERATIONS.contains(tokens[idx]) &&
                    !LOGICAL_OPERATIONS.contains(tokens[idx - 1])
                ) {
                    return@List listOf("&", tokens[idx])
                } else {
                    return@List listOf(tokens[idx])
                }
            }.flatten().joinToString(separator = " ") { s: String ->
                s.replace(":", "\\:").replace("\\\\:", "\\:")
            } // Filter out :
        }
    }
}

class TitleFilter(
    val title: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_TITLE,
        value = title,
    ) {
    override fun getFilterType(): FilterType = FilterType.TITLE
}

class CommunityNameFilter(
    communityName: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_COMMUNITY,
        value = communityName,
    ) {
    override fun getFilterType(): FilterType = FilterType.COMMUNITY_NAME
}

class CollectionNameFilter(
    collectionName: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_COLLECTION,
        value = collectionName,
    ) {
    override fun getFilterType(): FilterType = FilterType.COLLECTION_NAME
}

class HandleFilter(
    handleName: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_HANDLE,
        value = handleName,
    ) {
    override fun getFilterType(): FilterType = FilterType.HANDLE
}

class CommunityHandleFilter(
    communityHandle: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_COMMUNITY_HANDLE,
        value = communityHandle,
    ) {
    override fun getFilterType(): FilterType = FilterType.COMMUNITY_HANDLE
}

class CollectionHandleFilter(
    collectionHandle: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_COLLECTION_HANDLE,
        value = collectionHandle,
    ) {
    override fun getFilterType(): FilterType = FilterType.COLLECTION_HANDLE
}

class SubcommunityHandleFilter(
    subcommunityHandle: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_SUBCOMMUNITY_HANDLE,
        value = subcommunityHandle,
    ) {
    override fun getFilterType(): FilterType = FilterType.SUB_COMMUNITY_HANDLE
}

class SubcommunityNameFilter(
    subcommunityName: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_SUBCOMMUNITY_NAME,
        value = subcommunityName,
    ) {
    override fun getFilterType(): FilterType = FilterType.SUB_COMMUNITY_NAME
}

class MetadataIdFilter(
    metadataId: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_METADATA_ID,
        value = metadataId,
    ) {
    override fun getFilterType(): FilterType = FilterType.METADATA_ID
}

class LicenceUrlFilter(
    licenceUrl: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_LICENCE_URL,
        value = licenceUrl,
    ) {
    override fun getFilterType(): FilterType = FilterType.LICENCE_URL
}

class PublicationDateFilter(
    val fromYear: Int,
    val toYear: Int,
) : MetadataSearchFilter(
        DatabaseConnector.COLUMN_METADATA_PUBLICATION_DATE,
    ) {
    private val fromDate = LocalDate.of(fromYear, 1, 1)
    private val toDate = LocalDate.of(toYear, 12, 31)

    override fun toWhereClause(): String = "($dbColumnName >= ? AND $dbColumnName <= ? AND $dbColumnName is not null)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setDate(counter, Date.valueOf(fromDate))
        preparedStatement.setDate(counter + 1, Date.valueOf(toDate))
        return counter + 2
    }

    override fun toString(): String = "${getFilterType().keyAlias}:$fromYear-$toYear"

    override fun toSQLString(): String = "$fromYear-$toYear"

    override fun getFilterType(): FilterType = FilterType.PUBLICATION_DATE

    companion object {
        const val MIN_YEAR = 1800
        const val MAX_YEAR = 2200

        fun fromString(s: String?): PublicationDateFilter? = QueryParameterParser.parsePublicationDateFilter(s)
    }
}

class PublicationTypeFilter(
    val publicationTypes: List<PublicationType>,
) : MetadataSearchFilter(
        DatabaseConnector.COLUMN_METADATA_PUBLICATION_TYPE,
    ) {
    override fun toWhereClause(): String =
        publicationTypes.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "LOWER($dbColumnName) = LOWER(?)"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        publicationTypes.forEach {
            preparedStatement.setString(localCounter++, it.toString())
        }
        return localCounter
    }

    override fun toString(): String = "${getFilterType().keyAlias}:\"${publicationTypes.joinToString(separator = ",")}\""

    override fun toSQLString(): String = publicationTypes.joinToString(separator = ",")

    override fun getFilterType(): FilterType = FilterType.PUBLICATION_TYPE

    companion object {
        fun fromString(s: String?): PublicationTypeFilter? = QueryParameterParser.parsePublicationTypeFilter(s)
    }
}

class PaketSigelFilter(
    val paketSigels: List<String>,
) : MetadataSearchFilter(
        DatabaseConnector.COLUMN_METADATA_PAKET_SIGEL,
    ) {
    override fun toWhereClause(): String =
        paketSigels.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "LOWER($dbColumnName) = LOWER(?) AND $dbColumnName is not null"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        paketSigels.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun toSQLString(): String = paketSigels.joinToString(separator = ",")

    override fun getFilterType(): FilterType = FilterType.PAKET_SIGEL

    companion object {
        fun fromString(s: String?): PaketSigelFilter? = QueryParameterParser.parsePaketSigelFilter(s)
    }
}

class ZDBIdFilter(
    val zdbIds: List<String>,
) : MetadataSearchFilter(
        DatabaseConnector.COLUMN_METADATA_ZDB_ID_JOURNAL,
    ) {
    override fun toWhereClause(): String =
        zdbIds.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "(LOWER($dbColumnName) = LOWER(?) AND $dbColumnName is not null) OR " +
                "(LOWER($COLUMN_METADATA_ZDB_ID_SERIES) = LOWER(?) AND $COLUMN_METADATA_ZDB_ID_SERIES is not null)"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        zdbIds.forEach {
            preparedStatement.setString(localCounter++, it)
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun toSQLString(): String = zdbIds.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.ZDB_ID

    companion object {
        fun fromString(s: String?): ZDBIdFilter? = QueryParameterParser.parseZDBIdFilter(s)
    }
}

class SeriesFilter(
    val seriesNames: List<String>,
) : MetadataSearchFilter(
        DatabaseConnector.COLUMN_METADATA_IS_PART_OF_SERIES,
    ) {
    override fun toWhereClause(): String =
        seriesNames.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "LOWER($dbColumnName) = LOWER(?) and $dbColumnName is not null"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        seriesNames.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun getFilterType(): FilterType = FilterType.SERIES

    override fun toSQLString(): String = seriesNames.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    companion object {
        fun fromString(s: String?): SeriesFilter? = QueryParameterParser.parseSeriesFilter(s)
    }
}

/**
 * All right related filter options.
 */
abstract class RightSearchFilter(
    dbColumnName: String,
) : SearchFilter(dbColumnName) {
    companion object {
        const val WHERE_REQUIRE_RIGHT_ID = "${ALIAS_ITEM_RIGHT}.${DatabaseConnector.COLUMN_RIGHT_ID} IS NOT NULL"
    }
}

class AccessStateFilter(
    val accessStates: List<AccessState>,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_ACCESS_STATE) {
    override fun toWhereClause(): String =
        "(" +
            accessStates.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
                "($dbColumnName = ? AND $dbColumnName is not null)"
            } +
            " AND $WHERE_REQUIRE_RIGHT_ID)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        accessStates.forEach {
            preparedStatement.setString(localCounter++, it.toString())
        }
        return localCounter
    }

    override fun getFilterType(): FilterType = FilterType.ACCESS

    override fun toSQLString(): String = accessStates.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    companion object {
        fun fromString(s: String?): AccessStateFilter? = QueryParameterParser.parseAccessStateFilter(s)
    }
}

class TemporalValidityFilter(
    val temporalValidity: List<TemporalValidity>,
) : RightSearchFilter("") {
    override fun toWhereClause(): String =
        "(" +
            temporalValidity.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
                when (it) {
                    TemporalValidity.FUTURE -> "$COLUMN_RIGHT_START_DATE > ?"
                    TemporalValidity.PAST -> "$COLUMN_RIGHT_END_DATE < ?"
                    TemporalValidity.PRESENT ->
                        "$COLUMN_RIGHT_START_DATE <= ?" +
                            " AND $COLUMN_RIGHT_END_DATE >= ?"
                }
            } + " AND ${WHERE_REQUIRE_RIGHT_ID})"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        temporalValidity.forEach {
            preparedStatement.setDate(localCounter++, Date.valueOf(LocalDate.now()))
            if (it == TemporalValidity.PRESENT) {
                preparedStatement.setDate(localCounter++, Date.valueOf(LocalDate.now()))
            }
        }
        return localCounter
    }

    override fun getFilterType(): FilterType = FilterType.TEMPORAL_VALIDITY

    override fun toSQLString(): String = temporalValidity.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    companion object {
        fun fromString(s: String?): TemporalValidityFilter? = QueryParameterParser.parseTemporalValidity(s)
    }
}

/**
 * Filters for items which have a valid right information
 * on a given day.
 */
class RightValidOnFilter(
    val date: LocalDate,
) : RightSearchFilter("") {
    override fun toWhereClause(): String =
        "($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE >= ? AND" +
            " $COLUMN_RIGHT_START_DATE is not null AND" +
            " $COLUMN_RIGHT_END_DATE is not null AND $WHERE_REQUIRE_RIGHT_ID)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        return localCounter
    }

    override fun toSQLString(): String = date.toString()

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.RIGHT_VALID_ON

    companion object {
        fun fromString(s: String?) = QueryParameterParser.parseRightValidOnFilter(s)
    }
}

class StartDateFilter(
    val date: LocalDate,
) : RightSearchFilter(COLUMN_RIGHT_START_DATE) {
    override fun toWhereClause(): String = "($dbColumnName = ? AND $dbColumnName is not null AND $WHERE_REQUIRE_RIGHT_ID)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setDate(counter, Date.valueOf(date))
        return counter + 1
    }

    override fun toSQLString(): String = date.toString()

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.START_DATE

    companion object {
        fun fromString(s: String?): StartDateFilter? = QueryParameterParser.parseStartDateFilter(s)
    }
}

class EndDateFilter(
    val date: LocalDate,
) : RightSearchFilter(COLUMN_RIGHT_END_DATE) {
    override fun toWhereClause(): String = "($dbColumnName = ? AND $dbColumnName is not null AND $WHERE_REQUIRE_RIGHT_ID)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setDate(counter, Date.valueOf(date))
        return counter + 1
    }

    override fun toSQLString(): String = date.toString()

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.END_DATE

    companion object {
        fun fromString(s: String?): EndDateFilter? = QueryParameterParser.parseEndDateFilter(s)
    }
}

class TemplateNameFilter(
    val templateNames: List<String>,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_TEMPLATE_NAME) {
    override fun toWhereClause(): String =
        "(${DatabaseConnector.COLUMN_RIGHT_IS_TEMPLATE} = true AND " +
            templateNames.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
                "(LOWER(${ALIAS_ITEM_RIGHT}.$dbColumnName) = LOWER(?) AND ${ALIAS_ITEM_RIGHT}.$dbColumnName is not null)"
            } + " AND $WHERE_REQUIRE_RIGHT_ID)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        templateNames.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun getFilterType(): FilterType = FilterType.TEMPLATE_NAME

    override fun toSQLString(): String = templateNames.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    companion object {
        fun fromString(s: String?): TemplateNameFilter? = QueryParameterParser.parseTemplateNameFilter(s)
    }
}

class FormalRuleFilter(
    val formalRules: List<FormalRule>,
) : RightSearchFilter("") {
    override fun toWhereClause(): String =
        formalRules.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            when (it) {
                FormalRule.LICENCE_CONTRACT -> "${DatabaseConnector.COLUMN_RIGHT_LICENCE_CONTRACT} <> ''"
                FormalRule.ZBW_USER_AGREEMENT -> "${DatabaseConnector.COLUMN_RIGHT_ZBW_USER_AGREEMENT} = true"
                FormalRule.OPEN_CONTENT_LICENCE ->
                    "(${DatabaseConnector.COLUMN_RIGHT_OPEN_CONTENT_LICENCE} <> '' OR" +
                        " ${DatabaseConnector.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE} = true OR" +
                        " ${DatabaseConnector.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL} <> '' OR" +
                        " ${DatabaseConnector.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE} = true)"
            }
        } + " AND $WHERE_REQUIRE_RIGHT_ID"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int = counter

    override fun toSQLString(): String = formalRules.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.FORMAL_RULE

    companion object {
        fun fromString(s: String?): FormalRuleFilter? = QueryParameterParser.parseFormalRuleFilter(s)
    }
}

class NoRightInformationFilter : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_ID) {
    override fun toWhereClause(): String = "${ALIAS_ITEM_RIGHT}.${DatabaseConnector.COLUMN_RIGHT_ID} IS NULL"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int = counter

    override fun toSQLString(): String = "true"

    override fun toString(): String = "${getFilterType().keyAlias}:on"

    override fun getFilterType(): FilterType = FilterType.NO_RIGHTS

    companion object {
        fun fromString(s: String?): NoRightInformationFilter? = QueryParameterParser.parseNoRightInformationFilter(s)
    }
}

enum class FilterType(
    val keyAlias: String,
) {
    ACCESS("acc"),
    COLLECTION_HANDLE("hdlcol"),
    COLLECTION_NAME("col"),
    COMMUNITY_HANDLE("hdlcom"),
    COMMUNITY_NAME("com"),
    END_DATE("zge"),
    FORMAL_RULE("reg"),
    HANDLE("hdl"),
    LICENCE_URL("lur"),
    METADATA_ID("metadataid"),
    NO_RIGHTS("nor"),
    PUBLICATION_DATE("jah"),
    PUBLICATION_TYPE("typ"),
    PAKET_SIGEL("sig"),
    TEMPLATE_NAME("tpl"),
    RIGHT_VALID_ON("zgp"),
    SERIES("ser"),
    START_DATE("zgb"),
    SUB_COMMUNITY_NAME("subcom"),
    SUB_COMMUNITY_HANDLE("hdlsubcom"),
    TEMPORAL_VALIDITY("zga"),
    TITLE("tit"),
    ZDB_ID("zdb"),
}
