package de.zbw.business.lori.server.type

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.TitleFilter
import de.zbw.business.lori.server.utils.SearchExpressionResolution.hasRightQueries
import de.zbw.business.lori.server.utils.SearchExpressionResolution.resolveSearchExpression
import io.ktor.http.cio.ParserException
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SearchExpressionTest {
    @DataProvider(name = DATA_FOR_PARSING_SEARCH_QUERY)
    fun createDataForParsing() =
        arrayOf(
            arrayOf(
                "tit:'foo'",
                "SEVariable(searchFilter=tit:\"foo\")",
                false,
                "Single Search Pair",
            ),
            arrayOf(
                "!tit:'foo'",
                "SENot(body=SEVariable(searchFilter=tit:\"foo\"))",
                false,
                "One negation",
            ),
            arrayOf(
                "tit:'foo'|zdb:'123'",
                "SEOr(left=SEVariable(searchFilter=tit:\"foo\"), right=SEVariable(searchFilter=zdb:\"123\"))",
                false,
                "Or search pairs no whitespace",
            ),
            arrayOf(
                "tit:'foo' | zdb:'123'",
                "SEOr(left=SEVariable(searchFilter=tit:\"foo\"), right=SEVariable(searchFilter=zdb:\"123\"))",
                false,
                "Or search pairs",
            ),
            arrayOf(
                "tit:'foo' & zdb:'123'",
                "SEAnd(left=SEVariable(searchFilter=tit:\"foo\"), right=SEVariable(searchFilter=zdb:\"123\"))",
                false,
                "And search pairs",
            ),
            arrayOf(
                "(tit:'foo' & zdb:'123') | hdl:'123'",
                "SEOr(left=SEPar(body=SEAnd(left=SEVariable(searchFilter=tit:\"foo\")," +
                    " right=SEVariable(searchFilter=zdb:\"123\")))," +
                    " right=SEVariable(searchFilter=hdl:\"123\"))",
                false,
                "Or, And, parentheses",
            ),
            arrayOf(
                "com:2764793-6 & (hdl:4633 | zdb:4566)",
                "SEAnd(left=SEVariable(searchFilter=com:\"2764793-6\"), " +
                    "right=SEPar(body=SEOr(left=SEVariable(searchFilter=hdl:\"4633\")," +
                    " right=SEVariable(searchFilter=zdb:\"4566\"))))",
                false,
                "Verify that ) after key:value without \"' works",
            ),
            arrayOf(
                "sig:zdb-33-sfen & (!hdl:11159/86 | !hdl:11159/993)",
                "SEAnd(left=SEVariable(searchFilter=sig:\"zdb-33-sfen\")," +
                    " right=SEPar(body=SEOr(left=SENot(body=SEVariable(searchFilter=hdl:\"11159/86\"))," +
                    " right=SENot(body=SEVariable(searchFilter=hdl:\"11159/993\")))))",
                false,
                "Verify that ) after key:value without \"' works",
            ),
            arrayOf(
                "sig:zdb-33-sfen & (!hdl:11159/86 | !hdl:11159/993)",
                "SEAnd(left=SEVariable(searchFilter=sig:\"zdb-33-sfen\")," +
                    " right=SEPar(body=SEOr(left=SENot(body=SEVariable(searchFilter=hdl:\"11159/86\"))," +
                    " right=SENot(body=SEVariable(searchFilter=hdl:\"11159/993\")))))",
                false,
                "Verify that ) after key:value without \"' works",
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSING_SEARCH_QUERY)
    fun testExpressionParser(
        query: String,
        expected: String,
        throwsException: Boolean,
        details: String,
    ) {
        try {
            when (val expr: ParseResult<SearchExpression> = SearchGrammar.tryParseToEnd(query)) {
                is Parsed -> {
                    assertThat(
                        details,
                        expr.value.toString(),
                        `is`(expected),
                    )
                    assertTrue(!throwsException)
                }

                is ErrorResult -> Assert.fail(expr.toString())
            }
        } catch (pe: ParserException) {
            assertTrue(throwsException)
        }
    }

    @DataProvider(name = DATA_FOR_RESOLVE_SEARCH_EXPRESSION)
    fun createDataForResolveSearchExpression() =
        arrayOf(
            arrayOf(
                "(tit:'foo' & zdb:'123') | hdl:'123'",
                "((ts_title @@ to_tsquery(?) AND ts_title is not null) AND" +
                    " ((LOWER(zdb_id_journal) = LOWER(?) AND zdb_id_journal is not null)" +
                    " OR (LOWER(zdb_id_series) = LOWER(?) AND zdb_id_series is not null)))" +
                    " OR (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null)",
                "zdb key searchs on two fields",
            ),
            arrayOf(
                "sig:zdb-33-sfen & (!hdl:11159/86 | !hdl:11159/993)",
                "(LOWER(paket_sigel) = LOWER(?) AND paket_sigel is not null)" +
                    " AND (NOT (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null)" +
                    " OR NOT (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null))",
                "negation, parenthesis, or, and",
            ),
            arrayOf(
                "sig:zdb-33-sfen & !(hdl:11159/86 & hdl:11159/993)",
                "(LOWER(paket_sigel) = LOWER(?) AND paket_sigel is not null)" +
                    " AND NOT ((ts_hdl @@ to_tsquery(?) AND ts_hdl is not null)" +
                    " AND (ts_hdl @@ to_tsquery(?) AND ts_hdl is not null))",
                "negate term before paranthesis",
            ),
        )

    @Test(dataProvider = DATA_FOR_RESOLVE_SEARCH_EXPRESSION)
    fun resolveSearchExpression(
        query: String,
        expected: String,
        reason: String,
    ) {
        assertThat(
            reason,
            resolveSearchExpression(SearchGrammar.parseToEnd(query)),
            `is`(expected),
        )
    }

    @DataProvider(name = DATA_FOR_HAS_RIGHT_QUERIES)
    fun createDataForHasRightQueries() =
        arrayOf(
            arrayOf(
                SEAnd(
                    SEVariable(
                        AccessStateFilter(listOf(AccessState.OPEN)),
                    ),
                    SEVariable(
                        PublicationYearFilter(2000, 2010),
                    ),
                ),
                true,
                "And with one Right Filter",
            ),
            arrayOf(
                SEAnd(
                    SEVariable(
                        PublicationYearFilter(2000, 2010),
                    ),
                    SEVariable(
                        PublicationYearFilter(2000, 2010),
                    ),
                ),
                false,
                "And with non Right Filter",
            ),
            arrayOf(
                SEOr(
                    SEVariable(
                        AccessStateFilter(listOf(AccessState.OPEN)),
                    ),
                    SEVariable(
                        PublicationYearFilter(2000, 2010),
                    ),
                ),
                true,
                "Or with one Right Filter",
            ),
            arrayOf(
                SENot(
                    SEVariable(
                        AccessStateFilter(listOf(AccessState.OPEN)),
                    ),
                ),
                true,
                "Not with one Right Filter",
            ),
            arrayOf(
                SENot(
                    SEVariable(
                        TitleFilter("foo"),
                    ),
                ),
                false,
                "Not with no Right Filter",
            ),
            arrayOf(
                SENotPar(
                    SEVariable(
                        AccessStateFilter(listOf(AccessState.OPEN)),
                    ),
                ),
                true,
                "Negation before parenthesis with one Right Filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_HAS_RIGHT_QUERIES)
    fun testHasRightQueries(
        searchExpression: SearchExpression,
        expected: Boolean,
        reason: String,
    ) {
        assertThat(
            reason,
            hasRightQueries(searchExpression),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_HAS_RIGHT_QUERIES = "DATA_FOR_HAS_RIGHT_QUERIES"
        const val DATA_FOR_PARSING_SEARCH_QUERY = "DATA_FOR_PARSING_SEARCH_QUERY"
        const val DATA_FOR_RESOLVE_SEARCH_EXPRESSION = "DATA_FOR_RESOLVE_SEARCH_EXPRESSION"
    }
}
