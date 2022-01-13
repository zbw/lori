package de.zbw.business.access.server

data class Item(
    val metadata: Metadata,
    val actions: List<Action>,
)

data class Metadata(
    val id: String,
    val access_state: AccessState?,
    val band: String?,
    val doi: String?,
    val handle: String,
    val isbn: String?,
    val issn: String?,
    val paket_sigel: String?,
    val ppn: String?,
    val ppn_ebook: String?,
    val publicationType: PublicationType,
    val publicationYear: Int,
    val rights_k10plus: String?,
    val serialNumber: String?,
    val title: String,
    val title_journal: String?,
    val title_series: String?,
    val zbd_id: String?,
)

enum class AccessState {
    CLOSED,
    OPEN,
    RESTRICTED,
}

enum class PublicationType {
    MONO,
    PERIODICAL,
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
