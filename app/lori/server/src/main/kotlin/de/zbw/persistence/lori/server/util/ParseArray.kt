package de.zbw.persistence.lori.server.util

object ParseArray {
    fun parsePostgresArray(input: String): List<String> {
        // Remove outer braces
        val content = input.trim().removeSurrounding("{", "}")

        if (content.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val char = content[i]

            when {
                char == '"' && !inQuotes -> {
                    // Start of quoted element
                    inQuotes = true
                }

                char == '"' && inQuotes -> {
                    // Check for escaped quote
                    if (i + 1 < content.length && content[i + 1] == '"') {
                        current.append('"')
                        i++ // Skip next quote
                    } else {
                        // End of quoted element
                        inQuotes = false
                    }
                }

                char == ',' && !inQuotes -> {
                    // Element separator
                    result.add(current.toString())
                    current.clear()
                }

                else -> {
                    current.append(char)
                }
            }
            i++
        }

        // Add last element
        if (current.isNotEmpty() || inQuotes) {
            result.add(current.toString())
        }

        return result
    }
}
