package de.zbw.business.lori.server.utils

import de.zbw.business.lori.server.type.SEAnd
import de.zbw.business.lori.server.type.SENot
import de.zbw.business.lori.server.type.SENotPar
import de.zbw.business.lori.server.type.SEOr
import de.zbw.business.lori.server.type.SEPar
import de.zbw.business.lori.server.type.SEVariable
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchKey
import de.zbw.business.lori.server.type.SearchPair

object SearchExpressionResolution {
    fun resolveSearchExpression(expression: SearchExpression): String =
        when (expression) {
            is SEAnd -> "${resolveSearchExpression(expression.left)} AND ${resolveSearchExpression(expression.right)}"
            is SEOr -> "${resolveSearchExpression(expression.left)} OR ${resolveSearchExpression(expression.right)}"
            is SENot -> "NOT ${resolveSearchExpression(expression.body)}"
            is SEVariable -> expression.searchPair.toWhereClause()
            is SEPar -> "(${resolveSearchExpression(expression.body)})"
            is SENotPar -> "NOT (${resolveSearchExpression(expression.body)})"
        }

    /**
     * When searching for ZDB-IDs two different fields in the database should be considered.
     */
    fun extendZDBSearches(expression: SearchExpression): SearchExpression =
        when (expression) {
            is SEAnd -> SEAnd(extendZDBSearches(expression.left), extendZDBSearches(expression.right))
            is SENot -> SENot(extendZDBSearches(expression.body))
            is SENotPar -> SENot(extendZDBSearches(expression.body))
            is SEOr -> SEOr(extendZDBSearches(expression.left), extendZDBSearches(expression.right))
            is SEPar -> SEPar(extendZDBSearches(expression.body))
            is SEVariable ->
                when (expression.searchPair.key) {
                    SearchKey.ZDB_ID ->
                        SEPar(
                            SEOr(
                                expression,
                                SEVariable(SearchPair(SearchKey.ZDB_ID_SERIES, expression.searchPair.values)),
                            ),
                        )

                    else -> expression
                }
        }

    fun resolveSearchExpressionCoalesce(
        expression: SearchExpression,
        columnName: String = "score",
    ): String {
        val searchPairs = getSearchPairs(expression)
        return searchPairs.joinToString(
            prefix = "(",
            postfix = ")",
            separator = " + ",
        ) { pair ->
            pair.getCoalesce()
        } + "/${searchPairs.size} as $columnName"
    }

    fun getSearchPairs(expression: SearchExpression): List<SearchPair> =
        when (expression) {
            is SEAnd -> getSearchPairs(expression.left) + getSearchPairs(expression.right)
            is SENot -> getSearchPairs(expression.body)
            is SEPar -> getSearchPairs(expression.body)
            is SEOr -> getSearchPairs(expression.left) + getSearchPairs(expression.right)
            is SEVariable -> listOf(expression.searchPair)
            is SENotPar -> getSearchPairs(expression.body)
        }

    fun negateSearchExpression(expression: SearchExpression?): SearchExpression? = expression?.let { SENot(expression) }

    fun conjungateSearchExpressions(
        expr1: SearchExpression?,
        expr2: SearchExpression?,
    ): SearchExpression? =
        if (expr1 == null && expr2 == null) {
            null
        } else if (expr1 == null) {
            expr2
        } else if (expr2 == null) {
            expr1
        } else {
            SEAnd(expr1, expr2)
        }
}
