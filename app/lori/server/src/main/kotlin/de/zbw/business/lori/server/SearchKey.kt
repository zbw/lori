package de.zbw.business.lori.server

/**
 * SearchKeys representing keynames of the search input.
 * Pattern for searchterms: keyname:value
 *
 * Created on 09-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
enum class SearchKey(
    private val dbColumnName: String,
) {
    COMMUNITY("ts_community"),
    COLLECTION("ts_collection"),
    PAKET_SIGEL("ts_sigel"),
    ZBD_ID("ts_zbd_id");

    fun toWhereClause(): String =
        "${this.dbColumnName} @@ to_tsquery('english', ?)"

    companion object {
        fun toEnum(s: String): SearchKey? {
            return when (s.lowercase()) {
                "com" -> COMMUNITY
                "col" -> COLLECTION
                "sig" -> PAKET_SIGEL
                "zbd" -> ZBD_ID
                else -> null
            }
        }
    }
}
