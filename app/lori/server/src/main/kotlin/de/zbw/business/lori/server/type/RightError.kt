package de.zbw.business.lori.server.type

import java.time.OffsetDateTime

/**
 * Business representation of errors related to creating/managing rights
 * or applying templates.
 *
 * Created on 01-17-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class RightError(
    val conflictByRightId: String,
    val conflictByTemplateName: String?,
    val conflictType: ConflictType,
    val createdOn: OffsetDateTime,
    val message: String,
    val handleId: String,
    val metadataId: String,
    val errorId: Int?,
    val conflictingWithRightId: String,
)

enum class ConflictType {
    DATE_OVERLAP,
    UNSPECIFIED,
}
