package de.zbw.business.lori.server.type

import java.io.File
import java.io.InputStream
import java.time.Instant
import java.util.UUID

data class ExportJob(
    val id: UUID = UUID.randomUUID(),
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

    fun getInputStream(): InputStream? = getFile()?.inputStream()
}

enum class ExportJobStatus {
    QUEUED,
    RUNNING,
    SUCCESSFUL,
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
