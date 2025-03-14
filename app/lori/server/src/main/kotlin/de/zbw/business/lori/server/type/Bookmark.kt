package de.zbw.business.lori.server.type

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.AccessStateOnDateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.ManualRightFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.RightIdFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SearchFilter.Companion.filtersToString
import de.zbw.business.lori.server.SeriesFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.ZDBIdFilter
import java.time.OffsetDateTime

/**
 * Business representation of [BookmarkRest].
 *
 * Created on 03-20-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class Bookmark(
    val bookmarkName: String,
    val bookmarkId: Int,
    val createdBy: String? = null,
    val createdOn: OffsetDateTime? = null,
    val description: String? = null,
    val lastUpdatedBy: String? = null,
    val lastUpdatedOn: OffsetDateTime? = null,
    val searchTerm: String? = null,
    val publicationYearFilter: PublicationYearFilter? = null,
    val publicationTypeFilter: PublicationTypeFilter? = null,
    val paketSigelFilter: PaketSigelFilter? = null,
    val zdbIdFilter: ZDBIdFilter? = null,
    val accessStateFilter: AccessStateFilter? = null,
    val formalRuleFilter: FormalRuleFilter? = null,
    val startDateFilter: StartDateFilter? = null,
    val endDateFilter: EndDateFilter? = null,
    val validOnFilter: RightValidOnFilter? = null,
    val noRightInformationFilter: NoRightInformationFilter? = null,
    val seriesFilter: SeriesFilter? = null,
    val rightIdFilter: RightIdFilter? = null,
    val licenceURLFilter: LicenceUrlFilter? = null,
    val manualRightFilter: ManualRightFilter? = null,
    val accessStateOnFilter: AccessStateOnDateFilter? = null,
    private var queryString: String? = null,
) {
    fun getAllMetadataFilter(): List<MetadataSearchFilter> =
        listOfNotNull(
            licenceURLFilter,
            paketSigelFilter,
            publicationYearFilter,
            publicationTypeFilter,
            seriesFilter,
            zdbIdFilter,
        )

    fun getAllRightFilter(): List<RightSearchFilter> =
        listOfNotNull(
            accessStateFilter,
            endDateFilter,
            formalRuleFilter,
            rightIdFilter,
            startDateFilter,
            validOnFilter,
            manualRightFilter,
            accessStateOnFilter,
        )

    fun initializeQueryString() {
        queryString = computeQueryString()
    }

    fun getQueryString() = queryString

    fun computeQueryString(): String {
        return if (queryString == null) {
            val filters =
                filtersToString(
                    getAllMetadataFilter() + getAllRightFilter() +
                        listOfNotNull(noRightInformationFilter),
                )
            return if (searchTerm.isNullOrBlank()) {
                filters
            } else if (filters.isBlank()) {
                searchTerm
            } else {
                "($searchTerm) & ($filters)"
            }
        } else {
            queryString!!
        }
    }
}

data class RightIdTemplateName(
    val rightId: String,
    val templateName: String,
)
