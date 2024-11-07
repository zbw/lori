package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ConflictType
import de.zbw.persistence.lori.server.RightErrorDB
import java.sql.Date
import java.sql.PreparedStatement
import java.time.LocalDate

abstract class DashboardSearchFilter(
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
}

class DashboardTemplateNameFilter(
    private val templateNames: List<String>,
) : DashboardSearchFilter(RightErrorDB.COLUMN_CONFLICT_BY_CONTEXT) {
    override fun toWhereClause(): String =
        templateNames.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "$dbColumnName = ?"
        }

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
}

class DashboardConflictTypeFilter(
    private val conflictTypes: List<ConflictType>,
) : DashboardSearchFilter(RightErrorDB.COLUMN_CONFLICTING_TYPE) {
    override fun toWhereClause(): String =
        conflictTypes.joinToString(prefix = "(", postfix = ")", separator = " OR ") {
            "$dbColumnName = ?"
        }

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        var localCounter = counter
        conflictTypes.forEach {
            preparedStatement.setString(localCounter++, it.toString())
        }
        return localCounter
    }
}

class DashboardTimeIntervalStartFilter(
    private val startDate: LocalDate,
) : DashboardSearchFilter(RightErrorDB.COLUMN_CREATED_ON) {
    override fun toWhereClause(): String = "($dbColumnName >= ?)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setDate(counter, Date.valueOf(startDate))
        return counter + 1
    }
}

class DashboardTimeIntervalEndFilter(
    private val endDate: LocalDate,
) : DashboardSearchFilter(RightErrorDB.COLUMN_CREATED_ON) {
    override fun toWhereClause(): String = "($dbColumnName < ?)"

    override fun setSQLParameter(
        counter: Int,
        preparedStatement: PreparedStatement,
    ): Int {
        preparedStatement.setDate(counter, Date.valueOf(endDate.plusDays(1)))
        return counter + 1
    }
}
