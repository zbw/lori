package de.zbw.business.lori.server

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.TSVectorMetadataSearchFilter.Companion.SQL_FUNC_TO_TS_QUERY
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_END_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_START_DATE
import de.zbw.persistence.lori.server.MetadataDB
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_HANDLE
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_IS_PART_OF_SERIES
import de.zbw.persistence.lori.server.RightDB
import de.zbw.persistence.lori.server.SearchDB.Companion.ALIAS_ITEM_METADATA
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
                when (searchKey.lowercase()) {
                    "acc" -> QueryParameterParser.parseAccessStateFilter(searchValue.uppercase())
                    "com" ->
                        CommunityNameFilter(searchValue)
                    "col" ->
                        CollectionNameFilter(searchValue)
                    "doi" -> QueryParameterParser.parseDoiFilter(searchValue)
                    "isb" -> QueryParameterParser.parseISBNFilter(searchValue)
                    "hdl" -> {
                        if (searchValue.split(",".toRegex()).size > 1) {
                            HandlesFilter(searchValue.split(",".toRegex()))
                        } else {
                            HandleFilter(searchValue)
                        }
                    }
                    "sig" -> QueryParameterParser.parsePaketSigelFilter(searchValue)
                    "tit" ->
                        TitleFilter(searchValue)
                    "zdb" -> QueryParameterParser.parseZDBIdFilter(searchValue)
                    "hdlcol" ->
                        CollectionHandleFilter(searchValue)
                    "hdlcom" ->
                        CommunityHandleFilter(
                            searchValue,
                        )
                    "hdlsubcom" ->
                        SubcommunityHandleFilter(
                            searchValue,
                        )
                    "rightid" -> QueryParameterParser.parseRightIdFilter(searchValue)
                    "lur" -> QueryParameterParser.parseLicenceUrlFilter(searchValue)
                    "luk" -> QueryParameterParser.parseLicenceUrlLUKFilter(searchValue)
                    "ppn" -> QueryParameterParser.parsePPNFilter(searchValue)
                    "subcom" ->
                        SubcommunityNameFilter(
                            searchValue,
                        )
                    "ser" -> QueryParameterParser.parseSeriesFilter(searchValue)
                    "typ" ->
                        QueryParameterParser.parsePublicationTypeFilter(searchValue)
                    "jah" -> QueryParameterParser.parsePublicationYearFilter(searchValue)
                    "zgp" -> QueryParameterParser.parseRightValidOnFilter(searchValue)
                    "zgb" -> QueryParameterParser.parseStartDateFilter(searchValue)
                    "zge" -> QueryParameterParser.parseEndDateFilter(searchValue)
                    "reg" ->
                        QueryParameterParser.parseFormalRuleFilter(
                            searchValue
                                .uppercase()
                                .replace("LIZENZVERTRAG", FormalRule.LICENCE_CONTRACT.toString())
                                .replace("CC-LIZENZ OHNE EINSCHRÄNKUNG", FormalRule.CC_LICENCE_NO_RESTRICTION.toString())
                                .replace("ZBW-NUTZUNGSVEREINBARUNG", FormalRule.ZBW_USER_AGREEMENT.toString()),
                        )
                    "nor" ->
                        QueryParameterParser.parseNoRightInformationFilter(
                            when (searchValue) {
                                "on" -> "true"
                                else -> null
                            },
                        )
                    "man" ->
                        QueryParameterParser.parseManualRightFilter(
                            when (searchValue) {
                                "on" -> "true"
                                else -> null
                            },
                        )
                    "tpl" ->
                        QueryParameterParser.parseTemplateNameFilter(
                            searchValue,
                        )
                    "acd" ->
                        QueryParameterParser.parseAccessStateOnDate(
                            searchValue,
                        )
                    else -> null
                }
            } catch (iae: IllegalArgumentException) {
                null
            }

        fun filtersToString(filters: List<SearchFilter>): String =
            filters
                .joinToString(separator = " & ") { filter: SearchFilter ->
                    filter.toString()
                }.takeIf { it.isNotBlank() } ?: ""
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
        internal const val SQL_FUNC_TO_TS_QUERY = "to_tsquery"
        private val LOGICAL_OPERATIONS = setOf("|", "&", "(", ")")

        fun prepareValue(v: String): String =
            escapeWildcards(
                insertDefaultAndOperator(
                    escapeSpecialChars(v),
                ),
            )

        fun escapeWildcards(s: String): String =
            if (s.last() == '*') {
                s.dropLast(1) + ":*"
            } else {
                s
            }

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

class LicenceUrlFilterLUK(
    licenceURL: String,
) : TSVectorMetadataSearchFilter(
        dbColumnName = MetadataDB.TS_LICENCE_URL,
        value = licenceURL,
    ) {
    override fun toWhereClause(): String = "($dbColumnName @@ $SQL_FUNC_TO_TS_QUERY('simple', ?) AND $dbColumnName is not null)"

    override fun getFilterType(): FilterType = FilterType.LICENCE_URL_LUK
}

class LicenceUrlFilter(
    val licenceUrl: String,
) : MetadataSearchFilter(
        MetadataDB.COLUMN_METADATA_LICENCE_URL_FILTER,
    ) {
    override fun toWhereClause(): String = "(LOWER($dbColumnName) ILIKE ? AND $dbColumnName is not null)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setString(counter, licenceUrl)
        return counter + 1
    }

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun toSQLString(): String = licenceUrl

    override fun getFilterType(): FilterType = FilterType.LICENCE_URL

    companion object {
        fun fromString(s: String?): LicenceUrlFilter? = s?.let { QueryParameterParser.parseLicenceUrlFilter(it) }
    }
}

class PPNFilter(
    val ppn: String,
) : MetadataSearchFilter(
        MetadataDB.COLUMN_METADATA_PPN,
    ) {
    override fun toWhereClause(): String = "(LOWER($dbColumnName) ILIKE ? AND $dbColumnName is not null)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setString(counter, ppn)
        return counter + 1
    }

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun toSQLString(): String = ppn

    override fun getFilterType(): FilterType = FilterType.PPN

    companion object {
        fun fromString(s: String?): PPNFilter? = s?.let { QueryParameterParser.parsePPNFilter(it) }
    }
}

class DOIFilter(
    val dois: List<String>,
) : MetadataSearchFilter(
        MetadataDB.COLUMN_METADATA_DOI,
    ) {
    override fun toWhereClause(): String =
        dois.joinToString(prefix = "(", postfix = " AND $dbColumnName is not null)", separator = " AND ") {
            "(" +
                "EXISTS (" +
                "SELECT 1" +
                " FROM unnest($dbColumnName) AS element" +
                " WHERE (lower(element) ILIKE ?)" +
                ")" +
                ")"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        dois.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun toSQLString(): String = dois.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.DOI

    companion object {
        fun fromString(s: String?): DOIFilter? = QueryParameterParser.parseDoiFilter(s)
    }
}

class ISBNFilter(
    val isbns: List<String>,
) : MetadataSearchFilter(
        MetadataDB.COLUMN_METADATA_ISBN,
    ) {
    override fun toWhereClause(): String =
        isbns.joinToString(prefix = "(", postfix = " AND $dbColumnName is not null)", separator = " AND ") {
            "(" +
                "EXISTS (" +
                "SELECT 1" +
                " FROM unnest($dbColumnName) AS element" +
                " WHERE (lower(element) ILIKE ?)" +
                ")" +
                ")"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        isbns.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun toSQLString(): String = isbns.joinToString(separator = ",")

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun getFilterType(): FilterType = FilterType.ISBN

    companion object {
        fun fromString(s: String?): ISBNFilter? = QueryParameterParser.parseISBNFilter(s)
    }
}

class PublicationYearFilter(
    val fromYear: Int?,
    val toYear: Int?,
) : MetadataSearchFilter(
        MetadataDB.COLUMN_METADATA_PUBLICATION_YEAR,
    ) {
    override fun toWhereClause(): String =
        if (fromYear == null && toYear == null) {
            ""
        } else if (fromYear == null) {
            "($dbColumnName <= ? AND $dbColumnName is not null)"
        } else if (toYear == null) {
            "($dbColumnName >= ? AND $dbColumnName is not null)"
        } else {
            "($dbColumnName >= ? AND $dbColumnName <= ? AND $dbColumnName is not null)"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int =
        if (fromYear == null && toYear == null) {
            counter
        } else if (fromYear == null && toYear != null) {
            preparedStatement.setInt(counter, toYear)
            counter + 1
        } else if (toYear == null && fromYear != null) {
            preparedStatement.setInt(counter, fromYear)
            counter + 1
        } else {
            preparedStatement.setInt(counter, fromYear!!)
            preparedStatement.setInt(counter + 1, toYear!!)
            counter + 2
        }

    override fun toString(): String {
        val fromYearString =
            fromYear?.toString() ?: ""
        val toYearString =
            toYear?.toString() ?: ""
        return "${getFilterType().keyAlias}:$fromYearString-$toYearString"
    }

    override fun toSQLString(): String {
        val fromYearString =
            fromYear?.toString() ?: ""
        val toYearString =
            toYear?.toString() ?: ""
        return "$fromYearString-$toYearString"
    }

    override fun getFilterType(): FilterType = FilterType.PUBLICATION_YEAR

    companion object {
        fun fromString(s: String?): PublicationYearFilter? = QueryParameterParser.parsePublicationYearFilter(s)
    }
}

class PublicationTypeFilter(
    val publicationTypes: List<PublicationType>,
) : MetadataSearchFilter(
        MetadataDB.COLUMN_METADATA_PUBLICATION_TYPE,
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
        MetadataDB.COLUMN_METADATA_PAKET_SIGEL,
    ) {
    override fun toWhereClause(): String =
        paketSigels.joinToString(prefix = "(", postfix = " AND $dbColumnName is not null)", separator = " AND ") {
            "(" +
                "EXISTS (" +
                "SELECT 1" +
                " FROM unnest($dbColumnName) AS element" +
                " WHERE (element ILIKE ?)" +
                ")" +
                ")"
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
        MetadataDB.COLUMN_METADATA_ZDB_IDS,
    ) {
    override fun toWhereClause(): String =
        zdbIds.joinToString(prefix = "(", postfix = " AND $dbColumnName is not null)", separator = " AND ") {
            "(" +
                "EXISTS (" +
                "SELECT 1" +
                " FROM unnest($dbColumnName) AS element" +
                " WHERE (lower(element) ILIKE ?)" +
                ")" +
                ")"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        zdbIds.forEach {
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
        COLUMN_METADATA_IS_PART_OF_SERIES,
    ) {
    override fun toWhereClause(): String =
        seriesNames.joinToString(prefix = "(", postfix = " AND $dbColumnName is not null)", separator = " AND ") {
            "(" +
                "EXISTS (" +
                "SELECT 1" +
                " FROM unnest($dbColumnName) AS element" +
                " WHERE (lower(element) ILIKE ?)" +
                ")" +
                ")"
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

class HandlesFilter(
    val handles: List<String>,
) : MetadataSearchFilter(
        dbColumnName = COLUMN_METADATA_HANDLE,
    ) {
    override fun toWhereClause(): String =
        "${ALIAS_ITEM_METADATA}.$dbColumnName IN " +
            handles.joinToString(separator = ",", prefix = "(", postfix = ")") {
                "?"
            }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        handles.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

    override fun toSQLString(): String = handles.joinToString(separator = ",") { it }

    override fun getFilterType(): FilterType = FilterType.HANDLE
}

/**
 * All right related filter options.
 */
abstract class RightSearchFilter(
    dbColumnName: String,
) : SearchFilter(dbColumnName) {
    override fun toString(): String = "${getFilterType().keyAlias}:\"${toSQLString()}\""

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

class AccessStateOnDateFilter(
    val date: LocalDate,
    val accessState: AccessState?,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_ACCESS_STATE) {
    override fun toWhereClause(): String =
        if (accessState != null) {
            "((($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE >= ? AND" +
                " $COLUMN_RIGHT_START_DATE IS NOT NULL AND" +
                " $COLUMN_RIGHT_END_DATE IS NOT NULL AND $WHERE_REQUIRE_RIGHT_ID) OR" +
                " ($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE IS NULL AND" +
                " $COLUMN_RIGHT_START_DATE IS NOT NULL AND $WHERE_REQUIRE_RIGHT_ID))" +
                " AND ($dbColumnName = ? AND $dbColumnName is not null))"
        } else {
            "(($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE >= ? AND" +
                " $COLUMN_RIGHT_START_DATE IS NOT NULL AND" +
                " $COLUMN_RIGHT_END_DATE IS NOT NULL AND $WHERE_REQUIRE_RIGHT_ID) OR" +
                " ($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE IS NULL AND" +
                " $COLUMN_RIGHT_START_DATE IS NOT NULL AND $WHERE_REQUIRE_RIGHT_ID))"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        if (accessState != null) {
            preparedStatement.setString(localCounter++, accessState.toString())
        }
        return localCounter
    }

    override fun toSQLString(): String =
        if (accessState != null) {
            "$accessState+$date"
        } else {
            "$date"
        }

    override fun getFilterType(): FilterType = FilterType.ACCESS_ON_DATE

    companion object {
        fun fromString(s: String?) = QueryParameterParser.parseAccessStateOnDate(s)
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
        "(($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE >= ? AND" +
            " $COLUMN_RIGHT_START_DATE IS NOT NULL AND" +
            " $COLUMN_RIGHT_END_DATE IS NOT NULL AND $WHERE_REQUIRE_RIGHT_ID) OR" +
            " ($COLUMN_RIGHT_START_DATE <= ? AND $COLUMN_RIGHT_END_DATE IS NULL AND" +
            " $COLUMN_RIGHT_START_DATE IS NOT NULL AND $WHERE_REQUIRE_RIGHT_ID))"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        return localCounter
    }

    override fun toSQLString(): String = date.toString()

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

class RightIdFilter(
    val rightIds: List<String>,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_ID) {
    override fun toWhereClause(): String =
        "(" +
            rightIds.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
                "($ALIAS_ITEM_RIGHT.$dbColumnName = ? AND $ALIAS_ITEM_RIGHT.$dbColumnName is not null)"
            } + " AND $WHERE_REQUIRE_RIGHT_ID)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        rightIds.forEach {
            preparedStatement.setString(localCounter++, it)
        }
        return localCounter
    }

    override fun getFilterType(): FilterType = FilterType.RIGHT_ID

    override fun toSQLString(): String = rightIds.joinToString(separator = ",")

    companion object {
        fun fromString(s: String?): RightIdFilter? = QueryParameterParser.parseRightIdFilter(s)
    }
}

class TemplateNameFilter(
    val templateNames: List<String>,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_TEMPLATE_NAME) {
    override fun toWhereClause(): String =
        "(${DatabaseConnector.COLUMN_RIGHT_IS_TEMPLATE} = true" +
            " AND ${ALIAS_ITEM_RIGHT}.$dbColumnName is not null" +
            " AND $WHERE_REQUIRE_RIGHT_ID" +
            templateNames.joinToString(prefix = " AND (", postfix = ")", separator = " AND ") {
                "LOWER($ALIAS_ITEM_RIGHT.$dbColumnName) ILIKE ?"
            } + ")"

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
                FormalRule.CC_LICENCE_NO_RESTRICTION ->
                    "${DatabaseConnector.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE} = false AND " +
                        "${MetadataDB.TS_LICENCE_URL} @@ $SQL_FUNC_TO_TS_QUERY('simple', 'creativecommons') AND " +
                        "${MetadataDB.TS_LICENCE_URL} @@ $SQL_FUNC_TO_TS_QUERY('simple', 'licenses') AND " +
                        "${MetadataDB.TS_LICENCE_URL} is not null"

                FormalRule.COPYRIGHT_EXCEPTION_RISKFREE -> "${ALIAS_ITEM_RIGHT}.${RightDB.COLUMN_HAS_LEGAL_RISK} = false"
            }
        } + " AND $WHERE_REQUIRE_RIGHT_ID"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int = counter

    override fun toSQLString(): String = formalRules.joinToString(separator = ",")

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

class ManualRightFilter : RightSearchFilter(RightDB.COLUMN_IS_TEMPLATE) {
    override fun toWhereClause(): String = "${ALIAS_ITEM_RIGHT}.$dbColumnName = false AND $WHERE_REQUIRE_RIGHT_ID"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int = counter

    override fun toSQLString(): String = "true"

    override fun toString(): String = "${getFilterType().keyAlias}:on"

    override fun getFilterType(): FilterType = FilterType.MANUAL_RIGHTS

    companion object {
        fun fromString(s: String?): ManualRightFilter? = QueryParameterParser.parseManualRightFilter(s)
    }
}

enum class FilterType(
    val keyAlias: String,
) {
    ACCESS("acc"),
    ACCESS_ON_DATE("acd"),
    COLLECTION_HANDLE("hdlcol"),
    COLLECTION_NAME("col"),
    COMMUNITY_HANDLE("hdlcom"),
    COMMUNITY_NAME("com"),
    DOI("doi"),
    END_DATE("zge"),
    FORMAL_RULE("reg"),
    HANDLE("hdl"),
    ISBN("isb"),
    LICENCE_URL("lur"),
    LICENCE_URL_LUK("luk"),
    MANUAL_RIGHTS("man"),
    NO_RIGHTS("nor"),
    PUBLICATION_YEAR("jah"),
    PUBLICATION_TYPE("typ"),
    PAKET_SIGEL("sig"),
    PPN("ppn"),
    TEMPLATE_NAME("tpl"),
    RIGHT_ID("rightid"),
    RIGHT_VALID_ON("zgp"),
    SERIES("ser"),
    START_DATE("zgb"),
    SUB_COMMUNITY_NAME("subcom"),
    SUB_COMMUNITY_HANDLE("hdlsubcom"),
    TITLE("tit"),
    ZDB_ID("zdb"),
}
