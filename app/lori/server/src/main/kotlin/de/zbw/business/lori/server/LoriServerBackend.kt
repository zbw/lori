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
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchGrammar
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.business.lori.server.utils.DashboardUtil
import de.zbw.lori.model.ErrorRest
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.RightErrorDB
import io.ktor.http.HttpStatusCode
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.util.Strings
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

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

    internal suspend fun insertRightForHandles(
        right: ItemRight,
        handles: List<String>,
    ): String {
        val pkRight = dbConnector.rightDB.insertRight(right.copy(isTemplate = false, templateName = null))
        dbConnector.itemDB.insertItemBatch(
            handles.map {
                ItemId(
                    handle = it,
                    rightId = pkRight,
                )
            },
        )
        return pkRight
    }

    suspend fun insertItemEntry(
        handle: String,
        rightId: String,
        deleteOnConflict: Boolean = false,
    ): Either<Pair<HttpStatusCode, ErrorRest>, String> =
        if (checkRightConflicts(handle, rightId)) {
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
                .insertItem(
                    ItemId(
                        handle = handle,
                        rightId = rightId,
                    ),
                )?.let { Either.Right(it) }
                ?: Either.Left(Pair(HttpStatusCode.InternalServerError, ApiError.internalServerError()))
        }

    suspend fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> = metadataElems.map { insertMetadataElement(it) }

    suspend fun insertGroup(group: Group): Int = dbConnector.groupDB.insertGroup(group)

    suspend fun insertMetadataElement(metadata: ItemMetadata): String = dbConnector.metadataDB.insertMetadata(metadata)

    suspend fun insertRight(right: ItemRight): String {
        val generatedRightId = dbConnector.rightDB.insertRight(right)
        right.groups?.forEach { group ->
            dbConnector.groupDB.insertGroupRightPair(
                rightId = generatedRightId,
                groupId = group.groupId,
            )
        }
        return generatedRightId
    }

    suspend fun updateGroup(
        group: Group,
        updateBy: String,
    ): Int = dbConnector.groupDB.updateGroup(group, updateBy)

    suspend fun upsertRight(right: ItemRight): Int {
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

    suspend fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.metadataDB.upsertMetadataBatch(metadataElems.map { it })

    suspend fun upsertMetadata(metadata: List<ItemMetadata>): IntArray = dbConnector.metadataDB.upsertMetadataBatch(metadata)

    suspend fun updateMetadataAsDeleted(instant: Instant): Int {
        val deletedHandles = dbConnector.metadataDB.getMetadataHandlesOlderThanLastUpdatedOn(instant)
        return dbConnector.metadataDB.updateMetadataDeleteStatus(deletedHandles, true)
    }

    suspend fun getMetadataList(
        limit: Int,
        offset: Int,
    ): List<ItemMetadata> =
        dbConnector.metadataDB
            .getMetadataRange(limit, offset)
            .takeIf {
                it.isNotEmpty()
            }?.let { metadataList ->
                metadataList.sortedBy { it.handle }
            } ?: emptyList()

    suspend fun getMetadataElementsByIds(handles: List<String>): List<ItemMetadata> = dbConnector.metadataDB.getMetadata(handles)

    suspend fun metadataContainsHandle(handle: String): Boolean = dbConnector.metadataDB.metadataContainsHandle(handle)

    suspend fun rightContainsId(rightId: String): Boolean = dbConnector.rightDB.rightContainsId(rightId)

    suspend fun getItemByHandle(handle: String): Item? =
        dbConnector.metadataDB
            .getMetadata(listOf(handle))
            .takeIf { it.isNotEmpty() }
            ?.first()
            ?.let { meta ->
                val rights =
                    dbConnector.rightDB.getRightIdsByHandle(handle).let {
                        dbConnector.rightDB.getRightsByIds(it)
                    }
                Item(
                    meta,
                    rights,
                )
            }

    suspend fun getGroupById(
        groupId: Int,
        version: Int?,
    ): Group? =
        if (version != null) {
            dbConnector.groupDB.getGroupByIdAndVersion(groupId, version)
        } else {
            dbConnector.groupDB.getGroupById(groupId)
        }

    suspend fun getGroupList(
        limit: Int,
        offset: Int,
    ): List<Group> = dbConnector.groupDB.getGroupList(limit, offset)

    suspend fun getRightById(rightId: String): ItemRight? = getRightsByIds(listOf(rightId)).firstOrNull()

    suspend fun getRightsByIds(rightIds: List<String>): List<ItemRight> = dbConnector.rightDB.getRightsByIds(rightIds)

    suspend fun getItemList(
        limit: Int,
        offset: Int,
    ): List<Item> {
        val receivedMetadata = dbConnector.metadataDB.getMetadataRange(limit, offset)
        return receivedMetadata
            .takeIf {
                it.isNotEmpty()
            }?.let { metadataList ->
                runBlocking {
                    getRightsForMetadata(metadataList)
                }
            } ?: emptyList()
    }

    private suspend fun getRightsForMetadata(metadataList: List<ItemMetadata>): List<Item> =
        coroutineScope {
            val metadataToRights =
                metadataList.map { metadata ->
                    metadata to async { dbConnector.rightDB.getRightIdsByHandle(metadata.handle) }
                }

            return@coroutineScope metadataToRights.map { p ->
                Item(
                    p.first,
                    dbConnector.rightDB.getRightsByIds(p.second.await()),
                )
            }
        }

    suspend fun itemContainsMetadata(handle: String): Boolean = dbConnector.metadataDB.itemContainsHandle(handle)

    suspend fun itemContainsEntry(
        handle: String,
        rightId: String,
    ): Boolean = dbConnector.itemDB.itemContainsEntry(handle, rightId)

    suspend fun countMetadataEntries(): Int =
        dbConnector.searchDB.countSearchMetadata(
            null,
            emptyList(),
            emptyList(),
            null,
        )

    suspend fun countItemByRightId(rightId: String) = dbConnector.itemDB.countItemByRightId(rightId)

    suspend fun deleteItemEntry(
        handle: String,
        rightId: String,
    ) = dbConnector.itemDB.deleteItem(handle, rightId)

    suspend fun deleteItemEntriesByHandle(handle: String) = dbConnector.itemDB.deleteItemByHandle(handle)

    suspend fun deleteItemEntriesByRightId(rightId: String) = dbConnector.itemDB.deleteItemByRightId(rightId)

    suspend fun deleteGroup(groupId: Int): Int {
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

    internal suspend fun deleteMetadataByHandle(handle: String): Int = dbConnector.metadataDB.deleteMetadata(listOf(handle))

    suspend fun deleteRight(rightId: String): Int {
        // Delete exceptions first
        val exceptionIds = dbConnector.rightDB.getExceptionsByRightId(rightId).mapNotNull { it.rightId }
        exceptionIds.forEach {
            deleteItemEntriesByRightId(it)
            deleteBookmarkTemplatePairsByRightId(it)
            dbConnector.groupDB.deleteGroupPairsByRightId(it)
        }
        dbConnector.rightDB.deleteRightsByIds(exceptionIds)
        // Delete rightId
        deleteItemEntriesByRightId(rightId)
        deleteBookmarkTemplatePairsByRightId(rightId)
        dbConnector.groupDB.deleteGroupPairsByRightId(rightId)
        return dbConnector.rightDB.deleteRightsByIds(listOf(rightId))
    }

    suspend fun getRightEntriesByHandle(handle: String): List<ItemRight> =
        dbConnector.rightDB.getRightIdsByHandle(handle).let {
            dbConnector.rightDB.getRightsByIds(it)
        }

    suspend fun deleteSessionById(sessionID: String) = dbConnector.userDB.deleteSessionById(sessionID)

    suspend fun getSessionById(sessionID: String): Session? = dbConnector.userDB.getSessionById(sessionID)

    suspend fun insertSession(session: Session): String = dbConnector.userDB.insertSession(session)

    private suspend fun checkRightConflicts(
        handle: String,
        newRightId: String,
    ): Boolean {
        // Get all right ids
        val rightIds = dbConnector.itemDB.getRightIdsByHandle(handle)
        // Get data for all rights
        val rights: List<ItemRight> = dbConnector.rightDB.getRightsByIds(rightIds)
        val newRight: ItemRight = dbConnector.rightDB.getRightsByIds(listOf(newRightId)).firstOrNull() ?: return false
        // Check for conflicts
        return rights.foldRight(false) { r, acc ->
            acc || checkForDateConflict(newRight, r)
        }
    }

    suspend fun searchQuery(
        searchTerm: String?,
        limit: Int?,
        offset: Int?,
        metadataSearchFilter: List<MetadataSearchFilter> = emptyList(),
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
        noRightInformationFilter: NoRightInformationFilter? = null,
        handlesToIgnore: List<String> = emptyList(),
        facetsOnly: Boolean = false,
    ): SearchQueryResult =
        coroutineScope {
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
            val receivedMetadata: Deferred<List<ItemMetadata>> =
                async {
                    if (!facetsOnly) {
                        dbConnector.searchDB.searchMetadataItems(
                            searchExpression,
                            limit,
                            offset,
                            metadataSearchFilter,
                            rightSearchFilter.takeIf { noRightInformationFilter == null } ?: emptyList(),
                            noRightInformationFilter,
                            handlesToIgnore,
                        )
                    } else {
                        return@async emptyList()
                    }
                }

            // Collect all publication types, zdbIds and paketSigels
            val facetsDef =
                async {
                    dbConnector.searchDB.searchForFacets(
                        searchExpression,
                        metadataSearchFilter,
                        rightSearchFilter,
                        noRightInformationFilter,
                    )
                }

            // Combine Metadata entries with their rights
            val items: List<Item> =
                receivedMetadata
                    .await()
                    .takeIf {
                        it.isNotEmpty()
                    }?.let { metadata ->
                        getRightsForMetadata(metadata)
                    } ?: (emptyList())

            // Acquire number of results
            val numberOfResults =
                async {
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
                }

            val facets = facetsDef.await()

            SearchQueryResult(
                numberOfResults = numberOfResults.await(),
                results = items,
                accessState = facets.accessState,
                hasLicenceContract = facets.hasLicenceContract,
                hasOpenContentLicence = facets.hasOpenContentLicence,
                hasZbwUserAgreement = facets.hasZbwUserAgreement,
                paketSigels = facets.paketSigels,
                publicationType = facets.publicationType,
                templateNamesToOcc = getRightIdsByTemplateNames(facets.templateIdToOccurence),
                zdbIds = facets.zdbIdsJournal + facets.zdbIdsSeries,
                licenceUrl = facets.licenceUrls,
                filtersAsQuery =
                    SearchFilter.filtersToString(
                        filters =
                            (metadataSearchFilter + rightSearchFilter + listOf(noRightInformationFilter))
                                .filterNotNull(),
                    ),
                isPartOfSeries = facets.isPartOfSeries,
            )
        }

    private suspend fun getRightIdsByTemplateNames(idToCount: Map<String, Int>): Map<String, Pair<String, Int>> {
        return dbConnector.rightDB
            .getRightsByTemplateNames(idToCount.keys.toList())
            .mapNotNull { right ->
                if (idToCount[right.templateName] == null || right.rightId == null || right.templateName == null) {
                    return@mapNotNull null
                }
                right.rightId to (right.templateName to idToCount[right.templateName]!!)
            }.toMap()
    }

    suspend fun isException(rightId: String): Boolean = dbConnector.rightDB.isException(rightId)

    suspend fun addExceptionToTemplate(
        rightIdTemplate: String,
        rightIdExceptions: List<String>,
    ): Int =
        dbConnector.rightDB.addExceptionToTemplate(
            rightIdTemplate = rightIdTemplate,
            rightIdExceptions = rightIdExceptions,
        )

    suspend fun insertBookmark(bookmark: Bookmark): Int = dbConnector.bookmarkDB.insertBookmark(bookmark)

    suspend fun deleteBookmark(bookmarkId: Int): Int {
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

    suspend fun updateBookmark(
        bookmarkId: Int,
        bookmark: Bookmark,
    ): Int = dbConnector.bookmarkDB.updateBookmarkById(bookmarkId, bookmark)

    suspend fun getBookmarkById(bookmarkId: Int): Bookmark? = dbConnector.bookmarkDB.getBookmarksByIds(listOf(bookmarkId)).firstOrNull()

    suspend fun getBookmarkList(
        limit: Int,
        offset: Int,
    ): List<Bookmark> = dbConnector.bookmarkDB.getBookmarkList(limit, offset)

    /**
     * Insert template.
     */
    suspend fun insertTemplate(right: ItemRight): String = dbConnector.rightDB.insertRight(right)

    suspend fun getTemplateList(
        limit: Int,
        offset: Int,
    ): List<ItemRight> = dbConnector.rightDB.getTemplateList(limit, offset)

    /**
     * Template-Bookmark Pair.
     */
    suspend fun getBookmarksByRightId(rightId: String): List<Bookmark> {
        val bookmarkIds = dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightId(rightId)
        return dbConnector.bookmarkDB.getBookmarksByIds(bookmarkIds)
    }

    suspend fun deleteBookmarkTemplatePair(
        rightId: String,
        bookmarkId: Int,
    ): Int =
        dbConnector.bookmarkTemplateDB.deleteTemplateBookmarkPair(
            BookmarkTemplate(
                bookmarkId = bookmarkId,
                rightId = rightId,
            ),
        )

    suspend fun insertBookmarkTemplatePair(
        bookmarkId: Int,
        rightId: String,
    ): Int =
        dbConnector.bookmarkTemplateDB.insertTemplateBookmarkPair(
            BookmarkTemplate(
                bookmarkId = bookmarkId,
                rightId = rightId,
            ),
        )

    suspend fun upsertBookmarkTemplatePairs(bookmarkTemplates: List<BookmarkTemplate>): List<BookmarkTemplate> =
        dbConnector.bookmarkTemplateDB.upsertTemplateBookmarkBatch(bookmarkTemplates)

    suspend fun deleteBookmarkTemplatePairs(bookmarkTemplates: List<BookmarkTemplate>): Int =
        bookmarkTemplates.sumOf {
            dbConnector.bookmarkTemplateDB.deleteTemplateBookmarkPair(it)
        }

    suspend fun deleteBookmarkTemplatePairsByRightId(rightId: String): Int = dbConnector.bookmarkTemplateDB.deletePairsByRightId(rightId)

    suspend fun checkForRightErrors(createdBy: String): List<RightError> {
        val gapErrors = checkForGAPErrors(createdBy)
        val noRightErrors = checkForNoRightErrors(createdBy)
        return gapErrors + noRightErrors
    }

    suspend fun deleteErrorsByTestId(testId: String): Int = dbConnector.rightErrorDB.deleteErrorByTestId(testId)

    internal suspend fun checkForNoRightErrors(createdBy: String): List<RightError> {
        dbConnector.rightErrorDB.deleteErrorsByType(ConflictType.NO_RIGHT)
        val metadataWithoutRights =
            dbConnector.searchDB.searchMetadataItems(
                searchExpression = null,
                limit = null,
                offset = null,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
                noRightInformationFilter = NoRightInformationFilter(),
            )
        val errors =
            metadataWithoutRights.map { metadata ->
                RightError(
                    handle = metadata.handle,
                    message = "Handle ${metadata.handle} besitzt keine Rechteinformation.",
                    errorId = null,
                    createdOn = OffsetDateTime.now(ZoneOffset.UTC),
                    conflictingWithRightId = null,
                    conflictByRightId = null,
                    conflictType = ConflictType.NO_RIGHT,
                    conflictByContext = metadata.paketSigel ?: metadata.collectionName,
                    testId = null,
                    createdBy = createdBy,
                )
            }
        val errorIds = dbConnector.rightErrorDB.insertErrorsBatch(errors)
        return errors.mapIndexed { index, rightError -> rightError.copy(errorId = errorIds[index]) }
    }

    internal suspend fun checkForGAPErrors(createdBy: String): List<RightError> {
        dbConnector.rightErrorDB.deleteErrorsByType(ConflictType.GAP)
        val handles: List<String> = dbConnector.itemDB.getAllHandles()
        val metadata: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(handles)
        val items =
            metadata.map { m ->
                val rightIds = dbConnector.rightDB.getRightIdsByHandle(m.handle)
                Item(
                    metadata = m,
                    rights = dbConnector.rightDB.getRightsByIds(rightIds),
                )
            }
        val errors = items.map { DashboardUtil.checkForGapErrors(it, createdBy) }.flatten()
        val errorIds = dbConnector.rightErrorDB.insertErrorsBatch(errors)
        return errors.mapIndexed { index, rightError -> rightError.copy(errorId = errorIds[index]) }
    }

    suspend fun applyAllTemplates(
        skipTemplateDrafts: Boolean,
        dryRun: Boolean,
        createdBy: String,
    ): List<TemplateApplicationResult> {
        dbConnector.rightErrorDB.deleteErrorsByType(ConflictType.DATE_OVERLAP)
        return dbConnector.rightDB
            .getRightIdsForAllTemplates()
            .let {
                applyTemplates(
                    it,
                    skipTemplateDrafts,
                    dryRun,
                    createdBy,
                )
            }
    }

    suspend fun applyTemplates(
        rightIds: List<String>,
        skipTemplateDrafts: Boolean,
        dryRun: Boolean,
        createdBy: String,
    ): List<TemplateApplicationResult> =
        rightIds.mapNotNull { rightId ->
            applyTemplate(
                rightId,
                skipTemplateDrafts,
                dryRun,
                createdBy,
            )
        }

    internal suspend fun applyTemplate(
        rightId: String,
        skipTemplateDrafts: Boolean,
        dryRun: Boolean,
        createdBy: String,
    ): TemplateApplicationResult? {
        // Get Right object
        val right: ItemRight =
            dbConnector.rightDB.getRightsByIds(listOf(rightId)).firstOrNull() ?: return null
        if (skipTemplateDrafts && right.lastAppliedOn == null) {
            // Draft will be skipped for now.
            return null
        }
        // Exceptions
        val exceptionTemplates: List<ItemRight> = dbConnector.rightDB.getExceptionsByRightId(rightId)
        val exceptionTemplateApplicationResult: List<TemplateApplicationResult> =
            exceptionTemplates.mapNotNull { excTemp ->
                excTemp.rightId?.let {
                    applyTemplate(
                        it,
                        false,
                        dryRun,
                        createdBy,
                    )
                }
            }
        val bookmarksIdsExceptions: Set<Int> =
            dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightIds(exceptionTemplates.mapNotNull { it.rightId })
        val bookmarksExceptions: List<Bookmark> =
            dbConnector.bookmarkDB.getBookmarksByIds(bookmarksIdsExceptions.toList())

        val searchResultsExceptions: Set<String> =
            bookmarksExceptions
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
                    dbConnector.searchDB.searchForHandles(
                        searchExpression = searchExpression,
                        limit = null,
                        offset = null,
                        metadataSearchFilter = b.getAllMetadataFilter(),
                        rightSearchFilter = b.getAllRightFilter(),
                        noRightInformationFilter = b.noRightInformationFilter,
                        handlesToIgnore = emptyList(),
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
                    runBlocking {
                        searchQuery(
                            searchTerm = b.searchTerm,
                            limit = null,
                            offset = null,
                            metadataSearchFilter = b.getAllMetadataFilter(),
                            rightSearchFilter = b.getAllRightFilter(),
                            noRightInformationFilter = b.noRightInformationFilter,
                            handlesToIgnore = searchResultsExceptions.toList(),
                        )
                    }.results
                }.toSet()

        if (!dryRun) {
            // Delete all template connections
            dbConnector.itemDB.deleteItemByRightId(rightId)

            // Update last_applied_on field
            dbConnector.rightDB.updateAppliedOnByTemplateId(rightId)
        }

        // Connect Template to all results
        if (!dryRun) {
            val itemsWithConflicts: Map<Item, List<RightError>> =
                findItemsWithConflicts(searchResults, right, null, createdBy)
            val searchResultsWithoutConflict: Set<Item> = searchResults.subtract(itemsWithConflicts.keys)
            dbConnector.rightErrorDB.deleteByCausingRightId(right.rightId!!)
            dbConnector.rightErrorDB.insertErrorsBatch(itemsWithConflicts.values.flatten())
            dbConnector.itemDB.insertItemBatch(
                searchResultsWithoutConflict.map {
                    ItemId(
                        handle = it.metadata.handle,
                        rightId = rightId,
                    )
                },
            )
            return TemplateApplicationResult(
                rightId = rightId,
                appliedMetadataHandles = searchResultsWithoutConflict.map { it.metadata.handle },
                errors = itemsWithConflicts.values.flatten(),
                exceptionTemplateApplicationResult = exceptionTemplateApplicationResult,
                templateName = right.templateName ?: "Missing Template Name",
                testId = null,
                numberOfErrors = itemsWithConflicts.values.flatten().size,
            )
        } else {
            val testId = UUID.randomUUID().toString()
            val itemsWithConflicts: Map<Item, List<RightError>> =
                findItemsWithConflicts(searchResults, right, testId, createdBy)
            val searchResultsWithoutConflict: Set<Item> = searchResults.subtract(itemsWithConflicts.keys)
            dbConnector.rightErrorDB.insertErrorsBatch(itemsWithConflicts.values.flatten())
            return TemplateApplicationResult(
                rightId = rightId,
                // TODO(CB): Don't send back thousands of errors for now
                errors = emptyList(),
                appliedMetadataHandles = searchResultsWithoutConflict.map { it.metadata.handle },
                exceptionTemplateApplicationResult = exceptionTemplateApplicationResult,
                templateName = right.templateName ?: "Missing Template Name",
                testId = testId,
                numberOfErrors = itemsWithConflicts.values.flatten().size,
            )
        }
    }

    suspend fun getExceptionsByRightId(rightId: String): List<ItemRight> = dbConnector.rightDB.getExceptionsByRightId(rightId)

    /**
     * Errors.
     */
    suspend fun getRightErrorList(
        limit: Int,
        offset: Int,
        searchFilters: List<DashboardSearchFilter>,
        testId: String?,
    ): ErrorQueryResult =
        coroutineScope {
            val results =
                async {
                    dbConnector.rightErrorDB.getErrorList(
                        limit = limit,
                        offset = offset,
                        filters = searchFilters,
                        testId = testId,
                    )
                }
            val totalNumber =
                async {
                    dbConnector.rightErrorDB.getCount(filters = searchFilters, testId = testId)
                }

            val occurrenceConflictTypes =
                async {
                    dbConnector.rightErrorDB
                        .getOccurrences(
                            column = RightErrorDB.COLUMN_CONFLICTING_TYPE,
                            filters = searchFilters,
                            testId = testId,
                        ).map { ConflictType.valueOf(it) }
                }

            val occurrenceContextNames =
                async {
                    dbConnector.rightErrorDB.getOccurrences(
                        column = RightErrorDB.COLUMN_CONFLICT_BY_CONTEXT,
                        filters = searchFilters,
                        testId = testId,
                    )
                }
            return@coroutineScope ErrorQueryResult(
                totalNumberOfResults = totalNumber.await(),
                contextNames = occurrenceContextNames.await().toSet(),
                conflictTypes = occurrenceConflictTypes.await().toSet(),
                results = results.await(),
            )
        }

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
                } == true

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
            template: ItemRight,
            testId: String?,
            createdBy: String,
        ): Map<Item, List<RightError>> =
            searchResults
                .toList()
                .mapNotNull { item ->
                    val errors =
                        checkApplicationForErrorsByItem(
                            item,
                            template,
                            testId,
                            createdBy,
                        )
                    if (errors.isEmpty()) {
                        null
                    } else {
                        item to errors
                    }
                }.toMap()

        private fun checkApplicationForErrorsByItem(
            item: Item,
            template: ItemRight,
            testId: String?,
            createdBy: String,
        ): List<RightError> =
            item.rights
                .filter { r ->
                    (r.rightId != template.rightId)
                }.mapNotNull { r ->
                    checkForDateConflict(r, template)
                        .takeIf { it }
                        ?.let {
                            RightError(
                                handle = item.metadata.handle,
                                conflictingWithRightId = r.rightId ?: "No Right Id",
                                conflictType = ConflictType.DATE_OVERLAP,
                                conflictByRightId = template.rightId ?: "No Right Id",
                                conflictByContext = template.templateName,
                                createdOn = OffsetDateTime.now(ZoneOffset.UTC),
                                errorId = null,
                                message =
                                    "Start/End-Datum Konflikt: Template '${template.templateName}' steht im Widerspruch" +
                                        " mit einer Rechteinformation (Id: ${r.rightId}), welche an den" +
                                        " Handle ${item.metadata.handle} angebunden ist.",
                                testId = testId,
                                createdBy = createdBy,
                            )
                        }
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
