package de.zbw.business.lori.server

import de.zbw.persistence.lori.server.MetadataDB

/**
 * SearchKeys representing keynames of the search input.
 * Pattern for searchterms: keyname:value
 *
 * Created on 09-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
enum class SearchKey(
    val tsVectorColumn: String,
) {
    COMMUNITY(MetadataDB.TS_COMMUNITY),
    COLLECTION(MetadataDB.TS_COLLECTION),
    PAKET_SIGEL(MetadataDB.TS_SIGEL),
    TITLE(MetadataDB.TS_TITLE),
    ZDB_ID(MetadataDB.TS_ZDB_ID);

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
