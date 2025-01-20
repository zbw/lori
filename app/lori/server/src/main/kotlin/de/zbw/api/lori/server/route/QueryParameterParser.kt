package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.AccessStateOnDateFilter
import de.zbw.business.lori.server.DashboardConflictTypeFilter
import de.zbw.business.lori.server.DashboardTemplateNameFilter
import de.zbw.business.lori.server.DashboardTimeIntervalEndFilter
import de.zbw.business.lori.server.DashboardTimeIntervalStartFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.ManualRightFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SeriesFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemplateNameFilter
import de.zbw.business.lori.server.TemporalValidityFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.TemporalValidity
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
                noFromYear
                    .takeIf { !it }
                    ?.let { s.substringBefore("-").toInt() },
                noToYear
                    .takeIf { !it }
                    ?.let { s.substringAfter("-").toInt() },
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
                    PublicationType.valueOf(it)
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
        val paketSigelIds: List<String> = s.split(",".toRegex())
        return paketSigelIds.takeIf { it.isNotEmpty() }?.let { PaketSigelFilter(it) }
    }

    fun parseZDBIdFilter(s: String?): ZDBIdFilter? {
        if (s == null) {
            return null
        }
        val zdbIds: List<String> = s.split(",".toRegex())
        return zdbIds.takeIf { it.isNotEmpty() }?.let { ZDBIdFilter(it) }
    }

    fun parseSeriesFilter(s: String?): SeriesFilter? {
        if (s == null) {
            return null
        }
        val seriesNames: List<String> = s.split(",".toRegex())
        return seriesNames.takeIf { it.isNotEmpty() }?.let { SeriesFilter(it) }
    }

    fun parseAccessStateFilter(s: String?): AccessStateFilter? {
        if (s == null) {
            return null
        }
        val accessStates: List<AccessState> =
            s.split(",".toRegex()).mapNotNull {
                try {
                    AccessState.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        return accessStates.takeIf { it.isNotEmpty() }?.let { AccessStateFilter(it) }
    }

    fun parseTemporalValidity(s: String?): TemporalValidityFilter? {
        if (s == null) {
            return null
        }
        val temporalValidity: List<TemporalValidity> =
            s.split(",".toRegex()).mapNotNull {
                try {
                    TemporalValidity.valueOf(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        return temporalValidity.takeIf { it.isNotEmpty() }?.let { TemporalValidityFilter(it) }
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
                accessState = AccessState.valueOf(tokens[0]),
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
            }?.let {
                TemplateNameFilter(it)
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

    fun parseLicenceUrlFilter(s: String?): LicenceUrlFilter? = s?.let { LicenceUrlFilter(it) }
}
