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
    val values: String,
) {
    fun toWhereClause(): String =
        "${key.tsVectorColumn} @@ $SQL_FUNC_TO_TS_QUERY(?)"

    fun getCoalesce(): String =
        "$SQL_FUNC_COALESCE($SQL_FUNC_TS_RANK_CD(${key.tsVectorColumn}, $SQL_FUNC_TO_TS_QUERY(?)),1)"

    fun getValuesAsString(): String = values

    override fun toString(): String = "$key:$values"

    companion object {
        private const val SQL_FUNC_COALESCE = "coalesce"
        private const val SQL_FUNC_TO_TS_QUERY = "to_tsquery"
        private const val SQL_FUNC_TS_RANK_CD = "ts_rank_cd"
    }
}
