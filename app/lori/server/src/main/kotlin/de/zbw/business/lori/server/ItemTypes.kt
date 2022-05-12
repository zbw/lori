package de.zbw.business.lori.server

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
    val collectionName: String?,
    val communityName: String?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val doi: String?,
    val handle: String,
    val isbn: String?,
    val issn: String?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val paketSigel: String?,
    val ppn: String?,
    val ppnEbook: String?,
    val publicationType: PublicationType,
    val publicationYear: Int,
    val rightsK10plus: String?,
    val serialNumber: String?,
    val storageDate: OffsetDateTime,
    val title: String,
    val titleJournal: String?,
    val titleSeries: String?,
    val zbdId: String?,
)

enum class AccessState {
    CLOSED,
    OPEN,
    RESTRICTED,
}

enum class PublicationType {
    ARTICLE,
    BOOK,
}

data class ItemRight(
    val rightId: String,
    val accessState: AccessState?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val endDate: LocalDate,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
    val licenseConditions: String?,
    val startDate: LocalDate,
    val provenanceLicense: String?,
)
