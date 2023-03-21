package de.zbw.business.lori.server.type

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SearchKey
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
    val bookmarkId: Int?,
    val searchKeys: Map<SearchKey, List<String>>?,
    val publicationDateFilter: PublicationDateFilter?,
    val publicationTypeFilter: PublicationTypeFilter?,
    val paketSigelFilter: PaketSigelFilter?,
    val zdbIdFilter: ZDBIdFilter?,
    val accessStateFilter: AccessStateFilter?,
    val temporalValidityFilter: TemporalValidityFilter?,
    val formalRuleFilter: FormalRuleFilter?,
    val startDateFilter: StartDateFilter?,
    val endDateFilter: EndDateFilter?,
    val validOnFilter: RightValidOnFilter?,
    val noRightInformationFilter: NoRightInformationFilter?,
)
