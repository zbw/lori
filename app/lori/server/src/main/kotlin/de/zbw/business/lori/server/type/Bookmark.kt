package de.zbw.business.lori.server.type

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemporalValidityFilter
import de.zbw.business.lori.server.ZDBIdFilter

/**
 * Business representation of [BookmarkRest].
 *
 * Created on 03-20-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class Bookmark(
    val bookmarkName: String,
    val bookmarkId: Int,
    val description: String? = null,
    val searchPairs: List<SearchPair>? = null,
    val publicationDateFilter: PublicationDateFilter? = null,
    val publicationTypeFilter: PublicationTypeFilter? = null,
    val paketSigelFilter: PaketSigelFilter? = null,
    val zdbIdFilter: ZDBIdFilter? = null,
    val accessStateFilter: AccessStateFilter? = null,
    val temporalValidityFilter: TemporalValidityFilter? = null,
    val formalRuleFilter: FormalRuleFilter? = null,
    val startDateFilter: StartDateFilter? = null,
    val endDateFilter: EndDateFilter? = null,
    val validOnFilter: RightValidOnFilter? = null,
    val noRightInformationFilter: NoRightInformationFilter? = null,
)
