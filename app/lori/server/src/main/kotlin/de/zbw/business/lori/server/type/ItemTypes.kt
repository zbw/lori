package de.zbw.business.lori.server.type

import java.time.LocalDate
import java.time.OffsetDateTime

data class Item(
    val metadata: ItemMetadata,
    val rights: List<ItemRight>,
)

data class ItemId(
    val handle: String,
    val rightId: String,
)

data class ItemMetadata(
    val author: String?,
    val band: String?,
    val collectionHandle: String?,
    val collectionName: String?,
    val communityHandle: String?,
    val communityName: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val deleted: Boolean,
    val doi: List<String>?,
    val handle: String,
    val isbn: List<String>?,
    val issn: String?,
    val isPartOfSeries: List<String>?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val licenceUrl: String?,
    val licenceUrlFilter: String?,
    val paketSigel: List<String>?,
    val ppn: String?,
    val publicationType: PublicationType,
    val publicationYear: Int?,
    val subCommunityHandle: String?,
    val subCommunityName: String?,
    val storageDate: OffsetDateTime?,
    val title: String,
    val titleJournal: String?,
    val titleSeries: String?,
    val zdbIds: List<String>?,
)

enum class AccessState(
    val priority: Int,
) {
    OPEN(1),
    RESTRICTED(2),
    CLOSED(3),
}

enum class PublicationType(
    val priority: Int,
) {
    ARTICLE(1),
    BOOK(2),
    BOOK_PART(3),
    CONFERENCE_PAPER(4),
    OTHER(5),
    PERIODICAL_PART(6),
    PROCEEDING(7),
    RESEARCH_REPORT(8),
    THESIS(9),
    WORKING_PAPER(10),
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
    val basisAccessState: BasisAccessState?,
    val basisStorage: BasisStorage?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val endDate: LocalDate?,
    val exceptionOfId: String?,
    val hasExceptionId: String?,
    val groupIds: List<Int>?,
    val groups: List<Group>?,
    val isTemplate: Boolean,
    val lastAppliedOn: OffsetDateTime?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val licenceContract: String?,
    val hasLegalRisk: Boolean?,
    val notesGeneral: String?,
    val notesFormalRules: String?,
    val notesProcessDocumentation: String?,
    val notesManagementRelated: String?,
    val restrictedOpenContentLicence: Boolean?,
    val startDate: LocalDate,
    val templateDescription: String?,
    val templateName: String?,
    val zbwUserAgreement: Boolean?,
)

enum class FormalRule {
    LICENCE_CONTRACT,
    OPEN_CONTENT_LICENCE,
    ZBW_USER_AGREEMENT,
}
