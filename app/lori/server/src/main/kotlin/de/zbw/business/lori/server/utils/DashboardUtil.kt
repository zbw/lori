package de.zbw.business.lori.server.utils

import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.RightError
import org.apache.logging.log4j.LogManager
import java.time.OffsetDateTime
import java.time.ZoneOffset

object DashboardUtil {
    private val LOG = LogManager.getLogger(DashboardUtil::class.java)

    fun checkForGapErrors(
        item: Item,
        createdBy: String,
    ): List<RightError> {
        if (item.rights.isEmpty()) {
            return emptyList()
        }
        // Check if rights end at given time
        val hasUnlimitedEnd: Boolean = item.rights.any { r -> r.endDate == null }
        val unlimitedEndMissing: RightError? =
            if (hasUnlimitedEnd) {
                null
            } else {
                val lastRight: ItemRight = item.rights.maxByOrNull { r -> r.startDate }!!
                RightError(
                    handle = item.metadata.handle,
                    message = "Handle ${item.metadata.handle} hat nur Rechteinformationen bis zum ${lastRight.endDate}.",
                    errorId = null,
                    createdOn = OffsetDateTime.now(ZoneOffset.UTC),
                    conflictingWithRightId = null,
                    conflictByRightId = null,
                    conflictType = ConflictType.GAP,
                    conflictByContext = item.metadata.paketSigel?.joinToString(separator = ",") ?: item.metadata.collectionName,
                    testId = null,
                    createdBy = createdBy,
                )
            }
        // Check for gaps between rights
        var gapRightErrors: List<RightError> = emptyList()
        val sortedRights = item.rights.sortedBy { it.startDate }
        for ((index, value) in sortedRights.withIndex()) {
            if (index == 0) {
                continue
            }
            if (sortedRights[index - 1].endDate == null) {
                LOG.warn("Unexpected undefined end date for RightId: ${sortedRights[index - 1].rightId}")

                continue
            }
            if (value.startDate != sortedRights[index - 1].endDate!!.plusDays(1)) {
                (
                    gapRightErrors +
                        RightError(
                            handle = item.metadata.handle,
                            message =
                                "Dem Handle ${item.metadata.handle} fehlt eine Rechteinformation zwischen" +
                                    " ${sortedRights[index - 1].endDate} und ${value.startDate}.",
                            errorId = null,
                            createdOn = OffsetDateTime.now(ZoneOffset.UTC),
                            conflictingWithRightId = null,
                            conflictByRightId = null,
                            conflictType = ConflictType.GAP,
                            conflictByContext = item.metadata.paketSigel?.joinToString(separator = ",") ?: item.metadata.collectionName,
                            testId = null,
                            createdBy = createdBy,
                        )
                ).also { gapRightErrors = it }
            }
        }
        return (gapRightErrors + unlimitedEndMissing).filterNotNull()
    }
}
