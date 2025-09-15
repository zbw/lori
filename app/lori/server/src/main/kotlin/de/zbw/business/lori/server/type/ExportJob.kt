package de.zbw.business.lori.server.type

import java.io.File
import java.time.Instant
import java.util.UUID

data class ExportJob(
    val id: UUID,
    var status: ExportJobStatus,
    val createdOn: Instant = Instant.now(),
    val createdBy: String,
    var lastUpdatedOn: Instant = Instant.now(),
    var errorMessage: String?,
    var filePath: String?,
    val searchTerm: String,
    val format: ExportFormat,
) {
    fun getFile(): File? = filePath?.let { File(it) }
}

enum class ExportJobStatus {
    QUEUED,
    RUNNING,
    FINISHED,
    FAILED,
}

enum class ExportFormat {
    CSV,
    JSON,
    ;

    companion object {
        val DEFAULT = CSV
    }
}
