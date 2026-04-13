package de.zbw.api.lori.server.type

import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_ECONBIZID
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_ECONSTOR_ISSUE
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_ECONSTOR_VOLUME
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_HANDLE
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_IS_PART_OF_BOOK
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_IS_PART_OF_JOURNAL
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_LICENSE
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_PPN
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_PPN_BOOK
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_PPN_JOURNAL
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_PPN_SERIES
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_PUBLICATION_TYPE
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_PUBLICATION_YEAR
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_STORAGE_DATE
import de.zbw.api.lori.server.type.rest.DAItemConverter.KEY_TITLE

enum class MetadataValidationError(
    val description: String,
) {
    INVALID_ISSUED("Metadata having an invalid $KEY_PUBLICATION_YEAR value"),
    MULTIPLE_BOOKS("Metadata having multiple values for field $KEY_IS_PART_OF_BOOK"),
    MULTIPLE_ECONBIZIDS("Metadata having multiple values for $KEY_ECONBIZID"),
    MULTIPLE_ECONSTOR_ISSUE("Metadata having multiple values for $KEY_ECONSTOR_ISSUE "),
    MULTIPLE_ECONSTOR_VOLUME("Metadata having multiple values for $KEY_ECONSTOR_VOLUME "),
    MULTIPLE_HANDLES("Metadata having multiple values for $KEY_HANDLE"),
    MULTIPLE_JOURNALS("Metadata having multiple values for field $KEY_IS_PART_OF_JOURNAL"),
    MULTIPLE_LICENCEURLS("Metadata having multiple values for $KEY_LICENSE"),
    MULTIPLE_PPNS("Metadata having multiple values for field $KEY_PPN"),
    MULTIPLE_BOOK_PPNS("Metadata having multiple values for field $KEY_PPN_BOOK"),
    MULTIPLE_JOURNAL_PPNS("Metadata having multiple values for field $KEY_PPN_JOURNAL"),
    MULTIPLE_SERIES_PPNS("Metadata having multiple values for field $KEY_PPN_SERIES"),
    MULTIPLE_PUBLICATION_TYPES("Metadata having multiple values for $KEY_PUBLICATION_TYPE"),
    MULTIPLE_PUBLICATION_YEARS("Metadata having multiple values for $KEY_PUBLICATION_YEAR"),
    MULTIPLE_STORAGE_DATES("Metadata having multiple values for $KEY_STORAGE_DATE"),
    MULTIPLE_TITLES("Metadata having multiple values for $KEY_TITLE"),
    MISSING_ECONBIZID_AND_PPN("Metadata missing $KEY_ECONBIZID and $KEY_PPN"),
    MISSING_DATE_ISSUED_FIELD("Metadata missing required field $KEY_PUBLICATION_YEAR"),
    MISSING_REQUIRED_FIELD("Metadata missing $KEY_HANDLE or $KEY_PUBLICATION_TYPE or $KEY_TITLE"),
    UNKNOWN_PUBLICATION_TYPE("Metadata having an unknown $KEY_PUBLICATION_TYPE value"),
    ;

    companion object {
        fun prettyPrintMap(validationErrorMap: Map<MetadataValidationError, List<String>>): String {
            val sb = StringBuilder()
            validationErrorMap.entries.forEach { entry ->
                sb
                    .append(entry.key.description)
                    .append(":\n")
                    .append(entry.value.joinToString("\n"))
                    .append("\n\n")
            }
            return sb.toString()
        }
    }
}
