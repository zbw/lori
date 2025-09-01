package de.zbw.business.lori.server.type

import de.zbw.business.lori.server.type.SortByField.Companion.DEFAULT_SORT_BY_FIELD
import de.zbw.business.lori.server.type.SortOrder.Companion.DEFAULT_SORT_ORDER
import de.zbw.persistence.lori.server.MetadataDB

class SortInformation(
    val sortOrder: SortOrder = DEFAULT_SORT_ORDER,
    val sortByField: SortByField = DEFAULT_SORT_BY_FIELD,
) {
    companion object {
        val DEFAULT = SortInformation()
    }
}

enum class SortOrder(
    val sqlSyntax: String,
) {
    ASC("ASC"),
    DESC("DESC"),
    ;

    companion object {
        val DEFAULT_SORT_ORDER = DESC
    }
}

enum class SortByField(
    val columnName: String,
) {
    BAND(MetadataDB.COLUMN_METADATA_BAND),
    COLLECTION_NAME(MetadataDB.COLUMN_METADATA_COLLECTION_NAME),
    COMMUNITY_NAME(MetadataDB.COLUMN_METADATA_COMMUNITY_NAME),
    DOI(MetadataDB.COLUMN_METADATA_DOI),
    ISBN(MetadataDB.COLUMN_METADATA_ISBN),
    ISSN(MetadataDB.COLUMN_METADATA_ISSN),
    HANDLE(MetadataDB.COLUMN_METADATA_HANDLE_POSTFIX),
    PAKET_SIGEL(MetadataDB.COLUMN_METADATA_PAKET_SIGEL),
    PPN(MetadataDB.COLUMN_METADATA_PPN),
    IS_PART_OF_SERIES(MetadataDB.COLUMN_METADATA_IS_PART_OF_SERIES),
    PUBLICATION_TYPE(MetadataDB.COLUMN_METADATA_PUBLICATION_TYPE),
    PUBLICATION_YEAR(MetadataDB.COLUMN_METADATA_PUBLICATION_YEAR),
    TITLE(MetadataDB.COLUMN_METADATA_TITLE),
    TITLE_JOURNAL(MetadataDB.COLUMN_METADATA_TITLE_JOURNAL),
    TITLE_SERIES(MetadataDB.COLUMN_METADATA_TITLE_SERIES),
    ;

    companion object {
        val DEFAULT_SORT_BY_FIELD = HANDLE
    }
}
