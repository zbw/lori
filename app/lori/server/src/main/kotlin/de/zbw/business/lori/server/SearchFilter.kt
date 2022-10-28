package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.TemporalValidity
import de.zbw.persistence.lori.server.DatabaseConnector
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
    abstract fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int
}

abstract class MetadataSearchFilter(
    dbColumnName: String
) : SearchFilter(dbColumnName)

class PublicationDateFilter(
    val fromYear: Int,
    val toYear: Int,
) : MetadataSearchFilter(
    DatabaseConnector.COLUMN_METADATA_PUBLICATION_DATE,
) {
    private val fromDate = LocalDate.of(fromYear, 1, 1)
    private val toDate = LocalDate.of(toYear, 12, 31)

    override fun toWhereClause(): String =
        "$dbColumnName >= ? AND $dbColumnName <= ?"

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        preparedStatement.setDate(counter, Date.valueOf(fromDate))
        preparedStatement.setDate(counter + 1, Date.valueOf(toDate))
        return counter + 2
    }

    companion object {
        const val MIN_YEAR = 1800
        const val MAX_YEAR = 2200
    }
}

class PublicationTypeFilter(
    val publicationFilter: List<PublicationType>,
) : MetadataSearchFilter(
    DatabaseConnector.COLUMN_METADATA_PUBLICATION_TYPE,
) {
    override fun toWhereClause(): String =
        publicationFilter.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "$dbColumnName = ?"
        }

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        var localCounter = counter
        publicationFilter.forEach {
            preparedStatement.setString(localCounter++, it.toString())
        }
        return localCounter
    }
}

abstract class RightSearchFilter(
    dbColumnName: String
) : SearchFilter(dbColumnName)

class AccessStateFilter(
    val accessStates: List<AccessState>,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_ACCESS_STATE) {
    override fun toWhereClause(): String =
        accessStates.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "$dbColumnName = ?"
        }

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        var localCounter = counter
        accessStates.forEach {
            preparedStatement.setString(localCounter++, it.toString())
        }
        return localCounter
    }
}

class TemporalValidityFilter(
    val temporalValidity: List<TemporalValidity>,
) : RightSearchFilter("") {
    override fun toWhereClause(): String =
        temporalValidity.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            when (it) {
                TemporalValidity.FUTURE -> "${DatabaseConnector.COLUMN_RIGHT_START_DATE} > ?"
                TemporalValidity.PAST -> "${DatabaseConnector.COLUMN_RIGHT_END_DATE} < ?"
                TemporalValidity.PRESENT -> "${DatabaseConnector.COLUMN_RIGHT_START_DATE} <= ? AND ${DatabaseConnector.COLUMN_RIGHT_END_DATE} >= ?"
            }
        }

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        var localCounter = counter
        temporalValidity.forEach {
            preparedStatement.setDate(localCounter++, Date.valueOf(LocalDate.now()))
            if (it == TemporalValidity.PRESENT) {
                preparedStatement.setDate(localCounter++, Date.valueOf(LocalDate.now()))
            }
        }
        return localCounter
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
        "${DatabaseConnector.COLUMN_RIGHT_START_DATE} <= ? AND ${DatabaseConnector.COLUMN_RIGHT_END_DATE} >= ?"

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        var localCounter = counter
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        preparedStatement.setDate(localCounter++, Date.valueOf(date))
        return localCounter
    }
}

class StartDateFilter(
    val date: LocalDate,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_START_DATE) {
    override fun toWhereClause(): String =
        "$dbColumnName = ?"

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        preparedStatement.setDate(counter, Date.valueOf(date))
        return counter + 1
    }
}

class EndDateFilter(
    val date: LocalDate,
) : RightSearchFilter(DatabaseConnector.COLUMN_RIGHT_END_DATE) {
    override fun toWhereClause(): String =
        "$dbColumnName = ?"

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int {
        preparedStatement.setDate(counter, Date.valueOf(date))
        return counter + 1
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
        }

    override fun setSQLParameter(counter: Int, preparedStatement: PreparedStatement): Int = counter
}
