package de.zbw.business.lori.server

/**
 * This class represents a key-value pair of a search:
 * Pattern: keyname:'value1 operator1 value2 operator2 ...'
 *
 * Created on 08-18-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SearchPair(
    val key: SearchKey,
    val values: List<String>,
) {
    // TODO: Check if List<String> is viable for the query type
    // E.g. the user inputs: "com:'foo | bar' com:'baz'" should not get lost
    // TODO: Prevent SQL injections -> toWhereClause() and getCoalesce() only inside prepared statement
    // TODO: Unit test both functions
    fun toWhereClause(): String =
        "${key.tsVectorColumn} @@ $SQL_FUNC_TO_TS_QUERY(?)"

    fun getCoalesce(): String =
        "$SQL_FUNC_COALESCE($SQL_FUNC_TS_RANK_CD(${key.tsVectorColumn}, $SQL_FUNC_TO_TS_QUERY(?)),1)"

    fun getValuesAsString(): String = values.joinToString(separator = " & ")

    companion object {
        private const val SQL_FUNC_COALESCE = "coalesce"
        private const val SQL_FUNC_TO_TS_QUERY = "to_tsquery"
        private const val SQL_FUNC_TS_RANK_CD = "ts_rank_cd"
    }
}