package de.zbw.business.lori.server.utils

import de.zbw.business.lori.server.type.SEAnd
import de.zbw.business.lori.server.type.SENot
import de.zbw.business.lori.server.type.SENotPar
import de.zbw.business.lori.server.type.SEOr
import de.zbw.business.lori.server.type.SEPar
import de.zbw.business.lori.server.type.SEVariable
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchPair

object SearchExpressionResolution {
    fun resolveSearchExpression(expression: SearchExpression): String = when (expression) {
        is SEAnd -> "${resolveSearchExpression(expression.left)} AND ${resolveSearchExpression(expression.right)}"
        is SEOr -> "${resolveSearchExpression(expression.left)} OR ${resolveSearchExpression(expression.right)}"
        is SENot -> "NOT ${resolveSearchExpression(expression.body)}"
        is SEVariable -> expression.searchPair.toWhereClause()
        is SEPar -> "(${resolveSearchExpression(expression.body)})"
        is SENotPar -> "NOT (${resolveSearchExpression(expression.body)})"
    }

    fun resolveSearchExpressionCoalesce(expression: SearchExpression, columnName: String = "score"): String {
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
}
