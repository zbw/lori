package de.zbw.api.lori.server.type

enum class MetadataValidationError(
    val description: String,
) {
    INVALID_ISSUED("Metadata having an invalid dc.date.issued value"),
    MULTIPLE_ECONBIZIDS("Metadata having multiple values for dc.identifier.econbizid (Econbiz-ID)"),
    MULTIPLE_HANDLES("Metadata having multiple values for dc.identifier.uri (Handle)"),
    MULTIPLE_ISSNS("Metadata having multiple values for dc.identifier.issn (ISSN)"),
    MULTIPLE_JOURNAL_TITLES("Metadata having multiple values for dc.journalname (Journal titles)"),
    MULTIPLE_LICENCEURLS("Metadata having multiple values for dc.rights.license (Licence URL)"),
    MULTIPLE_PPNS("Metadata having multiple values for field dc.identifier.ppn (PPN)"),
    MULTIPLE_PUBLICATION_TYPES("Metadata having multiple values for dc.type (Publication type)"),
    MULTIPLE_PUBLICATION_YEARS("Metadata having multiple values for dc.date.issued (Publication years)"),
    MULTIPLE_SERIES_NAMES("Metadata having multiple values for dc.seriesname (Series name)"),
    MULTIPLE_STORAGE_DATES("Metadata having multiple values for dc.date.accessioned (Storage date)"),
    MULTIPLE_TITLES("Metadata having multiple values for dc.title (Title)"),
    MISSING_ECONBIZID_AND_PPN("Metadata missing dc.identifier.econbizid and dc.identifier.ppn"),
    MISSING_DATE_ISSUED_FIELD("Metadata missing required field dc.date.issued"),
    MISSING_REQUIRED_FIELD("Metadata missing dc.identifier.uri (Handle) or dc.type (Publication type) or dc.type (Publication type)"),
    UNKNOWN_PUBLICATION_TYPE("Metadata having an unknown dc.type value (publication type)"),
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
