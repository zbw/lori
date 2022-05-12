package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.Item
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.PublicationType
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.RightRest
import java.time.OffsetDateTime

/**
 * Conversion functions between rest interface and business logic.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */

fun ItemRest.toBusiness() =
    Item(
        metadata = metadata.toBusiness(),
        rights = rights?.map { it.toBusiness() } ?: emptyList(),
    )

fun Item.toRest() =
    ItemRest(
        metadata = metadata.toRest(),
        rights = rights.map { it.toRest() },
    )

fun MetadataRest.toBusiness() =
    ItemMetadata(
        metadataId = metadataId,
        author = author,
        band = band,
        collectionName = collectionName,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        paketSigel = paketSigel,
        ppn = ppn,
        ppnEbook = ppnEbook,
        publicationType = publicationType.toBusiness(),
        publicationYear = publicationYear,
        rightsK10plus = rightsK10plus,
        serialNumber = serialNumber,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zbdId = zbdId,
    )

fun ItemMetadata.toRest(): MetadataRest =
    MetadataRest(
        metadataId = metadataId,
        author = author,
        band = band,
        collectionName = collectionName,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        paketSigel = paketSigel,
        ppn = ppn,
        ppnEbook = ppnEbook,
        publicationType = publicationType.toRest(),
        publicationYear = publicationYear,
        rightsK10plus = rightsK10plus,
        serialNumber = serialNumber,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zbdId = zbdId,
    )

fun RightRest.toBusiness(): ItemRight =
    ItemRight(
        rightId = rightId,
        accessState = accessState?.toBusiness(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenseConditions = licenseConditions,
        provenanceLicense = provenanceLicense,
        startDate = startDate,
    )

fun ItemRight.toRest(): RightRest =
    RightRest(
        rightId = rightId,
        accessState = accessState?.toRest(),
        createdBy = createdBy,
        createdOn = createdOn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        startDate = startDate,
        endDate = endDate,
        provenanceLicense = provenanceLicense,
        licenseConditions = licenseConditions,
    )

internal fun RightRest.AccessState.toBusiness(): AccessState =
    when (this) {
        RightRest.AccessState.closed -> AccessState.CLOSED
        RightRest.AccessState.open -> AccessState.OPEN
        RightRest.AccessState.restricted -> AccessState.RESTRICTED
    }

internal fun AccessState.toRest(): RightRest.AccessState =
    when (this) {
        AccessState.CLOSED -> RightRest.AccessState.closed
        AccessState.OPEN -> RightRest.AccessState.open
        AccessState.RESTRICTED -> RightRest.AccessState.restricted
    }

internal fun MetadataRest.PublicationType.toBusiness(): PublicationType =
    when (this) {
        MetadataRest.PublicationType.article -> PublicationType.ARTICLE
        MetadataRest.PublicationType.book -> PublicationType.BOOK
    }

internal fun PublicationType.toRest(): MetadataRest.PublicationType =
    when (this) {
        PublicationType.ARTICLE -> MetadataRest.PublicationType.article
        PublicationType.BOOK -> MetadataRest.PublicationType.book
    }

fun DAItem.toBusiness(): ItemMetadata? {
    val metadata = this.metadata
    val handle = extractMetadata("dc.identifier.uri", metadata)
    val publicationType = extractMetadata("dc.type", metadata)?.let {
        PublicationType.valueOf(it.uppercase())
    }
    val publicationYear = extractMetadata("dc.date.issued", metadata)?.toInt()
    val title = extractMetadata("dc.title", metadata)

    return if (
        handle == null ||
        publicationYear == null ||
        publicationType == null ||
        title == null
    ) {
        null
    } else {
        ItemMetadata(
            metadataId = this.id.toString(),
            author = extractMetadata("dc.contributor.author", metadata),
            band = null, // Not in DA yet
            collectionName = this.parentCollection?.name,
            communityName = this.parentCommunityList.takeIf { it.isNotEmpty() }?.first()?.name,
            createdBy = null,
            createdOn = null,
            doi = extractMetadata("dc.identifier.pi", metadata),
            handle = handle,
            isbn = extractMetadata("dc.identifier.isbn", metadata),
            issn = extractMetadata("dc.identifier.issn", metadata),
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            paketSigel = null, // Not in DA yet
            ppn = extractMetadata("dc.identifier.ppn", metadata),
            ppnEbook = null, // Not in DA yet
            publicationType = publicationType,
            publicationYear = publicationYear,
            rightsK10plus = extractMetadata("dc.rights", metadata),
            serialNumber = null, // Not in DA yet
            storageDate = OffsetDateTime.parse(extractMetadata("dc.date.accessioned", metadata)),
            title = title,
            titleJournal = extractMetadata("dc.journalname", metadata),
            titleSeries = extractMetadata("dc.seriesname", metadata),
            zbdId = null, // Not in DA yet
        )
    }
}

private fun extractMetadata(key: String, metadata: List<DAMetadata>): String? =
    metadata.filter { dam -> dam.key == key }.takeIf { it.isNotEmpty() }?.first()?.value
