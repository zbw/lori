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
    HANDLE(MetadataDB.COLUMN_METADATA_HANDLE),
    ;

    companion object {
        val DEFAULT_SORT_BY_FIELD = HANDLE
    }
}
