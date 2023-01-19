package de.zbw.api.lori.server.type

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.UserRole
import de.zbw.lori.model.AccessStateRest
import de.zbw.lori.model.GroupRest
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PublicationTypeRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.RoleRest
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.LogManager
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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

fun Group.toRest() =
    GroupRest(
        name = this.name,
        description = this.description,
        ipAddresses = this.entry.joinToString(separator = "\n") {
            "${it.organisationName}${RestConverter.CSV_DELIMITER}${it.ipAddresses}"
        },
        hasCSVHeader = false,
    )

/**
 * Conversion throws an IllegalArgumentException if
 * the string representing the CSV file does not satisfy
 * the expected format.
 */
fun GroupRest.toBusiness() =
    Group(
        name = this.name,
        description = this.description,
        entry = RestConverter.parseToGroup(
            this.hasCSVHeader,
            this.ipAddresses
        )
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
        publicationType = publicationType.toBusiness(),
        publicationDate = publicationDate,
        rightsK10plus = rightsK10plus,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zdbId = zdbId,
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
        publicationType = publicationType.toRest(),
        publicationDate = publicationDate,
        rightsK10plus = rightsK10plus,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zdbId = zdbId,
    )

fun RightRest.toBusiness(): ItemRight =
    ItemRight(
        rightId = rightId,
        accessState = accessState?.toBusiness(),
        authorRightException = authorRightException,
        basisAccessState = basisAccessState?.toBusiness(),
        basisStorage = basisStorage?.toBusiness(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceContract = licenceContract,
        nonStandardOpenContentLicence = nonStandardOpenContentLicence,
        nonStandardOpenContentLicenceURL = nonStandardOpenContentLicenceURL,
        notesGeneral = notesGeneral,
        notesFormalRules = notesFormalRules,
        notesProcessDocumentation = notesProcessDocumentation,
        notesManagementRelated = notesManagementRelated,
        openContentLicence = openContentLicence,
        restrictedOpenContentLicence = restrictedOpenContentLicence,
        startDate = startDate,
        zbwUserAgreement = zbwUserAgreement,
    )

fun ItemRight.toRest(): RightRest =
    RightRest(
        rightId = rightId,
        accessState = accessState?.toRest(),
        authorRightException = authorRightException,
        basisAccessState = basisAccessState?.toRest(),
        basisStorage = basisStorage?.toRest(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceContract = licenceContract,
        nonStandardOpenContentLicence = nonStandardOpenContentLicence,
        nonStandardOpenContentLicenceURL = nonStandardOpenContentLicenceURL,
        notesGeneral = notesGeneral,
        notesFormalRules = notesFormalRules,
        notesProcessDocumentation = notesProcessDocumentation,
        notesManagementRelated = notesManagementRelated,
        openContentLicence = openContentLicence,
        restrictedOpenContentLicence = restrictedOpenContentLicence,
        startDate = startDate,
        zbwUserAgreement = zbwUserAgreement,
    )

internal fun AccessStateRest.toBusiness(): AccessState =
    when (this) {
        AccessStateRest.closed -> AccessState.CLOSED
        AccessStateRest.open -> AccessState.OPEN
        AccessStateRest.restricted -> AccessState.RESTRICTED
    }

internal fun AccessState.toRest(): AccessStateRest =
    when (this) {
        AccessState.CLOSED -> AccessStateRest.closed
        AccessState.OPEN -> AccessStateRest.open
        AccessState.RESTRICTED -> AccessStateRest.restricted
    }

internal fun RightRest.BasisAccessState.toBusiness(): BasisAccessState =
    when (this) {
        RightRest.BasisAccessState.authorRightException -> BasisAccessState.AUTHOR_RIGHT_EXCEPTION
        RightRest.BasisAccessState.licenceContract -> BasisAccessState.LICENCE_CONTRACT
        RightRest.BasisAccessState.licenceContractOa -> BasisAccessState.LICENCE_CONTRACT_OA
        RightRest.BasisAccessState.openContentLicence -> BasisAccessState.OPEN_CONTENT_LICENCE
        RightRest.BasisAccessState.userAgreement -> BasisAccessState.USER_AGREEMENT
        RightRest.BasisAccessState.zbwPolicy -> BasisAccessState.ZBW_POLICY
    }

internal fun BasisAccessState.toRest(): RightRest.BasisAccessState =
    when (this) {
        BasisAccessState.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisAccessState.authorRightException
        BasisAccessState.LICENCE_CONTRACT -> RightRest.BasisAccessState.licenceContract
        BasisAccessState.LICENCE_CONTRACT_OA -> RightRest.BasisAccessState.licenceContractOa
        BasisAccessState.OPEN_CONTENT_LICENCE -> RightRest.BasisAccessState.openContentLicence
        BasisAccessState.USER_AGREEMENT -> RightRest.BasisAccessState.userAgreement
        BasisAccessState.ZBW_POLICY -> RightRest.BasisAccessState.zbwPolicy
    }

internal fun RightRest.BasisStorage.toBusiness(): BasisStorage =
    when (this) {
        RightRest.BasisStorage.authorRightException -> BasisStorage.AUTHOR_RIGHT_EXCEPTION
        RightRest.BasisStorage.licenceContract -> BasisStorage.LICENCE_CONTRACT
        RightRest.BasisStorage.openContentLicence -> BasisStorage.LICENCE_CONTRACT
        RightRest.BasisStorage.userAgreement -> BasisStorage.USER_AGREEMENT
        RightRest.BasisStorage.zbwPolicyRestricted -> BasisStorage.ZBW_POLICY_RESTRICTED
        RightRest.BasisStorage.zbwPolicyUnanswered -> BasisStorage.ZBW_POLICY_UNANSWERED
    }

internal fun BasisStorage.toRest(): RightRest.BasisStorage =
    when (this) {
        BasisStorage.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisStorage.authorRightException
        BasisStorage.LICENCE_CONTRACT -> RightRest.BasisStorage.licenceContract
        BasisStorage.OPEN_CONTENT_LICENCE -> RightRest.BasisStorage.openContentLicence
        BasisStorage.USER_AGREEMENT -> RightRest.BasisStorage.userAgreement
        BasisStorage.ZBW_POLICY_RESTRICTED -> RightRest.BasisStorage.zbwPolicyRestricted
        BasisStorage.ZBW_POLICY_UNANSWERED -> RightRest.BasisStorage.zbwPolicyUnanswered
    }

internal fun PublicationTypeRest.toBusiness(): PublicationType =
    when (this) {
        PublicationTypeRest.article -> PublicationType.ARTICLE
        PublicationTypeRest.book -> PublicationType.BOOK
        PublicationTypeRest.bookPart -> PublicationType.BOOK_PART
        PublicationTypeRest.periodicalPart -> PublicationType.PERIODICAL_PART
        PublicationTypeRest.workingPaper -> PublicationType.WORKING_PAPER
        PublicationTypeRest.researchReport -> PublicationType.RESEARCH_REPORT
        PublicationTypeRest.proceedings -> PublicationType.PROCEEDINGS
        PublicationTypeRest.thesis -> PublicationType.THESIS
        PublicationTypeRest.conferencePaper -> PublicationType.CONFERENCE_PAPER
    }

internal fun PublicationType.toRest(): PublicationTypeRest =
    when (this) {
        PublicationType.ARTICLE -> PublicationTypeRest.article
        PublicationType.BOOK -> PublicationTypeRest.book
        PublicationType.BOOK_PART -> PublicationTypeRest.bookPart
        PublicationType.CONFERENCE_PAPER -> PublicationTypeRest.conferencePaper
        PublicationType.PERIODICAL_PART -> PublicationTypeRest.periodicalPart
        PublicationType.WORKING_PAPER -> PublicationTypeRest.workingPaper
        PublicationType.RESEARCH_REPORT -> PublicationTypeRest.researchReport
        PublicationType.PROCEEDINGS -> PublicationTypeRest.proceedings
        PublicationType.THESIS -> PublicationTypeRest.thesis
    }

fun DAItem.toBusiness(): ItemMetadata? {
    val metadata = this.metadata
    val handle = RestConverter.extractMetadata("dc.identifier.uri", metadata)
    val publicationType = RestConverter.extractMetadata("dc.type", metadata)?.let {
        PublicationType.valueOf(it.uppercase().replace(oldChar = ' ', newChar = '_'))
    }
    val publicationDate = RestConverter.extractMetadata("dc.date.issued", metadata)
    val title = RestConverter.extractMetadata("dc.title", metadata)

    return if (
        handle == null ||
        publicationDate == null ||
        publicationType == null ||
        title == null
    ) {
        null
    } else {
        ItemMetadata(
            metadataId = this.id.toString(),
            author = RestConverter.extractMetadata("dc.contributor.author", metadata),
            band = null, // Not in DA yet
            collectionName = this.parentCollection?.name,
            communityName = this.parentCommunityList.takeIf { it.isNotEmpty() }?.first()?.name,
            createdBy = null,
            createdOn = null,
            doi = RestConverter.extractMetadata("dc.identifier.pi", metadata),
            handle = handle,
            isbn = RestConverter.extractMetadata("dc.identifier.isbn", metadata),
            issn = RestConverter.extractMetadata("dc.identifier.issn", metadata),
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            paketSigel = RestConverter.extractMetadata("dc.identifier.packageid", metadata),
            ppn = RestConverter.extractMetadata("dc.identifier.ppn", metadata),
            publicationType = publicationType,
            publicationDate = RestConverter.parseToDate(publicationDate),
            rightsK10plus = RestConverter.extractMetadata("dc.rights", metadata),
            storageDate = RestConverter.extractMetadata("dc.date.accessioned", metadata)
                ?.let { OffsetDateTime.parse(it) },
            title = title,
            titleJournal = RestConverter.extractMetadata("dc.journalname", metadata),
            titleSeries = RestConverter.extractMetadata("dc.seriesname", metadata),
            zdbId = RestConverter.extractMetadata("dc.relation.journalzdbid", metadata),
        )
    }
}

fun RoleRest.Role.toBusiness(): UserRole =
    when (this) {
        RoleRest.Role.readOnly -> UserRole.READONLY
        RoleRest.Role.readWrite -> UserRole.READWRITE
        RoleRest.Role.admin -> UserRole.ADMIN
    }

/**
 * Utility functions helping to perform convert
 * REST to Business Object and vice versa.
 */
object RestConverter {
    fun extractMetadata(key: String, metadata: List<DAMetadata>): String? =
        metadata.filter { dam -> dam.key == key }.takeIf { it.isNotEmpty() }?.first()?.value

    fun parseToDate(s: String): LocalDate {
        return if (s.matches("\\d{4}-\\d{2}-\\d{2}".toRegex())) {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
        } else if (s.matches("\\d{4}-\\d{2}".toRegex())) {
            LocalDate.parse("$s-01", DateTimeFormatter.ISO_LOCAL_DATE)
        } else if (s.matches("\\d{4}/\\d{2}".toRegex())) {
            LocalDate.parse(
                "${s.substringBefore('/')}-${s.substringAfter('/')}-01",
                DateTimeFormatter.ISO_LOCAL_DATE
            )
        } else if (s.matches("\\d{4}".toRegex())) {
            LocalDate.parse("$s-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            LOG.warn("Date format can't be recognized: $s")
            LocalDate.parse("1970-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    /**
     * Parse CSV formatted string.
     * The expected format is as following:
     * organisation_name,ipAddress
     */
    fun parseToGroup(
        hasCSVHeader: Boolean,
        ipAddressesCSV: String
    ): List<GroupEntry> =
        try {
            val csvFormat: CSVFormat = CSVFormat.Builder.create()
                .setDelimiter(';')
                .setQuote(Character.valueOf('"'))
                .setRecordSeparator("\r\n")
                .build()
            CSVFormat.Builder.create(csvFormat).apply {
                setIgnoreSurroundingSpaces(true)
            }.build()
                .let { CSVParser.parse(ipAddressesCSV, it) }
                .let {
                    if (hasCSVHeader) {
                        it.drop(1)
                    } else {
                        it
                    }
                } // Dropping the header
                .map {
                    GroupEntry(
                        organisationName = it[0],
                        ipAddresses = it[1],
                    )
                }
        } catch (e: Exception) {
            throw IllegalArgumentException()
        }

    private val LOG = LogManager.getLogger(RestConverter::class.java)
    const val CSV_DELIMITER = ";"
}
