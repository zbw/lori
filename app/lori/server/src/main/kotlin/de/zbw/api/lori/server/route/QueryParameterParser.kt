package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.PublicationDateFilter

/**
 * Helper object for parsing all sort of query parameters.
 *
 * Created on 09-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object QueryParameterParser {
    fun parsePublicationDateFilter(s: String?): PublicationDateFilter? {
        if (s == null) {
            return null
        }
        val noToYear = s.matches("\\d+-".toRegex())
        val noFromYear = s.matches("-\\d+".toRegex())
        val both = s.matches("\\d+-\\d+".toRegex())
        return if (noFromYear || noToYear || both) {
            PublicationDateFilter(
                noFromYear.takeIf { !it }
                    ?.let { s.substringBefore("-").toInt() }
                    ?: PublicationDateFilter.MIN_YEAR,
                noToYear.takeIf { !it }
                    ?.let { s.substringAfter("-").toInt() }
                    ?: PublicationDateFilter.MAX_YEAR
            )
        } else {
            null
        }
    }
}
