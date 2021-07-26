package de.zbw.business.access.server

data class AccessRight(
    val header: Header,
    val actions: List<Action>,
)

data class Header(
    val id: String,
    val tenant: String?,
    val usageGuide: String?,
    val template: String?,
    val mention: Boolean,
    val shareAlike: Boolean,
    val commercialUse: Boolean,
    val copyright: Boolean,
)

data class Action(
    val type: ActionType,
    val permission: Boolean,
    val restrictions: List<Restriction>,
)

data class Restriction(
    val type: RestrictionType,
    val attribute: Attribute,
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
