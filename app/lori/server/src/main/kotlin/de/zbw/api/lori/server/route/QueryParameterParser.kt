package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.AccessStateOnDateFilter
import de.zbw.business.lori.server.DOIFilter
import de.zbw.business.lori.server.DashboardConflictTypeFilter
import de.zbw.business.lori.server.DashboardTemplateNameFilter
import de.zbw.business.lori.server.DashboardTimeIntervalEndFilter
import de.zbw.business.lori.server.DashboardTimeIntervalStartFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.ISBNFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.LicenceUrlFilterLUK
import de.zbw.business.lori.server.ManualRightFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PPNFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.RightIdFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SeriesFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemplateNameFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.PublicationType
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.text.dropLast
import kotlin.text.last

/**
 * Helper object for parsing all sorts of query parameters.
 *
 * Created on 09-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object QueryParameterParser {
    fun parsePublicationYearFilter(s: String?): PublicationYearFilter? {
        if (s == null) {
            return null
        }
        val noToYear = s.matches("\\d+-".toRegex())
        val noFromYear = s.matches("-\\d+".toRegex())
        val both = s.matches("\\d+-\\d+".toRegex())
        return if (noFromYear || noToYear || both) {
            val fromYear =
                runCatching {
                    if (noToYear || both) s.substringBefore("-").toInt() else null
                }.getOrNull()

            val toYear =
                runCatching {
                    if (noFromYear || both) s.substringAfter("-").toInt() else null
                }.getOrNull()

            if (toYear == null && fromYear == null) {
                return null
            }
            PublicationYearFilter(
                fromYear,
                toYear,
            )
        } else {
            null
        }
    }

    fun parsePublicationTypeFilter(s: String?): PublicationTypeFilter? {
        if (s == null) {
            return null
        }
        val receivedPubTypes: List<PublicationType> =
            s.split(",".toRegex()).mapNotNull {
                try {
                    PublicationType.valueOf(it.uppercase())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        return receivedPubTypes.takeIf { it.isNotEmpty() }?.let { PublicationTypeFilter(it) }
    }

    fun parsePaketSigelFilter(s: String?): PaketSigelFilter? {
        if (s == null) {
            return null
        }
        val paketSigelIds: List<String> =
            s
                .split(",".toRegex())
                .map {
                    escapeWildcards(it)
                }
        return paketSigelIds.takeIf { it.isNotEmpty() }?.let { PaketSigelFilter(it) }
    }

    fun parseZDBIdFilter(s: String?): ZDBIdFilter? {
        if (s == null) {
            return null
        }
        val zdbIds: List<String> =
            s
                .split(",".toRegex())
                .map {
                    escapeWildcards(it)
                }
        return zdbIds.takeIf { it.isNotEmpty() }?.let { ZDBIdFilter(it) }
    }

    fun parseISBNFilter(s: String?): ISBNFilter? {
        if (s == null) {
            return null
        }
        val isbns: List<String> =
            s
                .split(",".toRegex())
                .map {
                    escapeWildcards(it)
                }
        return isbns.takeIf { it.isNotEmpty() }?.let { ISBNFilter(it) }
    }

    fun parseDoiFilter(s: String?): DOIFilter? {
        if (s == null) {
            return null
        }
        val dois: List<String> =
            s
                .split(",".toRegex())
                .map {
                    escapeWildcards(it)
                }
        return dois.takeIf { it.isNotEmpty() }?.let { DOIFilter(it) }
    }

    fun parseSeriesFilter(s: String?): SeriesFilter? {
        if (s == null) {
            return null
        }
        val seriesNames: List<String> =
            s
                .split(",".toRegex())
                .map {
                    escapeWildcards(it)
                }
        return seriesNames.takeIf { it.isNotEmpty() }?.let { SeriesFilter(it) }
    }

    fun parseAccessStateFilter(s: String?): AccessStateFilter? {
        if (s == null) {
            return null
        }
        val accessStates: List<AccessState> =
            s.split(",".toRegex()).mapNotNull {
                try {
                    AccessState.valueOf(it.uppercase())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        return accessStates.takeIf { it.isNotEmpty() }?.let { AccessStateFilter(it) }
    }

    fun parseStartDateFilter(s: String?): StartDateFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { StartDateFilter(it) }

    fun parseEndDateFilter(s: String?): EndDateFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { EndDateFilter(it) }

    fun parseRightValidOnFilter(s: String?): RightValidOnFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { RightValidOnFilter(it) }

    fun parseAccessStateOnDate(s: String?): AccessStateOnDateFilter? {
        // Format: OPEN+2024-09-17 or 2024-09-17
        if (s == null) {
            return null
        }
        val tokens: List<String> = s.split("\\+".toRegex())
        return if (tokens.size == 2) {
            val parsedDate = parseDate(tokens[1])
            if (parsedDate == null) {
                return null
            }
            AccessStateOnDateFilter(
                accessState = AccessState.valueOf(tokens[0].uppercase()),
                date = parsedDate,
            )
        } else if (tokens.size == 1) {
            val parsedDate = parseDate(tokens[0])
            if (parsedDate == null) {
                return null
            }
            AccessStateOnDateFilter(
                accessState = null,
                date = parsedDate,
            )
        } else {
            null
        }
    }

    private fun parseDate(s: String): LocalDate? =
        try {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeException) {
            null
        }

    fun parseFormalRuleFilter(s: String?): FormalRuleFilter? =
        s?.let { input ->
            val formalRules: List<FormalRule> =
                input.split(",".toRegex()).mapNotNull {
                    try {
                        FormalRule.valueOf(it.uppercase())
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            return formalRules.takeIf { it.isNotEmpty() }?.let { FormalRuleFilter(it) }
        }

    fun parseNoRightInformationFilter(s: String?): NoRightInformationFilter? =
        s?.let { input ->
            if (input.lowercase().toBoolean()) {
                NoRightInformationFilter()
            } else {
                null
            }
        }

    fun parseManualRightFilter(s: String?): ManualRightFilter? =
        s?.let { input ->
            if (input.lowercase().toBoolean()) {
                ManualRightFilter()
            } else {
                null
            }
        }

    fun parseTemplateNameFilter(s: String?): TemplateNameFilter? =
        s
            ?.split(",".toRegex())
            ?.takeIf {
                it.isNotEmpty()
            }?.map {
                escapeWildcards(it)
            }?.let {
                TemplateNameFilter(it)
            }

    fun parseRightIdFilter(s: String?): RightIdFilter? =
        s
            ?.split(",".toRegex())
            ?.takeIf {
                it.isNotEmpty()
            }?.let {
                RightIdFilter(it)
            }

    fun parseDashboardTemplateNameFilter(s: String?): DashboardTemplateNameFilter? =
        s
            ?.split(",".toRegex())
            ?.takeIf {
                it.isNotEmpty()
            }?.let {
                DashboardTemplateNameFilter(it)
            }

    fun parseDashboardConflictTypeFilter(s: String?): DashboardConflictTypeFilter? {
        if (s == null) {
            return null
        }
        val receivedConflictTypes =
            s.split(",".toRegex()).mapNotNull {
                try {
                    ConflictType.valueOf(it.uppercase())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        return receivedConflictTypes.takeIf { it.isNotEmpty() }?.let { DashboardConflictTypeFilter(it) }
    }

    fun parseDashboardStartDateFilter(s: String?): DashboardTimeIntervalStartFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { DashboardTimeIntervalStartFilter(it) }

    fun parseDashboardEndDateFilter(s: String?): DashboardTimeIntervalEndFilter? =
        s
            ?.let { parseDate(it) }
            ?.let { DashboardTimeIntervalEndFilter(it) }

    fun parseLicenceUrlFilter(s: String?): LicenceUrlFilter? = s?.let { LicenceUrlFilter(escapeWildcards(it)) }

    fun parseLicenceUrlLUKFilter(s: String?): LicenceUrlFilterLUK? =
        s?.let {
            LicenceUrlFilterLUK(
                it.replace(Regex("[-_]"), ""),
            )
        }

    fun parsePPNFilter(s: String?): PPNFilter? = s?.let { PPNFilter(escapeWildcards(it)) }

    fun escapeWildcards(s: String): String {
        val escaped =
            s
                .replace("&", "\\&")
                .replace("_", "\\_")
        return if (escaped.last() == '*') {
            escaped.dropLast(1) + "%"
        } else {
            escaped
        }
    }
}
