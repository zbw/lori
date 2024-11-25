package de.zbw.api.lori.server.utils

/**
 * Utility functions for [RestConverter].
 *
 * Created on 11-20-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object RestConverterUtil {
    fun prepareLicenceUrlFilter(licenceUrl: String?): String? {
        if (licenceUrl.isNullOrBlank()) {
            return null
        }
        return if (licenceUrl.startsWith("http://creativecommons.org/licenses/") ||
            licenceUrl.startsWith("https://creativecommons.org/licenses/")
        ) {
            licenceUrl
                .substringAfter("http://creativecommons.org/licenses/")
                .substringAfter("https://creativecommons.org/licenses/")
                .lowercase()
        } else {
            "andere"
        }
    }
}
