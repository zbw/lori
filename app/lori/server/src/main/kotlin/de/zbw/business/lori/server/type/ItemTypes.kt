package de.zbw.business.lori.server.type

import java.time.LocalDate
import java.time.OffsetDateTime

data class Item(
    val metadata: ItemMetadata,
    val rights: List<ItemRight>,
)

data class ItemMetadata(
    val metadataId: String,
    val author: String?,
    val band: String?,
    val collectionHandle: String?,
    val collectionName: String?,
    val communityHandle: String?,
    val communityName: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val doi: String?,
    val handle: String,
    val isbn: String?,
    val issn: String?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val licenceUrl: String?,
    val paketSigel: String?,
    val ppn: String?,
    val publicationType: PublicationType,
    val publicationDate: LocalDate,
    val rightsK10plus: String?,
    val subCommunityHandle: String?,
    val subCommunityName: String?,
    val storageDate: OffsetDateTime?,
    val title: String,
    val titleJournal: String?,
    val titleSeries: String?,
    val zdbId: String?,
)

enum class AccessState {
    CLOSED,
    OPEN,
    RESTRICTED,
}

enum class TemporalValidity {
    FUTURE,
    PAST,
    PRESENT,
}

enum class PublicationType {
    ARTICLE,
    BOOK,
    BOOK_PART,
    CONFERENCE_PAPER,
    PERIODICAL_PART,
    PROCEEDINGS,
    RESEARCH_REPORT,
    THESIS,
    WORKING_PAPER,
    OTHER,
}

enum class BasisStorage {
    AUTHOR_RIGHT_EXCEPTION,
    LICENCE_CONTRACT,
    OPEN_CONTENT_LICENCE,
    USER_AGREEMENT,
    ZBW_POLICY_RESTRICTED,
    ZBW_POLICY_UNANSWERED,
}

enum class BasisAccessState {
    AUTHOR_RIGHT_EXCEPTION,
    LICENCE_CONTRACT,
    LICENCE_CONTRACT_OA,
    OPEN_CONTENT_LICENCE,
    USER_AGREEMENT,
    ZBW_POLICY,
}

data class ItemRight(
    val rightId: String?,
    val accessState: AccessState?,
    val authorRightException: Boolean?,
    val basisAccessState: BasisAccessState?,
    val basisStorage: BasisStorage?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val endDate: LocalDate?,
    val exceptionFrom: String?,
    val groupIds: List<String>?,
    val isTemplate: Boolean,
    val lastAppliedOn: OffsetDateTime?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val licenceContract: String?,
    val nonStandardOpenContentLicence: Boolean?,
    val nonStandardOpenContentLicenceURL: String?,
    val notesGeneral: String?,
    val notesFormalRules: String?,
    val notesProcessDocumentation: String?,
    val notesManagementRelated: String?,
    val openContentLicence: String?,
    val restrictedOpenContentLicence: Boolean?,
    val startDate: LocalDate,
    val templateDescription: String?,
    val templateName: String?,
    val zbwUserAgreement: Boolean?,
)

enum class FormalRule {
    LICENCE_CONTRACT,
    OPEN_CONTENT_LICENCE,
    ZBW_USER_AGREEMENT;
}
