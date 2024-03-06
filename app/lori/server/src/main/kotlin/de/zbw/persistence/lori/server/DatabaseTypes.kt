package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType
import java.time.OffsetDateTime

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class FacetTransient(
    val accessState: AccessState?,
    val licenceContract: String?,
    val nonStandardsOCL: Boolean,
    val nonStandardsOCLUrl: String?,
    val paketSigel: String?,
    val publicationType: PublicationType,
    val ocl: String?,
    val oclRestricted: Boolean,
    val templateId: Int,
    val zdbId: String?,
    val zbwUserAgreement: Boolean,
)

data class FacetTransientSet(
    val accessState: Map<AccessState, Int>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasZbwUserAgreement: Boolean,
    val paketSigels: Map<String, Int>,
    val publicationType: Map<PublicationType, Int>,
    val templateIdToOccurence: Map<Int, Int>,
    val zdbIds: Map<String, Int>,
)

data class TemplateTransient(
    val templateId: Int,
    val templateName: String,
    val description: String?,
    val rightId: String,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val lastAppliedOn: OffsetDateTime?,
)

data class TemplateRightIdCreated(
    val templateId: Int,
    val rightId: String,
)
