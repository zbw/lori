package de.zbw.business.lori.server

import de.zbw.persistence.lori.server.DatabaseConnector

/**
 * SearchKeys representing keynames of the search input.
 * Pattern for searchterms: keyname:value
 *
 * Created on 09-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
enum class SearchKey(
    private val dbColumnName: String,
    val tsVectorColumn: String,
) {
    COMMUNITY(DatabaseConnector.COLUMN_METADATA_COMMUNITY_NAME, "ts_community"),
    COLLECTION(DatabaseConnector.COLUMN_METADATA_COLLECTION_NAME, "ts_collection"),
    PAKET_SIGEL(DatabaseConnector.COLUMN_METADATA_PAKET_SIGEL, "ts_sigel"),
    TITLE(DatabaseConnector.COLUMN_METADATA_TITLE, "ts_title"),
    ZDB_ID(DatabaseConnector.COLUMN_METADATA_ZDB_ID, "ts_zdb_id");

    fun toSelectClause(): String =
        "${this.dbColumnName} <-> ? as ${this.tsVectorColumn}"

    fun toWhereClause(query: List<String>): String =
        "$tsVectorColumn @@ to_tsquery('${query.joinToString(separator = " & ") }')"

    fun fromEnum(): String {
        return when (this) {
            COMMUNITY -> "com"
            COLLECTION -> "col"
            PAKET_SIGEL -> "sig"
            TITLE -> "tit"
            ZDB_ID -> "zdb"
        }
    }

    companion object {
        const val SUBQUERY_NAME = "sub"

        fun toEnum(s: String): SearchKey? {
            return when (s.lowercase()) {
                "com" -> COMMUNITY
                "col" -> COLLECTION
                "sig" -> PAKET_SIGEL
                "tit" -> TITLE
                "zdb" -> ZDB_ID
                else -> null
            }
        }
    }
}
