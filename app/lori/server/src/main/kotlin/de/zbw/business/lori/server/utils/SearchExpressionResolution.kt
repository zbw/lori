package de.zbw.business.lori.server.utils

import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.SearchFilter
import de.zbw.business.lori.server.type.SEAnd
import de.zbw.business.lori.server.type.SENot
import de.zbw.business.lori.server.type.SENotPar
import de.zbw.business.lori.server.type.SEOr
import de.zbw.business.lori.server.type.SEPar
import de.zbw.business.lori.server.type.SEVariable
import de.zbw.business.lori.server.type.SearchExpression

object SearchExpressionResolution {
    fun resolveSearchExpression(expression: SearchExpression): String =
        when (expression) {
            is SEAnd -> "${resolveSearchExpression(
                expression.left,
            )} AND ${resolveSearchExpression(expression.right)}"
            is SEOr -> "${resolveSearchExpression(
                expression.left,
            )} OR ${resolveSearchExpression(expression.right)}"
            is SENot -> "NOT ${resolveSearchExpression(expression.body)}"
            is SEVariable -> expression.searchFilter.toWhereClause()
            is SEPar -> "(${resolveSearchExpression(expression.body)})"
            is SENotPar -> "NOT (${resolveSearchExpression(expression.body)})"
        }

    fun hasRightQueries(expression: SearchExpression?): Boolean =
        if (expression == null) {
            false
        } else {
            when (expression) {
                is SEAnd ->
                    hasRightQueries(expression.left) || hasRightQueries(expression.right)

                is SEOr -> hasRightQueries(expression.left) || hasRightQueries(expression.right)
                is SENot -> hasRightQueries(expression.body)
                is SEVariable -> expression.searchFilter is RightSearchFilter
                is SEPar -> hasRightQueries(expression.body)
                is SENotPar -> hasRightQueries(expression.body)
            }
        }

    fun getSearchPairs(expression: SearchExpression): List<SearchFilter> =
        when (expression) {
            is SEAnd -> getSearchPairs(expression.left) + getSearchPairs(expression.right)
            is SENot -> getSearchPairs(expression.body)
            is SEPar -> getSearchPairs(expression.body)
            is SEOr -> getSearchPairs(expression.left) + getSearchPairs(expression.right)
            is SEVariable -> listOf(expression.searchFilter)
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
