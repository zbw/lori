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
    val distColumnName: String,
) {
    COMMUNITY(DatabaseConnector.COLUMN_METADATA_COMMUNITY_NAME, "dist_com"),
    COLLECTION(DatabaseConnector.COLUMN_METADATA_COLLECTION_NAME, "dist_col"),
    PAKET_SIGEL(DatabaseConnector.COLUMN_METADATA_PAKET_SIGEL, "dist_sig"),
    TITLE(DatabaseConnector.COLUMN_METADATA_TITLE, "dist_title"),
    ZDB_ID(DatabaseConnector.COLUMN_METADATA_ZDB_ID, "dist_zdb");

    fun toSelectClause(): String =
        "${this.dbColumnName} <-> ? as ${this.distColumnName}"

    fun toWhereClause(): String =
        "$SUBQUERY_NAME.${this.distColumnName} < $DISTANCE_VALUE"

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
        const val DISTANCE_VALUE = "0.9"
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
