package de.zbw.api.lori.server.type

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.api.lori.server.type.RestConverter.LOG
import de.zbw.api.lori.server.utils.RestConverterUtil.prepareLicenceUrlFilter
import de.zbw.business.lori.server.AccessStateOnDateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.ManualRightFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.RightIdFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.GroupVersion
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.RightIdTemplateName
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.business.lori.server.type.UserPermission
import de.zbw.lori.model.AccessStateRest
import de.zbw.lori.model.AccessStateWithCountRest
import de.zbw.lori.model.BookmarkRawRest
import de.zbw.lori.model.BookmarkRest
import de.zbw.lori.model.BookmarkTemplateRest
import de.zbw.lori.model.ConflictTypeRest
import de.zbw.lori.model.FilterAccessStateOnRest
import de.zbw.lori.model.FilterPublicationYearRest
import de.zbw.lori.model.FilterRightIdRest
import de.zbw.lori.model.GroupRest
import de.zbw.lori.model.IsPartOfSeriesCountRest
import de.zbw.lori.model.ItemInformation
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.LicenceUrlCountRest
import de.zbw.lori.model.MetadataRest
import de.zbw.lori.model.OldGroupVersionRest
import de.zbw.lori.model.OrganisationToIp
import de.zbw.lori.model.PaketSigelWithCountRest
import de.zbw.lori.model.PublicationTypeRest
import de.zbw.lori.model.PublicationTypeWithCountRest
import de.zbw.lori.model.RightErrorInformationRest
import de.zbw.lori.model.RightErrorRest
import de.zbw.lori.model.RightRest
import de.zbw.lori.model.TemplateApplicationRest
import de.zbw.lori.model.TemplateNameWithCountRest
import de.zbw.lori.model.UserPermissionRest
import de.zbw.lori.model.UserSessionRest
import de.zbw.lori.model.ZdbIdWithCountRest
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.LogManager
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.flatten
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
        groupId = this.groupId,
        description = this.description,
        allowedAddressesRaw =
            this.entries.joinToString(separator = "\n") {
                "${it.organisationName}${RestConverter.CSV_DELIMITER}${it.ipAddresses}"
            },
        hasCSVHeader = false,
        title = title,
        allowedAddresses =
            this.entries.map {
                OrganisationToIp(
                    ipv4Allowed = it.ipAddresses,
                    organisation = it.organisationName,
                )
            },
        createdBy = createdBy,
        createdOn = createdOn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        version = version,
        oldVersions = oldVersions?.map { it.toRest() },
    )

fun GroupVersion.toRest() =
    OldGroupVersionRest(
        createdOn = createdOn,
        createdBy = createdBy,
        version = version,
        description = description,
    )

/**
 * Conversion throws an IllegalArgumentException if
 * the string representing the CSV file does not satisfy
 * the expected format.
 */
fun GroupRest.toBusiness() =
    Group(
        groupId = this.groupId,
        description = this.description,
        entries =
            this.allowedAddressesRaw?.let {
                RestConverter.parseToGroup(
                    this.hasCSVHeader,
                    it,
                )
            } ?: emptyList(),
        title = title,
        createdBy = createdBy,
        createdOn = createdOn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        version = version,
        // History can't be changed through APIs ;)
        oldVersions = emptyList(),
    )

fun MetadataRest.toBusiness() =
    ItemMetadata(
        author = author,
        band = band,
        collectionHandle = collectionHandle,
        collectionName = collectionName,
        communityHandle = communityHandle,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        deleted = deleted,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        isPartOfSeries = isPartOfSeries,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceUrl = licenceUrl,
        licenceUrlFilter = prepareLicenceUrlFilter(licenceUrl),
        paketSigel = paketSigel,
        ppn = ppn,
        publicationType = publicationType.toBusiness(),
        publicationYear = publicationYear,
        subCommunityHandle = subCommunityHandle,
        subCommunityName = subCommunityName,
        storageDate = storageDate,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zdbIds = zdbIds,
    )

fun ItemMetadata.toRest(): MetadataRest =
    MetadataRest(
        author = author,
        band = band,
        collectionHandle = collectionHandle,
        collectionName = collectionName,
        communityHandle = communityHandle,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        deleted = deleted,
        doi = doi,
        handle = handle,
        isbn = isbn,
        issn = issn,
        isPartOfSeries = isPartOfSeries,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceUrl = licenceUrl,
        paketSigel = paketSigel,
        ppn = ppn,
        publicationType = publicationType.toRest(),
        publicationYear = publicationYear,
        storageDate = storageDate,
        subCommunityHandle = subCommunityHandle,
        subCommunityName = subCommunityName,
        title = title,
        titleJournal = titleJournal,
        titleSeries = titleSeries,
        zdbIds = zdbIds,
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
        groups = groups?.map { it.toBusiness() },
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
        templateName = templateName?.trim(),
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
        groupIds = groups?.map { it.groupId },
        groups = groups?.map { it.toRest() },
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
        RightRest.BasisStorage.opencontentlicence -> BasisStorage.OPEN_CONTENT_LICENCE
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
        PublicationTypeRest.book_part -> PublicationType.BOOK_PART
        PublicationTypeRest.periodical_part -> PublicationType.PERIODICAL_PART
        PublicationTypeRest.working_paper -> PublicationType.WORKING_PAPER
        PublicationTypeRest.research_report -> PublicationType.RESEARCH_REPORT
        PublicationTypeRest.proceeding -> PublicationType.PROCEEDING
        PublicationTypeRest.thesis -> PublicationType.THESIS
        PublicationTypeRest.conference_paper -> PublicationType.CONFERENCE_PAPER
        PublicationTypeRest.other -> PublicationType.OTHER
    }

internal fun PublicationType.toRest(): PublicationTypeRest =
    when (this) {
        PublicationType.ARTICLE -> PublicationTypeRest.article
        PublicationType.BOOK -> PublicationTypeRest.book
        PublicationType.BOOK_PART -> PublicationTypeRest.book_part
        PublicationType.CONFERENCE_PAPER -> PublicationTypeRest.conference_paper
        PublicationType.PERIODICAL_PART -> PublicationTypeRest.periodical_part
        PublicationType.WORKING_PAPER -> PublicationTypeRest.working_paper
        PublicationType.RESEARCH_REPORT -> PublicationTypeRest.research_report
        PublicationType.PROCEEDING -> PublicationTypeRest.proceeding
        PublicationType.THESIS -> PublicationTypeRest.thesis
        PublicationType.OTHER -> PublicationTypeRest.other
    }

fun DAItem.toBusiness(directParentCommunityId: Int): ItemMetadata? {
    val metadata = this.metadata
    val handle =
        RestConverter.extractMetadata("dc.identifier.uri", metadata)?.let {
            if (it.size > 1) {
                LOG.warn("Item has multiple handles: Handle ${this.handle}")
            }
            it[0]
        }
    val publicationType =
        try {
            RestConverter.extractMetadata("dc.type", metadata)?.let {
                if (it.size > 1) {
                    LOG.warn("Item has multiple publication types: Handle ${this.handle}")
                }
                PublicationType.valueOf(
                    it[0]
                        .uppercase()
                        .replace(oldChar = ' ', newChar = '_')
                        .replace("PROCEEDINGS", "PROCEEDING"),
                )
            }
        } catch (iae: IllegalArgumentException) {
            LOG.warn("Unknown PublicationType found: Handle ${this.handle}")
            throw iae
        }
    val publicationYear: String? =
        RestConverter
            .extractMetadata("dc.date.issued", metadata)
            ?.let {
                if (it.size > 1) {
                    LOG.warn("Item has multiple publication years: Handle ${this.handle}")
                }
                it[0]
            }
    val title =
        RestConverter.extractMetadata("dc.title", metadata)?.let {
            if (it.size > 1) {
                LOG.warn("Item has multiple titles: Handle ${this.handle}")
            }
            it[0]
        }

    return if (
        handle == null ||
        publicationType == null ||
        title == null
    ) {
        LOG.warn("Required field missing for metadata: Handle ${this.handle}")
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
                LOG.warn("Invalid numbers of parent communities (should be 1 or 2): Handle ${this.handle}")
                return null
            }
        }
        val licenceUrl =
            RestConverter.extractMetadata("dc.rights.license", metadata)?.let {
                if (it.size > 1) {
                    LOG.warn("Item has multiple licenceUrls: Handle ${this.handle}")
                }
                it[0]
            }

        ItemMetadata(
            // TODO(CB): Needs to be fixed asap -> multiple authors
            author = RestConverter.extractMetadata("dc.contributor.author", metadata)?.let { it[0] },
            // Not in DA yet
            band = null,
            collectionHandle =
                this.parentCollection?.handle?.let {
                    RestConverter.parseHandle(it)
                },
            collectionName = this.parentCollection?.name,
            communityHandle =
                parentDACommunity?.handle?.let {
                    RestConverter.parseHandle(it)
                },
            communityName = parentDACommunity?.name,
            createdBy = null,
            createdOn = null,
            deleted = this.withdrawn?.toBoolean() == true,
            doi =
                RestConverter.extractMetadata("dc.identifier.pi", metadata)?.let {
                    if (it.size > 1) {
                        LOG.warn("Item has multiple dois: Handle ${this.handle}")
                    }
                    it[0]
                },
            handle = RestConverter.parseHandle(handle),
            isbn =
                RestConverter.extractMetadata("dc.identifier.isbn", metadata)?.let {
                    if (it.size > 1) {
                        LOG.warn("Item has multiple isbns: Handle ${this.handle}")
                    }
                    it[0]
                },
            issn =
                RestConverter.extractMetadata("dc.identifier.issn", metadata)?.let {
                    if (it.size > 1) {
                        LOG.warn("Item has multiple issns: Handle ${this.handle}")
                    }
                    it[0]
                },
            isPartOfSeries =
                RestConverter.extractMetadata("dc.relation.ispartofseries", metadata),
            lastUpdatedBy = null,
            lastUpdatedOn = null,
            licenceUrl = licenceUrl,
            licenceUrlFilter = prepareLicenceUrlFilter(licenceUrl),
            paketSigel = RestConverter.extractMetadata("dc.identifier.packageid", metadata),
            ppn =
                RestConverter.extractMetadata("dc.identifier.ppn", metadata)?.let {
                    if (it.size > 1) {
                        LOG.warn("Item has multiple ppns: Handle ${this.handle}")
                    }
                    it[0]
                },
            publicationType = publicationType,
            publicationYear = publicationYear?.let { RestConverter.parseToDate(it) }?.year,
            subCommunityHandle =
                subDACommunity?.handle?.let {
                    RestConverter.parseHandle(it)
                },
            subCommunityName = subDACommunity?.name,
            storageDate =
                RestConverter
                    .extractMetadata("dc.date.accessioned", metadata)
                    ?.let {
                        if (it.size > 1) {
                            LOG.warn("Item has multiple storage dates: Handle ${this.handle}")
                        }
                        OffsetDateTime.parse(it[0])
                    },
            title = title,
            titleJournal =
                RestConverter.extractMetadata("dc.journalname", metadata)?.let {
                    if (it.size > 1) {
                        LOG.warn("Item has multiple journal tiles: Handle ${this.handle}")
                    }
                    it[0]
                },
            titleSeries =
                RestConverter.extractMetadata("dc.seriesname", metadata)?.let {
                    if (it.size > 1) {
                        LOG.warn("Item has multiple title series names: ${this.handle}")
                    }
                    it[0]
                },
            zdbIds =
                listOfNotNull(
                    RestConverter.extractMetadata("dc.relation.journalzdbid", metadata),
                    RestConverter.extractMetadata("dc.relation.serieszdbid", metadata),
                ).flatten(),
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
        publicationYearFilter = QueryParameterParser.parsePublicationYearFilter(this.filterPublicationYear),
        publicationTypeFilter = QueryParameterParser.parsePublicationTypeFilter(this.filterPublicationType),
        paketSigelFilter = QueryParameterParser.parsePaketSigelFilter(this.filterPaketSigel),
        zdbIdFilter = QueryParameterParser.parseZDBIdFilter(this.filterZDBId),
        accessStateFilter = QueryParameterParser.parseAccessStateFilter(this.filterAccessState),
        formalRuleFilter = QueryParameterParser.parseFormalRuleFilter(this.filterFormalRule),
        startDateFilter = QueryParameterParser.parseStartDateFilter(this.filterStartDate),
        endDateFilter = QueryParameterParser.parseEndDateFilter(this.filterEndDate),
        validOnFilter = QueryParameterParser.parseRightValidOnFilter(this.filterValidOn),
        noRightInformationFilter = QueryParameterParser.parseNoRightInformationFilter(this.filterNoRightInformation),
        seriesFilter = QueryParameterParser.parseSeriesFilter(this.filterSeries),
        manualRightFilter = QueryParameterParser.parseManualRightFilter(this.filterManualRight),
        licenceURLFilter = QueryParameterParser.parseLicenceUrlFilter(this.filterLicenceUrl),
        accessStateOnFilter = QueryParameterParser.parseAccessStateOnDate(this.filterAccessOnDate),
        rightIdFilter = QueryParameterParser.parseRightIdFilter(this.filterRightId),
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
        publicationYearFilter =
            PublicationYearFilter(
                fromYear = this.filterPublicationYear?.fromYear,
                toYear = this.filterPublicationYear?.toYear,
            ),
        publicationTypeFilter =
            QueryParameterParser.parsePublicationTypeFilter(
                this.filterPublicationType?.joinToString(
                    separator = ",",
                ),
            ),
        paketSigelFilter = QueryParameterParser.parsePaketSigelFilter(this.filterPaketSigel?.joinToString(separator = ",")),
        zdbIdFilter = QueryParameterParser.parseZDBIdFilter(this.filterZDBId?.joinToString(separator = ",")),
        accessStateFilter = QueryParameterParser.parseAccessStateFilter(this.filterAccessState?.joinToString(separator = ",")),
        formalRuleFilter = QueryParameterParser.parseFormalRuleFilter(this.filterFormalRule?.joinToString(separator = ",")),
        startDateFilter = this.filterStartDate?.let { StartDateFilter(it) },
        endDateFilter = this.filterEndDate?.let { EndDateFilter(it) },
        validOnFilter = this.filterValidOn?.let { RightValidOnFilter(it) },
        noRightInformationFilter = this.filterNoRightInformation?.takeIf { it }?.let { NoRightInformationFilter() },
        manualRightFilter = this.filterManualRight?.takeIf { it }?.let { ManualRightFilter() },
        accessStateOnFilter =
            this.filterAccessOnDate?.let {
                AccessStateOnDateFilter(
                    date = it.date,
                    accessState = AccessState.valueOf(it.accessState),
                )
            },
        licenceURLFilter = this.filterLicenceUrl?.let { LicenceUrlFilter(it) },
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        createdBy = createdBy,
        createdOn = createdOn,
        rightIdFilter =
            this.filterRightId
                ?.takeIf { it.isNotEmpty() }
                ?.map { it.rightId }
                ?.let { RightIdFilter(it) },
    )

fun Bookmark.toRest(
    filtersAsQuery: String,
    filterRightIds: List<RightIdTemplateName>,
): BookmarkRest =
    BookmarkRest(
        bookmarkName = this.bookmarkName,
        bookmarkId = this.bookmarkId,
        description = this.description,
        searchTerm = this.searchTerm,
        filterPublicationYear =
            FilterPublicationYearRest(
                fromYear = this.publicationYearFilter?.fromYear,
                toYear = this.publicationYearFilter?.toYear,
            ),
        filterPublicationType = this.publicationTypeFilter?.publicationTypes?.map { it.toString() },
        filterAccessState = this.accessStateFilter?.accessStates?.map { it.toString() },
        filterStartDate = this.startDateFilter?.date,
        filterEndDate = this.endDateFilter?.date,
        filterFormalRule = this.formalRuleFilter?.formalRules?.map { it.toString() },
        filterValidOn = this.validOnFilter?.date,
        filterPaketSigel = this.paketSigelFilter?.paketSigels,
        filterZDBId = this.zdbIdFilter?.zdbIds,
        filterNoRightInformation = this.noRightInformationFilter?.let { true } == true,
        filterManualRight = this.manualRightFilter?.let { true } == true,
        createdBy = this.createdBy,
        createdOn = this.createdOn,
        lastUpdatedBy = this.lastUpdatedBy,
        lastUpdatedOn = this.lastUpdatedOn,
        filterSeries = this.seriesFilter?.seriesNames,
        filterRightId = filterRightIds.map { it.toRest() },
        filtersAsQuery = filtersAsQuery,
        filterLicenceUrl = this.licenceURLFilter?.licenceUrl,
        filterAccessOnDate =
            this.accessStateOnFilter?.let {
                FilterAccessStateOnRest(
                    date = it.date,
                    accessState = it.accessState.toString(),
                )
            },
    )

fun RightIdTemplateName.toRest(): FilterRightIdRest =
    FilterRightIdRest(
        templateName = this.templateName,
        rightId = this.rightId,
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

fun ErrorQueryResult.toRest(pageSize: Int): RightErrorInformationRest {
    val totalPages = ceil(this.totalNumberOfResults.toDouble() / pageSize.toDouble()).toInt()
    return RightErrorInformationRest(
        numberOfResults = totalNumberOfResults,
        totalPages = totalPages,
        errors = this.results.map { it.toRest() },
        contextNames = this.contextNames.toList(),
        conflictTypes = this.conflictTypes.map { it.toRest() },
    )
}

fun SearchQueryResult.toRest(pageSize: Int): ItemInformation {
    val totalPages = ceil(this.numberOfResults.toDouble() / pageSize.toDouble()).toInt()
    return ItemInformation(
        itemArray = this.results.map { it.toRest() },
        totalPages = totalPages,
        accessStateWithCount =
            this.accessState.entries
                .sortedBy { it.key.priority }
                .map {
                    AccessStateWithCountRest(it.key.toRest(), it.value)
                }.toList(),
        hasLicenceContract = this.hasLicenceContract,
        hasOpenContentLicence = this.hasOpenContentLicence,
        hasZbwUserAgreement = this.hasZbwUserAgreement,
        numberOfResults = this.numberOfResults,
        paketSigelWithCount =
            this.paketSigels.entries
                .map { PaketSigelWithCountRest(count = it.value, paketSigel = it.key) }
                .toList()
                .sortedBy { it.paketSigel.lowercase() },
        publicationTypeWithCount =
            this.publicationType.entries
                .sortedBy { it.key.priority }
                .map {
                    PublicationTypeWithCountRest(
                        count = it.value,
                        publicationType = it.key.toRest(),
                    )
                }.toList(),
        zdbIdWithCount =
            this.zdbIds.entries
                .map {
                    ZdbIdWithCountRest(
                        count = it.value,
                        zdbId = it.key,
                    )
                }.toList()
                .sortedBy { it.count }
                .reversed(),
        templateNameWithCount =
            this.templateNamesToOcc.entries
                .map {
                    TemplateNameWithCountRest(
                        templateName = it.value.first,
                        count = it.value.second,
                        rightId = it.key,
                    )
                }.sortedBy { it.templateName.lowercase() },
        isPartOfSeriesCount =
            this.isPartOfSeries.entries
                .map {
                    IsPartOfSeriesCountRest(
                        count = it.value,
                        series = it.key,
                    )
                }.toList()
                .sortedBy { it.count }
                .reversed(),
        licenceUrlCount =
            this.licenceUrl.entries
                .map {
                    LicenceUrlCountRest(
                        count = it.value,
                        licenceUrl = it.key,
                    )
                }.toList()
                .sortedBy { it.licenceUrl.lowercase() },
        filtersAsQuery = filtersAsQuery,
    )
}

fun ConflictType.toRest(): ConflictTypeRest =
    when (this) {
        ConflictType.DATE_OVERLAP -> ConflictTypeRest.date_overlap
        ConflictType.UNSPECIFIED -> ConflictTypeRest.unspecified
        ConflictType.GAP -> ConflictTypeRest.gap
        ConflictType.DELETION -> ConflictTypeRest.deletion
        ConflictType.NO_RIGHT -> ConflictTypeRest.no_right
    }

fun ConflictType.toProto(): de.zbw.lori.api.ConflictType =
    when (this) {
        ConflictType.DATE_OVERLAP -> de.zbw.lori.api.ConflictType.CONFLICT_TYPE_DATE_OVERLAP
        ConflictType.UNSPECIFIED -> de.zbw.lori.api.ConflictType.CONFLICT_TYPE_UNSPECIFIED
        ConflictType.DELETION -> de.zbw.lori.api.ConflictType.CONFLICT_TYPE_DELETION
        ConflictType.GAP -> de.zbw.lori.api.ConflictType.CONFLICT_TYPE_GAP
        ConflictType.NO_RIGHT -> de.zbw.lori.api.ConflictType.CONFLICT_TYPE_NO_RIGHT
    }

fun RightError.toRest(): RightErrorRest =
    RightErrorRest(
        conflictByRightId = conflictByRightId,
        conflictByContext = conflictByContext,
        createdOn = createdOn,
        message = message,
        handle = handle,
        conflictingWithRightId = conflictingWithRightId,
        conflictType = conflictType.toRest(),
        errorId = errorId ?: -1,
    )

fun TemplateApplicationResult.toRest(): TemplateApplicationRest =
    TemplateApplicationRest(
        rightId = rightId,
        templateName = templateName,
        handles = appliedMetadataHandles,
        errors = errors.map { it.toRest() },
        numberOfErrors = numberOfErrors,
        numberOfAppliedEntries = appliedMetadataHandles.size,
        testId = testId,
        exceptionTemplateApplications =
            exceptionTemplateApplicationResult.map { exc ->
                TemplateApplicationRest(
                    rightId = exc.rightId,
                    handles = exc.appliedMetadataHandles,
                    templateName = exc.templateName,
                    errors = exc.errors.map { it.toRest() },
                    numberOfAppliedEntries = exc.appliedMetadataHandles.size,
                    testId = exc.testId,
                    numberOfErrors = exc.numberOfErrors,
                    exceptionTemplateApplications = emptyList(),
                )
            },
    )

/**
 * Utility functions helping to convert
 * REST to Business Objects and vice versa.
 */
object RestConverter {
    fun extractMetadata(
        key: String,
        metadata: List<DAMetadata>,
    ): List<String>? =
        metadata
            .filter { dam -> dam.key == key }
            .takeIf { it.isNotEmpty() }
            ?.map { it.value }

    fun parseToDate(s: String): LocalDate =
        if (s.matches("\\d{4}-\\d{2}-\\d{2}".toRegex())) {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
        } else if (s.matches("\\d{4}-\\d{2}".toRegex())) {
            LocalDate.parse("$s-01", DateTimeFormatter.ISO_LOCAL_DATE)
        } else if (s.matches("\\d{4}/\\d{2}".toRegex())) {
            LocalDate.parse(
                "${s.substringBefore('/')}-${s.substringAfter('/')}-01",
                DateTimeFormatter.ISO_LOCAL_DATE,
            )
        } else if (s.matches("\\d{4}".toRegex())) {
            LocalDate.parse("$s-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
        } else {
            LOG.warn("Date format can't be recognized: $s")
            LocalDate.parse("1970-01-01", DateTimeFormatter.ISO_LOCAL_DATE)
        }

    /**
     * Parse CSV formatted string.
     * The expected format is as following:
     * organisation_name,ipAddress
     */
    fun parseToGroup(
        hasCSVHeader: Boolean,
        ipAddressesCSV: String,
    ): List<GroupEntry> =
        try {
            val csvFormat: CSVFormat =
                CSVFormat.Builder
                    .create()
                    .setDelimiter(';')
                    .setQuote(Character.valueOf('"'))
                    .setRecordSeparator("\r\n")
                    .build()
            CSVFormat.Builder
                .create(csvFormat)
                .apply {
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
            throw IllegalArgumentException(e)
        }

    fun parseHandle(given: String): String = given.replace("^[\\D]*".toRegex(), "")

    val LOG = LogManager.getLogger(RestConverter::class.java)
    const val CSV_DELIMITER = ";"
}
