package de.zbw.business.lori.server.type

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.SearchFilter

/**
 * Search expressions represented as an algebraic datatype.
 *
 * Created on 03-21-2024
 * @author Christian Bay (c.bay@zbw.eu)
 */
sealed class SearchExpression

data class SEVariable(
    val searchFilter: SearchFilter,
) : SearchExpression()

data class SENot(
    val body: SearchExpression,
) : SearchExpression()

data class SENotPar(
    val body: SearchExpression,
) : SearchExpression()

data class SEAnd(
    val left: SearchExpression,
    val right: SearchExpression,
) : SearchExpression()

data class SEOr(
    val left: SearchExpression,
    val right: SearchExpression,
) : SearchExpression()

data class SEPar(
    val body: SearchExpression,
) : SearchExpression()

object SearchGrammar : Grammar<SearchExpression>() {
    val id by regexToken(LoriServerBackend.SEARCH_KEY_REGEX)
    private val lpar by literalToken("(")
    private val rpar by literalToken(")")
    val not by literalToken("!")
    val and by literalToken("&")
    val or by literalToken("|")

    // Even if not used this variable seems to be necessary according to documentation
    val ws by regexToken("\\s+", ignore = true)

    private val negation by -not * parser(this::term) map { SENot(it) }
    private val bracedExpression by -lpar * parser(this::orChain) * -rpar map { SEPar(it) }
    private val bracedNegExpression by -not * -lpar * parser(this::orChain) * -rpar map { SEPar(it) }

    private val term: Parser<SearchExpression> by
        (
            id map { searchPairInQuery: TokenMatch ->
                LoriServerBackend
                    .parseSearchTermToFilters(searchPairInQuery.text)
                    .takeIf { it.isNotEmpty() }
                    ?.let { it: List<SearchFilter> ->
                        SEVariable(it.first())
                    } ?: throw ParsingException("Invalid SearchFilter found in $searchPairInQuery")
            }
        ) or
            negation or
            bracedExpression or
            bracedNegExpression

    private val andChain: Parser<SearchExpression> by leftAssociative(term, and) { a, _, b -> SEAnd(a, b) }
    private val orChain by leftAssociative(andChain, or) { a, _, b -> SEOr(a, b) }

    override val rootParser by orChain
}

class ParsingException(
    message: String,
) : Exception(message)
