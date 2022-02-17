package de.zbw.business.lori.server

data class Item(
    val itemMetadata: ItemMetadata,
    val actions: List<Action>,
)

data class ItemMetadata(
    val id: String,
    val accessState: AccessState?,
    val band: String?,
    val doi: String?,
    val handle: String,
    val isbn: String?,
    val issn: String?,
    val licenseConditions: String?,
    val paketSigel: String?,
    val ppn: String?,
    val ppnEbook: String?,
    val provenanceLicense: String?,
    val publicationType: PublicationType,
    val publicationYear: Int,
    val rightsK10plus: String?,
    val serialNumber: String?,
    val title: String,
    val titleJournal: String?,
    val titleSeries: String?,
    val zbdId: String?,
)

enum class AccessState {
    CLOSED,
    OPEN,
    RESTRICTED,
}

enum class PublicationType {
    ARTICLE,
    BOOK,
}

data class Action(
    val type: ActionType,
    val permission: Boolean,
    val restrictions: List<Restriction>,
)

data class Restriction(
    val type: RestrictionType,
    val attribute: Attribute, // TODO(CB): Should be a list.
)

data class Attribute(
    val type: AttributeType,
    val values: List<String>,
)

enum class ActionType {
    READ,
    RUN,
    LEND,
    DOWNLOAD,
    PRINT,
    REPRODUCE,
    MODIFY,
    REUSE,
    DISTRIBUTE,
    PUBLISH,
    ARCHIVE,
    INDEX,
    MOVE,
    DISPLAY_METADATA,
}

enum class RestrictionType {
    GROUP,
    AGE,
    LOCATION,
    DATE,
    DURATION,
    COUNT,
    CONCURRENT,
    WATERMARK,
    QUALITY,
    AGREEMENT,
    PARTS,
}

enum class AttributeType {
    FROM_DATE,
    TO_DATE,
    MAX_RESOLUTION,
    MAX_BITRATE,
    COUNT,
    INSIDE,
    SUBNET,
    OUTSIDE,
    WATERMARK,
    DURATION,
    MIN_AGE,
    MAX_AGE,
    REQUIRED,
    GROUPS,
    PARTS,
    SESSIONS,
}
