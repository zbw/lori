package de.zbw.business.lori.server.type

import java.time.Instant
import java.util.UUID

data class GenericJob(
    val createdBy: String,
    val createdOn: Instant = Instant.now(),
    var errorMessage: String? = null,
    val id: UUID = UUID.randomUUID(),
    val kind: JobKind,
    var lastUpdatedOn: Instant = Instant.now(),
    var status: JobStatus,
    var summary: String? = null,
)

enum class JobStatus {
    QUEUED,
    RUNNING,
    SUCCESSFUL,
    FAILED,
}

enum class JobKind {
    CLEAN_DOWNLOADS,
    FULL_IMPORT,
    TEMPLATE_APPLY,
    CHECK_RIGHT_ERRORS,
}
