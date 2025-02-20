package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class FacetTransient(
    val accessState: AccessState?,
    val isPartOfSeries: String?,
    val licenceContract: String?,
    val nonStandardsOCL: Boolean,
    val nonStandardsOCLUrl: String?,
    val templateName: String?,
    val paketSigel: String?,
    val publicationType: PublicationType,
    val ocl: String?,
    val oclRestricted: Boolean,
    val zdbIdJournal: String?,
    val zdbIdSeries: String?,
    val zbwUserAgreement: Boolean,
    val licenceUrlFilter: String?,
)

data class FacetTransientSet(
    val accessState: Map<AccessState, Int>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasZbwUserAgreement: Boolean,
    val isPartOfSeries: Map<List<String>, Int>,
    val licenceUrls: Map<String, Int>,
    val paketSigels: Map<List<String>, Int>,
    val publicationType: Map<PublicationType, Int>,
    val templateIdToOccurence: Map<String, Int>,
    val zdbIdsJournal: Map<String, Int>,
    val zdbIdsSeries: Map<String, Int>,
)
