package de.zbw.api.lori.server.type

data class ConflictError(
    val conflictWithMetadataId: String,
    val conflictType: ConflictType,
    val conflictWithRightId: String,
    val message: String?,
    val templateIdApplied: Int,
)

enum class ConflictType {
    DATE_OVERLAP,
    UNSPECIFIED,
}
