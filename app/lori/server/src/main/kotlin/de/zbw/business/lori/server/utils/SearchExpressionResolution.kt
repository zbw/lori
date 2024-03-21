package de.zbw.business.lori.server.utils

import de.zbw.business.lori.server.type.And
import de.zbw.business.lori.server.type.Not
import de.zbw.business.lori.server.type.Or
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchPair
import de.zbw.business.lori.server.type.Variable

object SearchExpressionResolution {
    fun resolveSearchExpression(expression: SearchExpression): String = when (expression) {
        is And -> "(${resolveSearchExpression(expression.left)}) AND ${resolveSearchExpression(expression.right)}"
        is Or -> "(${resolveSearchExpression(expression.left)}) OR ${resolveSearchExpression(expression.right)}"
        is Not -> "NOT ${resolveSearchExpression(expression.body)}"
        is Variable -> expression.searchPair.toWhereClause()
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
            is And -> getSearchPairs(expression.left) + getSearchPairs(expression.right)
            is Not -> getSearchPairs(expression.body)
            is Or -> getSearchPairs(expression.left) + getSearchPairs(expression.right)
            is Variable -> listOf(expression.searchPair)
        }
}
