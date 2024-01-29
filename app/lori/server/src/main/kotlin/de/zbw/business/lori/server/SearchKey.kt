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
    HDL(MetadataDB.TS_HANDLE),
    HDL_COL(MetadataDB.TS_COLLECTION_HANDLE),
    HDL_COM(MetadataDB.TS_COMMUNITY_HANDLE),
    HDL_SUBCOM(MetadataDB.TS_SUBCOMMUNITY_HANDLE),
    METADATA_ID(MetadataDB.TS_METADATA_ID),
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
            HDL -> "hdl"
            HDL_COL -> "hdlcol"
            HDL_COM -> "hdlcom"
            HDL_SUBCOM -> "hdlsubcom"
            METADATA_ID -> "metadataid"
        }
    }

    companion object {
        const val SUBQUERY_NAME = "sub"

        fun toEnum(s: String): SearchKey? {
            return when (s.lowercase()) {
                "com" -> COMMUNITY
                "col" -> COLLECTION
                "hdl" -> HDL
                "sig" -> PAKET_SIGEL
                "tit" -> TITLE
                "zdb" -> ZDB_ID
                "hdlcol" -> HDL_COL
                "hdlcom" -> HDL_COM
                "hdlsubcom" -> HDL_SUBCOM
                "metadataid" -> METADATA_ID
                else -> null
            }
        }
    }
}
