package de.zbw.business.lori.server.type

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import de.zbw.business.lori.server.LoriServerBackend

sealed class SearchExpression

data class Variable(val searchPair: SearchPair) : SearchExpression()
data class Not(val body: SearchExpression) : SearchExpression()
data class And(val left: SearchExpression, val right: SearchExpression) : SearchExpression()
data class Or(val left: SearchExpression, val right: SearchExpression) : SearchExpression()

object SearchGrammar : Grammar<SearchExpression>() {
    val id by regexToken(LoriServerBackend.SEARCH_KEY_REGEX)
    private val lpar by literalToken("(")
    private val rpar by literalToken(")")
    val not by literalToken("!")
    val and by literalToken("&")
    val or by literalToken("|")
    val ws by regexToken("\\s+", ignore = true)

    private val negation by -not * parser(this::term) map { Not(it) }
    private val bracedExpression by -lpar * parser(this::orChain) * -rpar

    private val term: Parser<SearchExpression> by
        (id map { searchPairInQuery ->
            LoriServerBackend.parseValidSearchPairs(searchPairInQuery.text).takeIf{ it.isNotEmpty() }
                ?.let {
                    Variable(it.first())
                }?: throw ParseError("Invalid Search Pair found in $searchPairInQuery")
            }) or
        negation or
        bracedExpression

    private val andChain: Parser<SearchExpression> by leftAssociative(term, and) { a, _, b -> And(a, b) }
    private val orChain by leftAssociative(andChain, or) { a, _, b -> Or(a, b) }

    override val rootParser by orChain
}

class ParseError(message: String) : Exception(message)
