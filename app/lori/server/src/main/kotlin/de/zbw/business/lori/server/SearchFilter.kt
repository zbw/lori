package de.zbw.business.lori.server

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

class PublicationDateFilter(
    val fromYear: Int,
    val toYear: Int,
) : SearchFilter(
    DatabaseConnector.COLUMN_PUBLICATION_DATE,
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
    val publicationFilter: List<PublicationType>
) : SearchFilter(
    DatabaseConnector.COLUMN_PUBLICATION_TYPE,
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
