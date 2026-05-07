package de.zbw.api.lori.server.type

import de.zbw.api.lori.server.exception.InvalidIPAddressException
import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.api.lori.server.type.rest.DAItemConverter
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
import de.zbw.business.lori.server.StorageDateFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.business.lori.server.type.ExportFormat
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.ExportJobStatus
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
import de.zbw.business.lori.server.type.SortByField
import de.zbw.business.lori.server.type.SortOrder
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.business.lori.server.type.UserPermission
import de.zbw.lori.model.AccessStateRest
import de.zbw.lori.model.AccessStateWithCountRest
import de.zbw.lori.model.BookmarkRawRest
import de.zbw.lori.model.BookmarkRest
import de.zbw.lori.model.BookmarkTemplateRest
import de.zbw.lori.model.ConflictTypeRest
import de.zbw.lori.model.ExportFormatRest
import de.zbw.lori.model.FilterAccessStateOnRest
import de.zbw.lori.model.FilterPublicationYearRest
import de.zbw.lori.model.FilterRightIdRest
import de.zbw.lori.model.FilterStorageDateRest
import de.zbw.lori.model.GroupRest
import de.zbw.lori.model.IsPartOfSeriesCountRest
import de.zbw.lori.model.ItemInformation
import de.zbw.lori.model.ItemRest
import de.zbw.lori.model.JobStatusRest
import de.zbw.lori.model.JobStatusUpdateRest
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
import de.zbw.lori.model.SortByRest
import de.zbw.lori.model.SortOrderRest
import de.zbw.lori.model.TemplateApplicationRest
import de.zbw.lori.model.TemplateNameWithCountRest
import de.zbw.lori.model.UserPermissionRest
import de.zbw.lori.model.UserSessionRest
import de.zbw.lori.model.ZdbIdWithCountRest
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.LocalDate
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
        groupId = this.groupId,
        description = this.description,
        allowedAddressesRaw =
            this.entries.joinToString(separator = "\n") {
                "${it.ipAddresses}${RestConverter.CSV_DELIMITER}${it.organisationName}"
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
        title = title,
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
        collectionHandle = collectionHandle,
        collectionName = collectionName,
        communityHandle = communityHandle,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        deleted = deleted,
        pids = doi,
        econbizId = econbizid,
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
        zdbIds = zdbIds,
        econstorIssue = econstorIssue,
        econstorVolume = econstorVolume,
        isPartOfBook = isPartOfBook,
        isPartOfJournal = isPartOfJournal,
        ppnBook = ppnBook,
        ppnJournal = ppnJournal,
        ppnSeries = ppnSeries,
    )

fun ItemMetadata.toRest(): MetadataRest =
    MetadataRest(
        collectionHandle = collectionHandle,
        collectionName = collectionName,
        communityHandle = communityHandle,
        communityName = communityName,
        createdBy = createdBy,
        createdOn = createdOn,
        deleted = deleted,
        doi = pids,
        econbizid = econbizId,
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
        zdbIds = zdbIds,
        econstorIssue = econstorIssue,
        econstorVolume = econstorVolume,
        ppnSeries = ppnSeries,
        isPartOfBook = isPartOfBook,
        isPartOfJournal = isPartOfJournal,
        ppnBook = ppnBook,
        ppnJournal = ppnJournal,
    )

fun RightRest.toBusiness(): ItemRight =
    ItemRight(
        rightId = rightId,
        accessState = accessState?.toBusiness(),
        basisAccessState = basisAccessState?.toBusiness(),
        basisStorage = basisStorage?.toBusiness(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        exceptionOfId = exceptionOfId,
        firstAppliedOn = firstAppliedOn,
        hasExceptionId = hasExceptionId,
        groups = groups?.map { it.toBusiness() },
        groupIds = groupIds,
        hasLegalRisk = hasLegalRisk,
        isTemplate = isTemplate,
        lastAppliedOn = lastAppliedOn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceContract = licenceContract,
        notesGeneral = notesGeneral,
        notesFormalRules = notesFormalRules,
        notesProcessDocumentation = notesProcessDocumentation,
        notesManagementRelated = notesManagementRelated,
        predecessorId =
            predecessorId?.let {
                it.ifBlank {
                    null
                }
            },
        restrictedOpenContentLicence = restrictedOpenContentLicence,
        startDate = startDate,
        successorId =
            successorId?.let {
                it.ifBlank {
                    null
                }
            },
        templateDescription = templateDescription,
        templateName = templateName?.trim(),
        zbwUserAgreement = zbwUserAgreement,
    )

fun ItemRight.toRest(): RightRest =
    RightRest(
        rightId = rightId,
        accessState = accessState?.toRest(),
        basisAccessState = basisAccessState?.toRest(),
        basisStorage = basisStorage?.toRest(),
        createdBy = createdBy,
        createdOn = createdOn,
        endDate = endDate,
        exceptionOfId = exceptionOfId,
        firstAppliedOn = firstAppliedOn,
        groupIds = groups?.map { it.groupId },
        groups = groups?.map { it.toRest() },
        hasExceptionId = hasExceptionId,
        hasLegalRisk = hasLegalRisk,
        isTemplate = isTemplate,
        lastAppliedOn = lastAppliedOn,
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        licenceContract = licenceContract,
        notesGeneral = notesGeneral,
        notesFormalRules = notesFormalRules,
        notesProcessDocumentation = notesProcessDocumentation,
        notesManagementRelated = notesManagementRelated,
        predecessorId = predecessorId,
        restrictedOpenContentLicence = restrictedOpenContentLicence,
        startDate = startDate,
        successorId = successorId,
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
        RightRest.BasisAccessState.opencontentlicence -> BasisAccessState.OPEN_CONTENT_LICENCE
        RightRest.BasisAccessState.useragreement -> BasisAccessState.USER_AGREEMENT
        RightRest.BasisAccessState.zbwpolicy -> BasisAccessState.ZBW_POLICY
    }

internal fun BasisAccessState.toRest(): RightRest.BasisAccessState =
    when (this) {
        BasisAccessState.AUTHOR_RIGHT_EXCEPTION -> RightRest.BasisAccessState.authorrightexception
        BasisAccessState.LICENCE_CONTRACT -> RightRest.BasisAccessState.licencecontract
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

suspend fun DAItem.toBusiness(
    daCommunity: DACommunity,
    daCollection: DACollection,
    validationErrorMap: MutableMap<MetadataValidationError, List<String>>,
    mutexForLogging: Mutex,
): ItemMetadata? = DAItemConverter.toBusiness(this, daCommunity, daCollection, validationErrorMap, mutexForLogging)

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
        storageDateFilter = QueryParameterParser.parseStorageDateFilter(this.filterStorageDate),
        paketSigelFilters = QueryParameterParser.parsePaketSigelFilter(this.filterPaketSigel),
        zdbIdFilters = QueryParameterParser.parseZDBIdFilters(this.filterZDBId),
        accessStateFilter = QueryParameterParser.parseAccessStateFilter(this.filterAccessState),
        formalRuleFilter = QueryParameterParser.parseFormalRuleFilter(this.filterFormalRule),
        startDateFilter = QueryParameterParser.parseStartDateFilter(this.filterStartDate),
        endDateFilter = QueryParameterParser.parseEndDateFilter(this.filterEndDate),
        validOnFilter = QueryParameterParser.parseRightValidOnFilter(this.filterValidOn),
        noRightInformationFilter = QueryParameterParser.parseNoRightInformationFilter(this.filterNoRightInformation),
        seriesFilter = QueryParameterParser.parseSeriesFilter(this.filterSeries),
        manualRightFilter = QueryParameterParser.parseManualRightFilter(this.filterManualRight),
        deletionsFilter = QueryParameterParser.parseDeletionsFilter(this.filterDeletions),
        licenceURLFilter = QueryParameterParser.parseLicenceUrlFilter(this.filterLicenceUrl),
        accessStateOnFilter = QueryParameterParser.parseAccessStateOnDate(this.filterAccessOnDate),
        rightIdFilter = QueryParameterParser.parseRightIdFilter(this.filterRightId),
        lastUpdatedBy = lastUpdatedBy,
        lastUpdatedOn = lastUpdatedOn,
        createdBy = createdBy,
        createdOn = createdOn,
        // If this ever comes up again: `templateNameFilter` is absent for a reason. It got replaced with `rightIdFilter`.
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
                this.filterPublicationType
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(
                        separator = ",",
                    ),
            ),
        paketSigelFilters =
            QueryParameterParser.parsePaketSigelFilter(
                this.filterPaketSigel
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ","),
            ),
        zdbIdFilters =
            QueryParameterParser.parseZDBIdFilters(
                this.filterZDBId
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ","),
            ),
        accessStateFilter =
            QueryParameterParser.parseAccessStateFilter(
                this.filterAccessState
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ","),
            ),
        formalRuleFilter =
            QueryParameterParser.parseFormalRuleFilter(
                this.filterFormalRule
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ","),
            ),
        startDateFilter = this.filterStartDate?.let { StartDateFilter(it) },
        endDateFilter = this.filterEndDate?.let { EndDateFilter(it) },
        validOnFilter = this.filterValidOn?.let { RightValidOnFilter(it) },
        noRightInformationFilter = this.filterNoRightInformation?.takeIf { it }?.let { NoRightInformationFilter() },
        manualRightFilter = this.filterManualRight?.takeIf { it }?.let { ManualRightFilter() },
        deletionsFilter = QueryParameterParser.parseDeletionsFilter(this.filterDeletions?.toString()),
        accessStateOnFilter =
            this.filterAccessOnDate?.let {
                AccessStateOnDateFilter(
                    date = it.date,
                    accessState = AccessState.valueOf(it.accessState),
                )
            },
        storageDateFilter =
            this.filterStorageDate?.let {
                StorageDateFilter(
                    from = it.fromDate,
                    to = it.toDate,
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
        filterPaketSigel = this.paketSigelFilters?.map { it.paketSigel },
        filterZDBId = this.zdbIdFilters?.map { it.zdbId },
        filterNoRightInformation = this.noRightInformationFilter?.let { true } == true,
        filterManualRight = this.manualRightFilter?.let { true } == true,
        filterDeletions = this.deletionsFilter?.on,
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
        filterStorageDate =
            this.storageDateFilter?.let {
                FilterStorageDateRest(
                    fromDate = it.from,
                    toDate = it.to,
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
        contextNames = this.contextNames,
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
        licenceContracts = this.licenceContracts,
        ccLicenceNoRestrictions = this.ccLicenceNoRestrictions,
        noLegalRisks = this.noLegalRisks,
        zbwUserAgreements = this.zbwUserAgreements,
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
        ConflictType.DATE_OVERLAP_NO_END_MANUAL -> ConflictTypeRest.date_overlap_no_end
    }

fun RightError.toRest(): RightErrorRest =
    RightErrorRest(
        conflictByRightId = conflictCausedByRightId,
        conflictByContext = conflictCausedInContext,
        createdOn = createdOn,
        message = message,
        handle = handle,
        conflictingWithRightId = conflictWithExistingRightId,
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
        exceptionTemplateApplication =
            exceptionTemplateApplicationResult?.let { exc ->
                TemplateApplicationRest(
                    rightId = exc.rightId,
                    handles = exc.appliedMetadataHandles,
                    templateName = exc.templateName,
                    errors = exc.errors.map { it.toRest() },
                    numberOfAppliedEntries = exc.appliedMetadataHandles.size,
                    testId = exc.testId,
                    numberOfErrors = exc.numberOfErrors,
                    exceptionTemplateApplication = null,
                )
            },
    )

fun SortOrderRest.toBusiness(): SortOrder =
    when (this) {
        SortOrderRest.asc -> SortOrder.ASC
        SortOrderRest.desc -> SortOrder.DESC
    }

fun SortByRest.toBusiness(): SortByField =
    when (this) {
        SortByRest.collection_name -> SortByField.COLLECTION_NAME
        SortByRest.community_name -> SortByField.COMMUNITY_NAME
        SortByRest.handle -> SortByField.HANDLE
        SortByRest.publication_type -> SortByField.PUBLICATION_TYPE
        SortByRest.publication_year -> SortByField.PUBLICATION_YEAR
        SortByRest.title -> SortByField.TITLE
        SortByRest.doi -> SortByField.DOI
        SortByRest.isbn -> SortByField.ISBN
        SortByRest.issn -> SortByField.ISSN
        SortByRest.paketSigel -> SortByField.PAKET_SIGEL
        SortByRest.ppn -> SortByField.PPN
        SortByRest.series -> SortByField.IS_PART_OF_SERIES
    }

fun ExportJob.toUpdateRest(
    downloadUrl: String? = null,
    rows: Int = 0,
): JobStatusUpdateRest =
    JobStatusUpdateRest(
        status = this.status.toRest(),
        jobId = this.id.toString(),
        downloadUrl = downloadUrl,
        rows = rows,
    )

fun ExportJobStatus.toRest(): JobStatusRest =
    when (this) {
        ExportJobStatus.QUEUED -> JobStatusRest.queued
        ExportJobStatus.RUNNING -> JobStatusRest.running
        ExportJobStatus.SUCCESSFUL -> JobStatusRest.finished
        ExportJobStatus.FAILED -> JobStatusRest.failed
    }

fun ExportFormatRest.toBusiness(): ExportFormat =
    when (this) {
        ExportFormatRest.csv -> ExportFormat.CSV
        ExportFormatRest.json -> ExportFormat.JSON
    }

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

    fun parseToDate(s: String): LocalDate? =
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
            null
        }

    /**
     * Parse CSV formatted string.
     * The expected format is as follows:
     * ipAddress1,ipAddress2,ipAdress3;organisationName;
     */
    fun parseToGroup(
        hasCSVHeader: Boolean,
        ipAddressesCSV: String,
    ): List<GroupEntry> =
        try {
            val csvFormat: CSVFormat =
                CSVFormat.Builder
                    .create()
                    .setDelimiter(CSV_DELIMITER)
                    .setQuote(Character.valueOf('"'))
                    .setRecordSeparator("\r\n")
                    .get()
            val groupEntries =
                CSVFormat.Builder
                    .create(csvFormat)
                    .apply {
                        setIgnoreSurroundingSpaces(true)
                    }.get()
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
                            organisationName = it[1],
                            ipAddresses = it[0],
                        ).also { ge ->
                            if (ge.ipAddresses == "" || ge.organisationName == "") {
                                throw IllegalArgumentException("Both, organisation name and ip addresses must be not null")
                            }
                        }
                    }
            val invalidIPAddresses = mutableListOf<String>()
            val allValidIps =
                groupEntries
                    .flatMap { entry ->
                        entry.ipAddresses.split(",").map { ipAddress ->
                            ipAddress.matches(IP_PATTERN_REGEX).also {
                                if (!it) {
                                    invalidIPAddresses.add(ipAddress)
                                }
                            }
                        }
                    }.all { it }
            if (!allValidIps) {
                throw InvalidIPAddressException("Folgende ungültige IP-Adressen wurden gefunden: $invalidIPAddresses")
            } else {
                groupEntries
            }
        } catch (e: InvalidIPAddressException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }

    fun parseHandle(given: String): String = given.replace("^[\\D]*".toRegex(), "")

    val LOG: Logger = LogManager.getLogger(RestConverter::class.java)
    const val CSV_DELIMITER = ';'
    val IP_PATTERN_REGEX =
        Regex(
            """
            ^(\d{1,3}|\*)(-(\d{1,3}))?\.(\d{1,3}|\*)(-(\d{1,3}))?\.(\d{1,3}|\*)(-(\d{1,3}))?\.((\d{1,3}|\*)(-(\d{1,3}))?)(/(\d{1,2}))?$
            """.trimIndent(),
        )
}
