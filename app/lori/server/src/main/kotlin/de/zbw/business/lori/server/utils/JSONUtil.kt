package de.zbw.business.lori.server.utils

import java.io.File

object JSONUtil {
    fun ndjsonToJsonArray(
        inputFile: File,
        outputFile: File,
    ) {
        outputFile.bufferedWriter().use { writer ->
            writer.write("[\n")

            val lines = inputFile.readLines()
            lines.forEachIndexed { index, line ->
                writer.write(line)
                if (index != lines.lastIndex) {
                    writer.write(",\n") // add commas between objects
                }
            }

            writer.write("\n]")
        }
    }
}
