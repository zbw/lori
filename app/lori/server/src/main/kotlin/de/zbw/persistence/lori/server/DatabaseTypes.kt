@file:Suppress("ktlint:standard:filename")

package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class FacetTransientSet(
    val accessState: Map<AccessState, Int>,
    val hasNoLegalRisk: Boolean,
    val hasLicenceContract: Boolean,
    val hasCCLicenceNoRestriction: Boolean,
    val hasZbwUserAgreement: Boolean,
    val isPartOfSeries: Map<List<String>, Int>,
    val licenceUrls: Map<String, Int>,
    val paketSigels: Map<List<String>, Int>,
    val publicationType: Map<PublicationType, Int>,
    val templateIdToOccurence: Map<String, Int>,
    val zdbIds: Map<List<String>, Int>,
)
