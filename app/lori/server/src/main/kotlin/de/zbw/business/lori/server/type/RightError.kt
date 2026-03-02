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
    val conflictCausedByRightId: String?,
    val conflictCausedInContext: String?,
    val conflictWithExistingRightId: String?,
    val conflictType: ConflictType,
    val createdBy: String?,
    val createdOn: OffsetDateTime,
    val errorId: Int?,
    val existingRightIsTemplate: Boolean = false,
    val handle: String,
    val message: String,
    val testId: String?,
)

enum class ConflictType {
    DATE_OVERLAP,
    DELETION,
    GAP,
    NO_RIGHT,
    UNSPECIFIED,
}
