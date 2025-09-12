package de.zbw.business.lori.server.export

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Export session.
 *
 * Created on 09-12-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ExportSession(
    private val file: File,
    private val writer: BufferedWriter,
    private val mutex: Mutex = Mutex(),
) : Closeable {
    /**
     * Append lines to the file in a thread-safe, coroutine-safe way.
     */
    suspend fun writeBatch(lines: List<String>) {
        mutex.withLock {
            lines.forEach { line ->
                writer.write(line)
                writer.newLine()
            }
            writer.flush() // flush to disk after each batch
        }
    }

    /**
     * Close the file. Must be called once you're done writing.
     */
    override fun close() {
        writer.close()
    }

    fun getFile(): File = file

    companion object {
        val dateFormatter: DateTimeFormatter? = DateTimeFormatter.ISO_DATE

        /**
         * Create a new ExportFile in the given directory.
         */
        fun create(
            exportDir: Path,
            jobUUID: UUID,
        ): ExportSession {
            Files.createDirectories(exportDir)

            val fileName = "${LocalDate.now().format(dateFormatter)}-$jobUUID.csv"
            val filePath = exportDir.resolve(fileName)

            val writer =
                Files.newBufferedWriter(
                    filePath,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE,
                )

            return ExportSession(filePath.toFile(), writer)
        }
    }
}
