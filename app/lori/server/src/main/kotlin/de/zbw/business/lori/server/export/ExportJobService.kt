package de.zbw.business.lori.server.export

import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.ExportJobStatus
import de.zbw.business.lori.server.type.ItemMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.plusAssign
import kotlin.math.ceil

class ExportJobService(
    private val exportDir: String,
    private val backend: LoriServerBackend,
) {
    private val jobs: MutableMap<UUID, ExportJob> = ConcurrentHashMap()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun createJob(
        createdBy: String,
        searchTerm: String,
        format: ExportFormat,
    ): ExportJob {
        val job =
            ExportJob(
                status = ExportJobStatus.QUEUED,
                searchTerm = searchTerm,
                createdBy = createdBy,
                errorMessage = null,
                filePath = null,
                format = format,
            )

        runBlocking {
            backend.insertExportJob(job)
        }
        jobs[job.id] = job

        // Launch job async
        scope.launch {
            runJob(job)
        }

        return job
    }

    fun getJob(id: UUID): ExportJob? = jobs[id]

    private suspend fun runJob(job: ExportJob) {
        job.status = ExportJobStatus.RUNNING
        try {
            val session =
                ExportSession.create(
                    exportDir = Path.of(exportDir),
                    jobUUID = job.id,
                    format = job.format,
                )
            job.filePath = session.getFile().path
            backend.updateExportJobById(job)
            session.use { exportSession ->
                coroutineScope {
                    val facetsResult =
                        backend.searchQuery(
                            job.searchTerm,
                            limit = null,
                            offset = null,
                            facetsOnly = true,
                        )

                    val deferHeader =
                        async {
                            if (job.format == ExportFormat.CSV) {
                                exportSession.writeBatch(
                                    listOf(ItemMetadata.csvFileHeader()),
                                )
                            }
                        }
                    deferHeader.await()

                    val jobs = mutableListOf<Job>()
                    for (offset in 0..<ceil(facetsResult.numberOfResults.toDouble() / BATCH_SIZE).toInt()) {
                        jobs +=
                            launch {
                                semaphore.withPermit {
                                    LOG.info(
                                        "Export-Job ${job.id}: " +
                                            "Export results ${offset * BATCH_SIZE} to ${offset * BATCH_SIZE + BATCH_SIZE}",
                                    )
                                    val results =
                                        backend.searchQuery(
                                            searchTerm = job.searchTerm,
                                            offset = offset * BATCH_SIZE,
                                            limit = BATCH_SIZE,
                                        )
                                    if (job.format == ExportFormat.CSV) {
                                        exportSession.writeBatch(
                                            results.results.map { it.metadata.toCSV() },
                                        )
                                    } else if (job.format == ExportFormat.JSON) {
                                        exportSession.writeBatch(
                                            results.results.map { it.metadata.toJson() },
                                        )
                                    }
                                }
                            }
                        jobs.joinAll()
                    }
                }
            }
            if (job.format == ExportFormat.JSON) {
                val newFile =
                    ExportSession.convertNDJsonToJson(
                        exportDir = Path.of(exportDir),
                        job = job,
                    )
                if (newFile != null) {
                    session.getFile().delete()
                    job.filePath = newFile.path
                }
            }
            job.status = ExportJobStatus.SUCCESSFUL
        } catch (e: Exception) {
            job.status = ExportJobStatus.FAILED
            job.errorMessage = e.message
        } finally {
            job.lastUpdatedOn = java.time.Instant.now()
            backend.updateExportJobById(job)
            jobs.remove(job.id)
        }
    }

    companion object {
        const val BATCH_SIZE = 1000
        private const val MAX_PARALLEL_CONNECTIONS = 5
        private val semaphore = Semaphore(MAX_PARALLEL_CONNECTIONS)
        internal val LOG: Logger = LogManager.getLogger(ExportJobService::class.java)
    }
}
