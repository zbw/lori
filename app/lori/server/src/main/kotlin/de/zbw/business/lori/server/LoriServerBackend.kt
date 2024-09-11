package de.zbw.business.lori.server

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.exception.ResourceStillInUseException
import de.zbw.api.lori.server.route.ApiError
import de.zbw.api.lori.server.type.Either
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchGrammar
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.lori.model.ErrorRest
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.FacetTransientSet
import io.ktor.http.HttpStatusCode
import io.opentelemetry.api.trace.Tracer
import org.apache.logging.log4j.util.Strings
import java.security.MessageDigest
import java.time.OffsetDateTime

/**
 * Backend for the Lori-Server.
 *
 * Created on 07-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackend(
    internal val dbConnector: DatabaseConnector,
    internal val config: LoriConfiguration,
) {
    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DatabaseConnector(
            config,
            tracer,
        ),
        config,
    )

    internal fun insertRightForMetadataIds(
        right: ItemRight,
        metadataIds: List<String>,
    ): String {
        val pkRight = dbConnector.rightDB.insertRight(right.copy(isTemplate = false, templateName = null))
        metadataIds.forEach {
            dbConnector.itemDB.insertItem(it, pkRight)
        }
        return pkRight
    }

    fun insertItemEntry(
        metadataId: String,
        rightId: String,
        deleteOnConflict: Boolean = false,
    ): Either<Pair<HttpStatusCode, ErrorRest>, String> =
        if (checkRightConflicts(metadataId, rightId)) {
            if (deleteOnConflict) {
                dbConnector.rightDB.deleteRightsByIds(listOf(rightId))
            }
            Either.Left(
                Pair(
                    HttpStatusCode.Conflict,
                    ApiError.conflictError("Es gibt einen Start- und/oder Enddatum Konflikt mit bereits bestehenden Rechten"),
                ),
            )
        } else {
            dbConnector.itemDB
                .insertItem(metadataId, rightId)
                ?.let { Either.Right(it) }
                ?: Either.Left(Pair(HttpStatusCode.InternalServerError, ApiError.internalServerError()))
        }

    fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> = metadataElems.map { insertMetadataElement(it) }

    fun insertGroup(group: Group): Int = dbConnector.groupDB.insertGroup(group)

    fun insertMetadataElement(metadata: ItemMetadata): String = dbConnector.metadataDB.insertMetadata(metadata)

    fun insertRight(right: ItemRight): String {
        val generatedRightId = dbConnector.rightDB.insertRight(right)
        right.groups?.forEach { group ->
            dbConnector.groupDB.insertGroupRightPair(
                rightId = generatedRightId,
                groupId = group.groupId,
            )
        }
        return generatedRightId
    }

    fun updateGroup(group: Group): Int = dbConnector.groupDB.updateGroup(group)

    fun upsertRight(right: ItemRight): Int {
        val rightId = right.rightId!!
        val oldGroupIds =
            dbConnector.groupDB
                .getGroupsByRightId(rightId)
                .map { it.groupId }
                .toSet()
        val newGroupIds = right.groupIds?.toSet() ?: emptySet()

        val toAdd = newGroupIds.subtract(oldGroupIds)
        toAdd.forEach { gId ->
            dbConnector.groupDB.insertGroupRightPair(
                rightId = rightId,
                groupId = gId,
            )
        }

        val toRemove = oldGroupIds.subtract(newGroupIds)
        toRemove.forEach { gId ->
            dbConnector.groupDB.deleteGroupPair(
                rightId = rightId,
                groupId = gId,
            )
        }

        return dbConnector.rightDB.upsertRight(right)
    }

    fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.metadataDB.upsertMetadataBatch(metadataElems.map { it })

    fun upsertMetadata(metadata: List<ItemMetadata>): IntArray = dbConnector.metadataDB.upsertMetadataBatch(metadata)

    fun getMetadataList(
        limit: Int,
        offset: Int,
    ): List<ItemMetadata> =
        dbConnector.metadataDB
            .getMetadataRange(limit, offset)
            .takeIf {
                it.isNotEmpty()
            }?.let { metadataList ->
                metadataList.sortedBy { it.metadataId }
            } ?: emptyList()

    fun getMetadataElementsByIds(metadataIds: List<String>): List<ItemMetadata> = dbConnector.metadataDB.getMetadata(metadataIds)

    fun metadataContainsId(id: String): Boolean = dbConnector.metadataDB.metadataContainsId(id)

    fun rightContainsId(rightId: String): Boolean = dbConnector.rightDB.rightContainsId(rightId)

    fun getItemByMetadataId(metadataId: String): Item? =
        dbConnector.metadataDB
            .getMetadata(listOf(metadataId))
            .takeIf { it.isNotEmpty() }
            ?.first()
            ?.let { meta ->
                val rights =
                    dbConnector.rightDB.getRightIdsByMetadata(metadataId).let {
                        dbConnector.rightDB.getRightsByIds(it)
                    }
                Item(
                    meta,
                    rights,
                )
            }

    fun getGroupById(groupId: Int): Group? = dbConnector.groupDB.getGroupById(groupId)

    fun getGroupList(
        limit: Int,
        offset: Int,
    ): List<Group> = dbConnector.groupDB.getGroupList(limit, offset)

    fun getGroupListIdsOnly(
        limit: Int,
        offset: Int,
    ): List<Group> =
        dbConnector.groupDB
            .getGroupListIdsOnly(limit, offset)
            .map {
                Group(
                    groupId = it,
                    entries = emptyList(),
                    description = null,
                    title = "",
                )
            }

    fun getRightById(rightId: String): ItemRight? = getRightsByIds(listOf(rightId)).firstOrNull()

    fun getRightsByIds(rightIds: List<String>): List<ItemRight> = dbConnector.rightDB.getRightsByIds(rightIds)

    fun getItemList(
        limit: Int,
        offset: Int,
    ): List<Item> {
        val receivedMetadata = dbConnector.metadataDB.getMetadataRange(limit, offset)
        return receivedMetadata
            .takeIf {
                it.isNotEmpty()
            }?.let { metadataList ->
                getRightsForMetadata(metadataList)
            } ?: emptyList()
    }

    private fun getRightsForMetadata(metadataList: List<ItemMetadata>): List<Item> {
        val metadataToRights =
            metadataList.map { metadata ->
                metadata to dbConnector.rightDB.getRightIdsByMetadata(metadata.metadataId)
            }
        return metadataToRights.map { p ->
            Item(
                p.first,
                dbConnector.rightDB.getRightsByIds(p.second),
            )
        }
    }

    fun itemContainsMetadata(metadataId: String): Boolean = dbConnector.metadataDB.itemContainsMetadata(metadataId)

    fun itemContainsEntry(
        metadataId: String,
        rightId: String,
    ): Boolean = dbConnector.itemDB.itemContainsEntry(metadataId, rightId)

    fun countMetadataEntries(): Int =
        dbConnector.searchDB.countSearchMetadata(
            null,
            emptyList(),
            emptyList(),
            null,
        )

    fun countItemByRightId(rightId: String) = dbConnector.itemDB.countItemByRightId(rightId)

    fun deleteItemEntry(
        metadataId: String,
        rightId: String,
    ) = dbConnector.itemDB.deleteItem(metadataId, rightId)

    fun deleteItemEntriesByMetadataId(metadataId: String) = dbConnector.itemDB.deleteItemByMetadataId(metadataId)

    fun deleteItemEntriesByRightId(rightId: String) = dbConnector.itemDB.deleteItemByRightId(rightId)

    fun deleteGroup(groupId: Int): Int {
        val receivedRights: List<String> = dbConnector.groupDB.getRightsByGroupId(groupId)
        return if (receivedRights.isEmpty()) {
            dbConnector.groupDB.deleteGroupById(groupId)
        } else {
            val group = dbConnector.groupDB.getGroupById(groupId)
            if (group == null) {
                return 0
            } else {
                val rightsBlocking: List<ItemRight> = dbConnector.rightDB.getRightsByIds(receivedRights)
                throw ResourceStillInUseException(
                    "Gruppe '${group.title} ($groupId)' wird noch von folgenden Rechten verwendet: " +
                        rightsBlocking.joinToString(separator = ",") { right: ItemRight ->
                            "ID: " + right.rightId + "; "
                        },
                )
            }
        }
    }

    fun deleteMetadata(metadataId: String): Int = dbConnector.metadataDB.deleteMetadata(listOf(metadataId))

    fun deleteRight(rightId: String): Int {
        dbConnector.groupDB.deleteGroupPairsByRightId(rightId)
        return dbConnector.rightDB.deleteRightsByIds(listOf(rightId))
    }

    fun getRightEntriesByMetadataId(metadataId: String): List<ItemRight> =
        dbConnector.rightDB.getRightIdsByMetadata(metadataId).let {
            dbConnector.rightDB.getRightsByIds(it)
        }

    fun deleteSessionById(sessionID: String) = dbConnector.userDB.deleteSessionById(sessionID)

    fun getSessionById(sessionID: String): Session? = dbConnector.userDB.getSessionById(sessionID)

    fun insertSession(session: Session): String = dbConnector.userDB.insertSession(session)

    private fun checkRightConflicts(
        metadataId: String,
        newRightId: String,
    ): Boolean {
        // Get all right ids
        val rightIds = dbConnector.itemDB.getRightIdsByMetadataId(metadataId)
        // Get data for all rights
        val rights: List<ItemRight> = dbConnector.rightDB.getRightsByIds(rightIds)
        val newRight: ItemRight = dbConnector.rightDB.getRightsByIds(listOf(newRightId)).firstOrNull() ?: return false
        // Check for conflicts
        return rights.foldRight(false) { r, acc ->
            acc || checkForDateConflict(newRight, r)
        }
    }

    fun searchQuery(
        searchTerm: String?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter> = emptyList(),
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
        noRightInformationFilter: NoRightInformationFilter? = null,
        metadataIdsToIgnore: List<String> = emptyList(),
    ): SearchQueryResult {
        val searchExpression: SearchExpression? =
            searchTerm
                ?.takeIf { it.isNotBlank() }
                ?.let { SearchGrammar.tryParseToEnd(it) }
                ?.let {
                    when (it) {
                        is Parsed -> it.value
                        is ErrorResult -> throw ParsingException("Parsing error in query: $it")
                    }
                }
        // Acquire search results
        val receivedMetadata: List<ItemMetadata> =
            dbConnector.searchDB.searchMetadataItems(
                searchExpression,
                limit,
                offset,
                metadataSearchFilter,
                rightSearchFilter.takeIf { noRightInformationFilter == null } ?: emptyList(),
                noRightInformationFilter,
                metadataIdsToIgnore,
            )

        // Combine Metadata entries with their rights
        val items: List<Item> =
            receivedMetadata
                .takeIf {
                    it.isNotEmpty()
                }?.let { metadata ->
                    getRightsForMetadata(metadata)
                } ?: (emptyList())

        // Acquire number of results
        val numberOfResults =
            items
                .takeIf { it.isNotEmpty() || offset != 0 }
                ?.let {
                    dbConnector.searchDB.countSearchMetadata(
                        searchExpression,
                        metadataSearchFilter,
                        rightSearchFilter.takeIf { noRightInformationFilter == null } ?: emptyList(),
                        noRightInformationFilter,
                    )
                }
                ?: 0

        // Collect all publication types, zdbIds and paketSigels
        val facets: FacetTransientSet =
            dbConnector.searchDB.searchForFacets(
                searchExpression,
                metadataSearchFilter,
                rightSearchFilter,
                noRightInformationFilter,
            )
        return SearchQueryResult(
            numberOfResults = numberOfResults,
            results = items,
            accessState = facets.accessState,
            hasLicenceContract = facets.hasLicenceContract,
            hasOpenContentLicence = facets.hasOpenContentLicence,
            hasZbwUserAgreement = facets.hasZbwUserAgreement,
            paketSigels = facets.paketSigels,
            publicationType = facets.publicationType,
            templateNamesToOcc = getRightIdsByTemplateNames(facets.templateIdToOccurence),
            zdbIds = facets.zdbIdsJournal + facets.zdbIdsSeries,
            searchBarEquivalent =
                SearchFilter.filtersToString(
                    filters = (metadataSearchFilter + rightSearchFilter + listOf(noRightInformationFilter)),
                    searchTerm = searchTerm,
                ),
            isPartOfSeries = facets.isPartOfSeries,
        )
    }

    private fun getRightIdsByTemplateNames(idToCount: Map<String, Int>): Map<String, Pair<String, Int>> {
        return dbConnector.rightDB
            .getRightsByTemplateNames(idToCount.keys.toList())
            .mapNotNull { right ->
                if (idToCount[right.templateName] == null || right.rightId == null || right.templateName == null) {
                    return@mapNotNull null
                }
                right.rightId to (right.templateName to idToCount[right.templateName]!!)
            }.toMap()
    }

    fun isException(rightId: String): Boolean = dbConnector.rightDB.isException(rightId)

    fun addExceptionToTemplate(
        rightIdTemplate: String,
        rightIdExceptions: List<String>,
    ): Int =
        dbConnector.rightDB.addExceptionToTemplate(
            rightIdTemplate = rightIdTemplate,
            rightIdExceptions = rightIdExceptions,
        )

    fun insertBookmark(bookmark: Bookmark): Int = dbConnector.bookmarkDB.insertBookmark(bookmark)

    fun deleteBookmark(bookmarkId: Int): Int {
        val receivedTemplateIds: List<String> = dbConnector.bookmarkTemplateDB.getRightIdsByBookmarkId(bookmarkId)
        return if (receivedTemplateIds.isEmpty()) {
            dbConnector.bookmarkDB.deleteBookmarkById(bookmarkId)
        } else {
            val bookmark = dbConnector.bookmarkDB.getBookmarksByIds(listOf(bookmarkId)).firstOrNull()
            if (bookmark == null) {
                return 0
            } else {
                val templatesBlocking: List<ItemRight> = dbConnector.rightDB.getRightsByIds(receivedTemplateIds)
                throw ResourceStillInUseException(
                    "Bookmark '${bookmark.bookmarkName} ($bookmarkId)' wird noch von folgenden Templates verwendet: " +
                        templatesBlocking.joinToString(separator = ",") { right: ItemRight ->
                            "'" + right.templateName + " (" + right.rightId + ")'"
                        },
                )
            }
        }
    }

    fun updateBookmark(
        bookmarkId: Int,
        bookmark: Bookmark,
    ): Int = dbConnector.bookmarkDB.updateBookmarkById(bookmarkId, bookmark)

    fun getBookmarkById(bookmarkId: Int): Bookmark? = dbConnector.bookmarkDB.getBookmarksByIds(listOf(bookmarkId)).firstOrNull()

    fun getBookmarkList(
        limit: Int,
        offset: Int,
    ): List<Bookmark> = dbConnector.bookmarkDB.getBookmarkList(limit, offset)

    /**
     * Insert template.
     */
    fun insertTemplate(right: ItemRight): String = dbConnector.rightDB.insertRight(right)

    fun getTemplateList(
        limit: Int,
        offset: Int,
    ): List<ItemRight> = dbConnector.rightDB.getTemplateList(limit, offset)

    /**
     * Template-Bookmark Pair.
     */
    fun getBookmarksByRightId(rightId: String): List<Bookmark> {
        val bookmarkIds = dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightId(rightId)
        return dbConnector.bookmarkDB.getBookmarksByIds(bookmarkIds)
    }

    fun deleteBookmarkTemplatePair(
        rightId: String,
        bookmarkId: Int,
    ): Int =
        dbConnector.bookmarkTemplateDB.deleteTemplateBookmarkPair(
            BookmarkTemplate(
                bookmarkId = bookmarkId,
                rightId = rightId,
            ),
        )

    fun insertBookmarkTemplatePair(
        bookmarkId: Int,
        rightId: String,
    ): Int =
        dbConnector.bookmarkTemplateDB.insertTemplateBookmarkPair(
            BookmarkTemplate(
                bookmarkId = bookmarkId,
                rightId = rightId,
            ),
        )

    fun upsertBookmarkTemplatePairs(bookmarkTemplates: List<BookmarkTemplate>): List<BookmarkTemplate> =
        dbConnector.bookmarkTemplateDB.upsertTemplateBookmarkBatch(bookmarkTemplates)

    fun deleteBookmarkTemplatePairs(bookmarkTemplates: List<BookmarkTemplate>): Int =
        bookmarkTemplates.sumOf {
            dbConnector.bookmarkTemplateDB.deleteTemplateBookmarkPair(it)
        }

    fun deleteBookmarkTemplatePairsByRightId(rightId: String): Int = dbConnector.bookmarkTemplateDB.deletePairsByRightId(rightId)

    fun applyAllTemplates(): List<TemplateApplicationResult> =
        dbConnector.rightDB
            .getRightIdsForAllTemplates()
            .let { applyTemplates(it) }

    fun applyTemplates(rightIds: List<String>): List<TemplateApplicationResult> =
        rightIds.mapNotNull { rightId ->
            applyTemplate(rightId)
        }

    internal fun applyTemplate(rightId: String): TemplateApplicationResult? {
        // Get Right object
        val right: ItemRight =
            dbConnector.rightDB.getRightsByIds(listOf(rightId)).firstOrNull() ?: return null
        // Exceptions
        val exceptionTemplates: List<ItemRight> = dbConnector.rightDB.getExceptionsByRightId(rightId)
        val exceptionTemplateApplicationResult: List<TemplateApplicationResult> =
            exceptionTemplates.mapNotNull { excTemp ->
                excTemp.rightId?.let { applyTemplate(it) }
            }
        val bookmarksIdsExceptions: Set<Int> =
            dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightIds(exceptionTemplates.mapNotNull { it.rightId })
        val bookmarksExceptions: List<Bookmark> =
            dbConnector.bookmarkDB.getBookmarksByIds(bookmarksIdsExceptions.toList())

        val searchResultsExceptions: Set<String> =
            bookmarksExceptions
                .asSequence()
                .flatMap { b ->
                    val searchExpression: SearchExpression? =
                        b.searchTerm
                            ?.takeIf { it.isNotBlank() }
                            ?.let { SearchGrammar.tryParseToEnd(it) }
                            ?.let {
                                when (it) {
                                    is Parsed -> it.value
                                    is ErrorResult -> throw ParsingException("Parsing error in query: $it")
                                }
                            }
                    dbConnector.searchDB.searchForMetadataIds(
                        searchExpression = searchExpression,
                        limit = null,
                        offset = null,
                        metadataSearchFilter =
                            listOfNotNull(
                                b.paketSigelFilter,
                                b.publicationDateFilter,
                                b.publicationTypeFilter,
                                b.zdbIdFilter,
                            ),
                        rightSearchFilter =
                            listOfNotNull(
                                b.accessStateFilter,
                                b.temporalValidityFilter,
                                b.validOnFilter,
                                b.startDateFilter,
                                b.endDateFilter,
                                b.formalRuleFilter,
                            ),
                        noRightInformationFilter = b.noRightInformationFilter,
                        metadataIdsToIgnore = emptyList(),
                    )
                }.toSet()

        // Receive all bookmark ids
        val bookmarkIds: List<Int> = dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightId(rightId)
        val bookmarks: List<Bookmark> = dbConnector.bookmarkDB.getBookmarksByIds(bookmarkIds)

        // Get search results for each bookmark
        val searchResults: Set<Item> =
            bookmarks
                .asSequence()
                .flatMap { b ->
                    searchQuery(
                        searchTerm = b.searchTerm,
                        limit = null,
                        offset = null,
                        metadataSearchFilter =
                            listOfNotNull(
                                b.paketSigelFilter,
                                b.publicationDateFilter,
                                b.publicationTypeFilter,
                                b.zdbIdFilter,
                            ),
                        rightSearchFilter =
                            listOfNotNull(
                                b.accessStateFilter,
                                b.temporalValidityFilter,
                                b.validOnFilter,
                                b.startDateFilter,
                                b.endDateFilter,
                                b.formalRuleFilter,
                            ),
                        noRightInformationFilter = b.noRightInformationFilter,
                        searchResultsExceptions.toList(),
                    ).results
                }.toSet()

        // Delete all template connections
        dbConnector.itemDB.deleteItemByRightId(rightId)

        // Update last_applied_on field
        dbConnector.rightDB.updateAppliedOnByTemplateId(rightId)

        // Connect Template to all results
        val itemsWithConflicts: Pair<Set<Item>, List<RightError>> =
            findItemsWithConflicts(searchResults, right)
        val searchResultsWithoutConflict = searchResults.subtract(itemsWithConflicts.first)
        val appliedMetadataIds =
            searchResultsWithoutConflict.map { result ->
                dbConnector.itemDB.insertItem(
                    metadataId = result.metadata.metadataId,
                    rightId = rightId,
                )
                result.metadata.metadataId
            }
        return TemplateApplicationResult(
            rightId = rightId,
            appliedMetadataIds = appliedMetadataIds,
            errors = itemsWithConflicts.second,
            exceptionTemplateApplicationResult = exceptionTemplateApplicationResult,
            templateName = right.templateName ?: "Missing Template Name",
        )
    }

    fun getExceptionsByRightId(rightId: String): List<ItemRight> = dbConnector.rightDB.getExceptionsByRightId(rightId)

    /**
     * Errors.
     */
    fun getRightErrorList(
        limit: Int,
        offset: Int,
    ): List<RightError> = dbConnector.rightErrorDB.getErrorList(limit = limit, offset = offset)

    companion object {
        /**
         * Valid patterns: key:value or key:'value1 value2 ...'.
         * Valid special characters: '-:;'
         */
        val SEARCH_KEY_REGEX = Regex("\\w+:[^\"\')\\s]+|\\w+:'(\\s|[^\'])+'|\\w+:\"(\\s|[^\"])+\"")

        fun parseSearchTermToFilters(s: String?): List<SearchFilter> =
            s?.let { tokenizeSearchInput(it) }?.mapNotNull {
                SearchFilter.toSearchFilter(
                    it.substringBefore(":"),
                    it.substringAfter(":").trim(),
                )
            }
                ?: emptyList()

        private fun tokenizeSearchInput(s: String): List<String> {
            val iter = SEARCH_KEY_REGEX.findAll(s).iterator()
            return generateSequence {
                if (iter.hasNext()) {
                    iter.next().value.filter { it != '\'' && it != '\"' }
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

        fun hasSearchTokensWithNoKey(s: String): Boolean =
            s
                .takeIf {
                    it.isNotEmpty()
                }?.let {
                    val tmp =
                        s
                            .trim()
                            .replace(SEARCH_KEY_REGEX, Strings.EMPTY)
                            .trim()
                            .split("\\s+".toRegex())
                    if (tmp.size == 1 && tmp[0].isEmpty()) {
                        return false
                    } else {
                        true
                    }
                } ?: false

        fun hashString(
            type: String,
            input: String,
        ): String {
            val bytes =
                MessageDigest
                    .getInstance(type)
                    .digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun findItemsWithConflicts(
            searchResults: Set<Item>,
            right: ItemRight,
        ): Pair<Set<Item>, List<RightError>> {
            val conflictErrors = emptyList<RightError>().toMutableList()
            val conflictingItems: Set<Item> =
                searchResults
                    .toList()
                    .filter { item ->
                        val rights = item.rights
                        rights
                            .map { r ->
                                if (r.rightId == right.rightId) {
                                    false
                                } else {
                                    val hasConflict = checkForDateConflict(r, right)
                                    if (hasConflict) {
                                        conflictErrors.add(
                                            RightError(
                                                metadataId = item.metadata.metadataId,
                                                conflictType = ConflictType.DATE_OVERLAP,
                                                conflictingRightId = right.rightId ?: "Unknown",
                                                createdOn = OffsetDateTime.now(),
                                                errorId = null,
                                                // TODO: Not sure if this is correct
                                                rightIdSource = right.rightId,
                                                handleId = item.metadata.handle,
                                                message =
                                                    "Start/End-Datum Konflikt: Template '${right.templateName}' steht im Widerspruch" +
                                                        " mit einer Rechteinformation (Id: ${r.rightId}), welche an die" +
                                                        " Metadata-ID ${item.metadata.metadataId} angebunden ist.",
                                            ),
                                        )
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }.any { it }
                    }.toSet()
            return Pair(conflictingItems, conflictErrors.toList())
        }

        fun checkForDateConflict(
            r1: ItemRight,
            r2: ItemRight,
        ): Boolean =
            if (r1.endDate == null && r2.endDate == null) {
                true
            } else if (r1.endDate == null) {
                r2.endDate!! > r1.startDate
            } else if (r2.endDate == null) {
                r1.endDate > r2.startDate
            } else if (r1.endDate >= r2.startDate && r1.endDate <= r2.endDate) {
                true
            } else if (r1.startDate >= r2.startDate && r1.startDate < r2.endDate) {
                true
            } else if (r2.startDate >= r1.startDate && r2.startDate < r1.endDate) {
                true
            } else {
                r1.startDate <= r2.endDate && r1.startDate >= r2.startDate
            }
    }
}
