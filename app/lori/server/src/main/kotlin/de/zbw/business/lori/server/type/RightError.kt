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
    val conflictingRightId: String?,
    val createdOn: OffsetDateTime?,
    val description: String?,
    val errorId: Int?,
    val handleId: String?,
    val metadataId: String?,
    val rightId: String?,
)
