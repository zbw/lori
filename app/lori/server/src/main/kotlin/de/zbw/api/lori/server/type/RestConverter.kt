package de.zbw.api.lori.server.type

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.UserPermission
import de.zbw.lori.model.AccessStateRest
import de.zbw.lori.model.AccessStateWithCountRest
import de.zbw.lori.model.BookmarkRawRest
import de.zbw.lori.model.BookmarkRest
import de.zbw.lori.model.BookmarkTemplateRest
import de.zbw.lori.model.ConflictTypeRest
import de.zbw.lori.model.FilterPublicationDateRest
import de.zbw.lori.model.GroupRest
import de.zbw.lori.model.ItemInformation
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.PaketSigelWithCountRest
import de.zbw.lori.model.PublicationTypeRest
import de.zbw.lori.model.PublicationTypeWithCountRest
import de.zbw.lori.model.RightErrorRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.TemplateNameWithCountRest
import de.zbw.lori.model.UserPermissionRest
import de.zbw.lori.model.UserSessionRest
import de.zbw.lori.model.ZdbIdWithCountRest
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

/**
 * Conversion functions between rest interface and business logic.
 *
 * Created on 07-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun ItemRest.toBusiness() =
    Item(
        metadata = metadata.toBusiness(),
        rights = rights.map { it.toBusiness() },
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
        ipAddresses = this.entries.joinToString(separator = "\n") {
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
        entries = RestConverter.parseToGroup(
            this.hasCSVHeader,
            this.ipAddresses
        )
    )

fun MetadataRest.toBusiness() =
    ItemMetadata(
        metadataId = metadataId,
        author = author,
        band = band,
        collectionHandle = collectionHandle,
        collectionName = collectionName,
        communityHandle = communityHandle,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceUrl = licenceUrl,
        paketSigel = paketSigel,
        ppn = ppn,
        publicationType = publicationType.toBusiness(),
        publicationDate = publicationDate,
        rightsK10plus = rightsK10plus,
        subCommunityHandle = subCommunityHandle,
        subCommunityName = subCommunityName,
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
        collectionHandle = collectionHandle,
        collectionName = collectionName,
        communityHandle = communityHandle,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceUrl = licenceUrl,
        paketSigel = paketSigel,
        ppn = ppn,
        publicationType = publicationType.toRest(),
        publicationDate = publicationDate,
        rightsK10plus = rightsK10plus,
        storageDate = storageDate,
        subCommunityHandle = subCommunityHandle,
        subCommunityName = subCommunityName,
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
        exceptionFrom = exceptionFrom,
        groupIds = groupIds,
        isTemplate = isTemplate,
        lastAppliedOn = lastAppliedOn,
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
        templateDescription = templateDescription,
        templateName = templateName,
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
        exceptionFrom = exceptionFrom,
        groupIds = groupIds,
        isTemplate = isTemplate,
        lastAppliedOn = lastAppliedOn,
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
        templateDescription = templateDescription,
        templateName = templateName,
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
        RightRest.BasisAccessState.authorrightexception -> BasisAccessState.AUTHOR_RIGHT_EXCEPTION
        RightRest.BasisAccessState.licencecontract -> BasisAccessState.LICENCE_CONTRACT
        RightRest.BasisAccessState.licencecontractoa -> BasisAccessState.LICENCE_CONTRACT_OA
        RightRest.BasisAccessState.opencontentlicence -> BasisAccessState.OPEN_CONTENT_LICENCE
        RightRest.BasisAccessState.useragreement -> BasisAccessState.USER_AGREEMENT
        RightRest.BasisAccessState.zbwpolicy -> BasisAccessState.ZBW_POLICY
    }

internal fun BasisAccessState.toRest(): RightRest.BasisAccessState =
    when (this) {
        BasisAccessState.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisAccessState.authorrightexception
        BasisAccessState.LICENCE_CONTRACT -> RightRest.BasisAccessState.licencecontract
        BasisAccessState.LICENCE_CONTRACT_OA -> RightRest.BasisAccessState.licencecontractoa
        BasisAccessState.OPEN_CONTENT_LICENCE -> RightRest.BasisAccessState.opencontentlicence
        BasisAccessState.USER_AGREEMENT -> RightRest.BasisAccessState.useragreement
        BasisAccessState.ZBW_POLICY -> RightRest.BasisAccessState.zbwpolicy
    }

internal fun RightRest.BasisStorage.toBusiness(): BasisStorage =
    when (this) {
        RightRest.BasisStorage.authorrightexception -> BasisStorage.AUTHOR_RIGHT_EXCEPTION
        RightRest.BasisStorage.licencecontract -> BasisStorage.LICENCE_CONTRACT
        RightRest.BasisStorage.opencontentlicence -> BasisStorage.LICENCE_CONTRACT
        RightRest.BasisStorage.useragreement -> BasisStorage.USER_AGREEMENT
        RightRest.BasisStorage.zbwpolicyrestricted -> BasisStorage.ZBW_POLICY_RESTRICTED
        RightRest.BasisStorage.zbwpolicyunanswered -> BasisStorage.ZBW_POLICY_UNANSWERED
    }

internal fun BasisStorage.toRest(): RightRest.BasisStorage =
    when (this) {
        BasisStorage.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisStorage.authorrightexception
        BasisStorage.LICENCE_CONTRACT -> RightRest.BasisStorage.licencecontract
        BasisStorage.OPEN_CONTENT_LICENCE -> RightRest.BasisStorage.opencontentlicence
        BasisStorage.USER_AGREEMENT -> RightRest.BasisStorage.useragreement
        BasisStorage.ZBW_POLICY_RESTRICTED -> RightRest.BasisStorage.zbwpolicyrestricted
        BasisStorage.ZBW_POLICY_UNANSWERED -> RightRest.BasisStorage.zbwpolicyunanswered
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

fun DAItem.toBusiness(directParentCommunityId: Int, LOG: Logger): ItemMetadata? {
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
        var subDACommunity: DACommunity? = null
        val parentDACommunity: DACommunity?
        when (this.parentCommunityList.size) {
            2 -> {
                subDACommunity = this.parentCommunityList.firstOrNull { it.id == directParentCommunityId }
                parentDACommunity = this.parentCommunityList.firstOrNull { it.id != directParentCommunityId }
            }
            1 -> {
                parentDACommunity = this.parentCommunityList.firstOrNull()
            }
            else -> {
                // This case should not happen. If it does however, a warning will be printed and the item will be irgnored for now.
                LOG.warn("Invalid numbers of parent communities (should be 1 or 2): MetadataId ${this.id}")
                return null
            }
        }
        ItemMetadata(
            metadataId = this.id.toString(),
            author = RestConverter.extractMetadata("dc.contributor.author", metadata),
            band = null, // Not in DA yet
            collectionHandle = this.parentCollection?.handle?.let {
                RestConverter.parseHandle(it)
            },
            collectionName = this.parentCollection?.name,
            communityHandle = parentDACommunity?.handle?.let {
                RestConverter.parseHandle(it)
            },
            communityName = parentDACommunity?.name,
            createdBy = null,
            createdOn = null,
            doi = RestConverter.extractMetadata("dc.identifier.pi", metadata),
            handle = RestConverter.parseHandle(handle),
            isbn = RestConverter.extractMetadata("dc.identifier.isbn", metadata),
            issn = RestConverter.extractMetadata("dc.identifier.issn", metadata),
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            licenceUrl = RestConverter.extractMetadata("dc.rights.license", metadata),
            paketSigel = RestConverter.extractMetadata("dc.identifier.packageid", metadata),
            ppn = RestConverter.extractMetadata("dc.identifier.ppn", metadata),
            publicationType = publicationType,
            publicationDate = RestConverter.parseToDate(publicationDate),
            rightsK10plus = RestConverter.extractMetadata("dc.rights", metadata),
            subCommunityHandle = subDACommunity?.handle?.let {
                RestConverter.parseHandle(it)
            },
            subCommunityName = subDACommunity?.name,
            storageDate = RestConverter.extractMetadata("dc.date.accessioned", metadata)
                ?.let { OffsetDateTime.parse(it) },
            title = title,
            titleJournal = RestConverter.extractMetadata("dc.journalname", metadata),
            titleSeries = RestConverter.extractMetadata("dc.seriesname", metadata),
            zdbId = RestConverter.extractMetadata("dc.relation.journalzdbid", metadata),
        )
    }
}

fun UserSession.toRest(): UserSessionRest =
    UserSessionRest(
        email = this.email,
        permissions = this.permissions.map { it.toRest() },
        sessionId = this.sessionId,
    )

fun UserSessionRest.toBusiness(): UserSession =
    UserSession(
        email = this.email,
        permissions = this.permissions?.map { it.toBusiness() } ?: emptyList(),
        sessionId = this.sessionId,
    )

fun UserPermissionRest.toBusiness(): UserPermission =
    when (this) {
        UserPermissionRest.read -> UserPermission.READ
        UserPermissionRest.write -> UserPermission.WRITE
        UserPermissionRest.admin -> UserPermission.ADMIN
    }

fun UserPermission.toRest(): UserPermissionRest =
    when (this) {
        UserPermission.READ -> UserPermissionRest.read
        UserPermission.WRITE -> UserPermissionRest.write
        UserPermission.ADMIN -> UserPermissionRest.admin
    }

fun BookmarkRawRest.toBusiness(): Bookmark =
    Bookmark(
        bookmarkName = this.bookmarkName,
        bookmarkId = this.bookmarkId,
        description = this.description,
        searchTerm = this.searchTerm,
        publicationDateFilter = QueryParameterParser.parsePublicationDateFilter(this.filterPublicationDate),
        publicationTypeFilter = QueryParameterParser.parsePublicationTypeFilter(this.filterPublicationType),
        paketSigelFilter = QueryParameterParser.parsePaketSigelFilter(this.filterPaketSigel),
        zdbIdFilter = QueryParameterParser.parseZDBIdFilter(this.filterZDBId),
        accessStateFilter = QueryParameterParser.parseAccessStateFilter(this.filterAccessState),
        temporalValidityFilter = QueryParameterParser.parseTemporalValidity(this.filterTemporalValidity),
        formalRuleFilter = QueryParameterParser.parseFormalRuleFilter(this.filterFormalRule),
        startDateFilter = QueryParameterParser.parseStartDateFilter(this.filterStartDate),
        endDateFilter = QueryParameterParser.parseEndDateFilter(this.filterEndDate),
        validOnFilter = QueryParameterParser.parseRightValidOnFilter(this.filterValidOn),
        noRightInformationFilter = QueryParameterParser.parseNoRightInformationFilter(this.filterNoRightInformation),
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        createdBy = createdBy,
        createdOn = createdOn,
    )

fun BookmarkRest.toBusiness(): Bookmark =
    Bookmark(
        bookmarkName = this.bookmarkName,
        bookmarkId = this.bookmarkId,
        description = this.description,
        searchTerm = this.searchTerm,
        publicationDateFilter = PublicationDateFilter(
            fromYear = this.filterPublicationDate?.fromYear ?: PublicationDateFilter.MIN_YEAR,
            toYear = this.filterPublicationDate?.toYear ?: PublicationDateFilter.MAX_YEAR,
        ),
        publicationTypeFilter = QueryParameterParser.parsePublicationTypeFilter(
            this.filterPublicationType?.joinToString(
                separator = ","
            )
        ),
        paketSigelFilter = QueryParameterParser.parsePaketSigelFilter(this.filterPaketSigel?.joinToString(separator = ",")),
        zdbIdFilter = QueryParameterParser.parseZDBIdFilter(this.filterZDBId?.joinToString(separator = ",")),
        accessStateFilter = QueryParameterParser.parseAccessStateFilter(this.filterAccessState?.joinToString(separator = ",")),
        temporalValidityFilter = QueryParameterParser.parseTemporalValidity(
            this.filterTemporalValidity?.joinToString(
                separator = ","
            )
        ),
        formalRuleFilter = QueryParameterParser.parseFormalRuleFilter(this.filterFormalRule?.joinToString(separator = ",")),
        startDateFilter = this.filterStartDate?.let { StartDateFilter(it) },
        endDateFilter = this.filterEndDate?.let { EndDateFilter(it) },
        validOnFilter = this.filterValidOn?.let { RightValidOnFilter(it) },
        noRightInformationFilter = this.filterNoRightInformation?.takeIf { it }?.let { NoRightInformationFilter() },
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        createdBy = createdBy,
        createdOn = createdOn,
    )

fun Bookmark.toRest(): BookmarkRest =
    BookmarkRest(
        bookmarkName = this.bookmarkName,
        bookmarkId = this.bookmarkId,
        description = this.description,
        searchTerm = this.searchTerm,
        filterPublicationDate = FilterPublicationDateRest(
            fromYear = this.publicationDateFilter?.fromYear,
            toYear = this.publicationDateFilter?.toYear,
        ),
        filterPublicationType = this.publicationTypeFilter?.publicationTypes?.map { it.toString() },
        filterAccessState = this.accessStateFilter?.accessStates?.map { it.toString() },
        filterTemporalValidity = this.temporalValidityFilter?.temporalValidity?.map { it.toString() },
        filterStartDate = this.startDateFilter?.date,
        filterEndDate = this.endDateFilter?.date,
        filterFormalRule = this.formalRuleFilter?.formalRules?.map { it.toString() },
        filterValidOn = this.validOnFilter?.date,
        filterPaketSigel = this.paketSigelFilter?.paketSigels,
        filterZDBId = this.zdbIdFilter?.zdbIds,
        filterNoRightInformation = this.noRightInformationFilter?.let { true } ?: false,
        createdBy = this.createdBy,
        createdOn = this.createdOn,
        lastUpdatedBy = this.lastUpdatedBy,
        lastUpdatedOn = this.lastUpdatedOn,
    )

fun BookmarkTemplateRest.toBusiness(): BookmarkTemplate =
    BookmarkTemplate(
        bookmarkId = this.bookmarkId,
        rightId = this.rightId,
    )

fun BookmarkTemplate.toRest(): BookmarkTemplateRest =
    BookmarkTemplateRest(
        bookmarkId = this.bookmarkId,
        rightId = this.rightId,
    )

fun SearchQueryResult.toRest(
    pageSize: Int,
): ItemInformation {
    val totalPages = ceil(this.numberOfResults.toDouble() / pageSize.toDouble()).toInt()
    return ItemInformation(
        itemArray = this.results.map { it.toRest() },
        totalPages = totalPages,
        accessStateWithCount = this.accessState.entries.map {
            AccessStateWithCountRest(it.key.toRest(), it.value)
        }.toList(),
        hasLicenceContract = this.hasLicenceContract,
        hasOpenContentLicence = this.hasOpenContentLicence,
        hasZbwUserAgreement = this.hasZbwUserAgreement,
        numberOfResults = this.numberOfResults,
        paketSigelWithCount = this.paketSigels.entries
            .map { PaketSigelWithCountRest(count = it.value, paketSigel = it.key) }.toList(),
        publicationTypeWithCount = this.publicationType.entries.map {
            PublicationTypeWithCountRest(
                count = it.value,
                publicationType = it.key.toRest(),
            )
        }.toList(),
        zdbIdWithCount = this.zdbIds.entries.map {
            ZdbIdWithCountRest(
                count = it.value,
                zdbId = it.key,
            )
        }.toList(),
        templateNameWithCount = this.templateNamesToOcc.entries.map {
            TemplateNameWithCountRest(
                templateName = it.value.first,
                count = it.value.second,
                rightId = it.key,
            )
        }
    )
}

fun ConflictType.toRest(): ConflictTypeRest =
    when (this) {
        ConflictType.DATE_OVERLAP -> ConflictTypeRest.dateOverlap
        ConflictType.UNSPECIFIED -> ConflictTypeRest.unspecified
    }

fun RightError.toRest(): RightErrorRest =
    RightErrorRest(
        errorId = errorId,
        conflictingRightId = conflictingRightId,
        createdOn = createdOn,
        message = message,
        handleId = handleId,
        metadataId = metadataId,
        rightIdSource = rightIdSource,
        conflictType = conflictType?.toRest(),
    )

/**
 * Utility functions helping to convert
 * REST to Business Objects and vice versa.
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

    fun parseHandle(given: String): String =
        given.replace("^[\\D]*".toRegex(), "")

    private val LOG = LogManager.getLogger(RestConverter::class.java)
    const val CSV_DELIMITER = ";"
}
