package de.zbw.business.lori.server

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.connector.DAConnector.Companion.DEFAULT_IMPORT_CHUNK_SIZE
import de.zbw.api.lori.server.exception.ResourceConflictException
import de.zbw.api.lori.server.exception.ResourceStillInUseException
import de.zbw.api.lori.server.route.ApiError
import de.zbw.api.lori.server.type.Either
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.business.lori.server.type.ExportJob
import de.zbw.business.lori.server.type.GenericJob
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.ItemRow
import de.zbw.business.lori.server.type.JobKind
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.RightIdTemplateName
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchGrammar
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.Session
import de.zbw.business.lori.server.type.SortInformation
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.business.lori.server.utils.DashboardUtil
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.business.lori.server.utils.TimezoneUtil.utcOffsetDateTimeToBerlinDate
import de.zbw.lori.model.ErrorRest
import de.zbw.lori.model.RelationshipRest
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.FacetTransientSet
import de.zbw.persistence.lori.server.RightErrorDB
import de.zbw.persistence.lori.server.types.MetadataHandleLastUpdatedTransient
import io.ktor.http.HttpStatusCode
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.util.Strings
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.filter
import kotlin.math.ceil

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
        createdBy: String,
    ): String {
        val pkRight = dbConnector.rightDB.insertRight(right)
        dbConnector.itemDB.upsertItemBatch(
            createdBy = createdBy,
            itemIds =
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
        createdBy: String = "",
    ): Either<Pair<HttpStatusCode, ErrorRest>, String> =
        if (checkRightConflicts(handle, rightId)) {
            if (deleteOnConflict) {
                dbConnector.rightDB.deleteRightsByIds(listOf(rightId))
            }
            Either.Left(
                Pair(
                    HttpStatusCode.Conflict,
                    ApiError.conflictError(ApiError.CONFLICT_RIGHTS),
                ),
            )
        } else {
            dbConnector.itemDB
                .insertItem(
                    itemId =
                        ItemId(
                            handle = handle,
                            rightId = rightId,
                        ),
                    createdBy = createdBy,
                )?.let { Either.Right(it) }
                ?: Either.Left(Pair(HttpStatusCode.InternalServerError, ApiError.internalServerError()))
        }

    suspend fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> = metadataElems.map { insertMetadataElement(it) }

    suspend fun insertGroup(group: Group): Int = dbConnector.groupDB.insertGroup(group)

    suspend fun insertMetadataElement(metadata: ItemMetadata): String = dbConnector.metadataDB.insertMetadata(metadata)

    suspend fun insertRight(right: ItemRight): String {
        val generatedRightId = dbConnector.rightDB.insertRight(right)
        right.groupIds?.forEach { id ->
            dbConnector.groupDB.insertGroupRightPair(
                rightId = generatedRightId,
                groupId = id,
            )
        }
        createRelationshipsByRight(right)
        return generatedRightId
    }

    suspend fun updateGroup(
        group: Group,
        updateBy: String,
    ): Int = dbConnector.groupDB.updateGroup(group, updateBy)

    suspend fun upsertRight(right: ItemRight): Either<Pair<HttpStatusCode, ErrorRest>, Int> {
        if (checkRightConflictsOnUpdate(right) && !right.isTemplate) {
            return Either.Left(
                Pair(
                    HttpStatusCode.Conflict,
                    ApiError.conflictError(ApiError.CONFLICT_RIGHTS),
                ),
            )
        }
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

        val insertedRows = dbConnector.rightDB.upsertRight(right)
        if (insertedRows == 1) {
            createRelationshipsByRight(right)
        }

        return Either.Right(insertedRows)
    }

    suspend fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.metadataDB.upsertMetadataBatch(metadataElems.map { it })

    suspend fun upsertMetadata(metadata: List<ItemMetadata>): IntArray = dbConnector.metadataDB.upsertMetadataBatch(metadata)

    suspend fun updateMetadataAsDeleted(instant: Instant): Int {
        val deletedHandles: List<MetadataHandleLastUpdatedTransient> =
            dbConnector.metadataDB.getMetadataHandlesOlderThanLastUpdatedOn(
                instant,
            )
        deletedHandles.forEach { transient ->
            if (!transient.isDeleted) {
                // TODAY - 1 is the deletion date
                deleteAndUpdateManualRightsByHandle(
                    instant.atZone(TimezoneUtil.TIME_ZONE_BERLIN).toLocalDate(),
                    transient.handle,
                )
            } else {
                // Entry got already deleted in past. Then lastUpdatedOn is deletion date
                deleteAndUpdateManualRightsByHandle(
                    transient.lastUpdatedOn
                        .atZone(TimezoneUtil.TIME_ZONE_BERLIN)
                        .toLocalDate(),
                    transient.handle,
                )
            }
        }
        return dbConnector.metadataDB.updateMetadataDeleteStatus(handles = deletedHandles.map { it.handle }, status = true)
    }

    suspend fun deleteAndUpdateManualRightsByHandle(
        deletionDate: LocalDate,
        handle: String,
    ): Int {
        val rightIds = dbConnector.itemDB.getRightIdsByHandle(handle)
        if (rightIds.isEmpty()) {
            return 0
        }
        var deletionsAndUpdates = 0
        val allRights =
            dbConnector.rightDB
                .getRightsByIds(
                    rightIds,
                )

        val manualRights =
            allRights.filter { !it.isTemplate }

        val templateRights =
            allRights.filter { it.isTemplate }

        manualRights
            .filter { it.startDate > deletionDate }
            .forEach {
                deletionsAndUpdates += deleteRight(it.rightId!!)
            }

        // Templates applying only in the future will be deleted as well
        templateRights
            .filter { it.startDate > deletionDate }
            .forEach {
                deletionsAndUpdates += deleteItemEntry(handle, it.rightId!!)
            }

        val rightToSetNewEndDate =
            manualRights
                .filter {
                    (it.endDate == null || it.endDate > deletionDate) && it.startDate <= deletionDate
                }

        val deletionDateGerman = deletionDate.format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))
        if (rightToSetNewEndDate.isNotEmpty()) {
            rightToSetNewEndDate
                .first()
                .let {
                    deletionsAndUpdates +=
                        dbConnector.rightDB.upsertRight(
                            it.copy(
                                endDate = deletionDate,
                                lastUpdatedBy = "Automatisch $deletionDateGerman",
                                notesManagementRelated =
                                    "Enddatum automatisch auf Item-Löschdatum" +
                                        " ${deletionDate.format(DateTimeFormatter.ofPattern("dd-MM-YYYY"))}" +
                                        " gesetzt.",
                            ),
                        )
                }
        }
        val currentTemplate =
            dbConnector.rightDB
                .getRightsByIds(
                    rightIds,
                ).filter { it.isTemplate }
                .filter {
                    (it.endDate == null || it.endDate > deletionDate) && it.startDate <= deletionDate
                }

        if (currentTemplate.isNotEmpty()) {
            // Remove old template
            val templateId = currentTemplate.first().rightId!!
            val firstTemplateApplication: LocalDate =
                dbConnector.itemDB
                    .getItemRowByHandleAndRightId(handle = handle, rightId = templateId)
                    ?.createdOn
                    ?.let { utcOffsetDateTimeToBerlinDate(it) }
                    ?: throw InternalError("Missing create date in item table. This should never happen")
            deletionsAndUpdates +=
                dbConnector
                    .itemDB
                    .deleteItem(
                        rightId = templateId,
                        handle = handle,
                    )
            // Create a new manual entry
            val newManualRight: ItemRight =
                currentTemplate
                    .first()
                    .copy(
                        startDate =
                            if (currentTemplate.first().startDate < firstTemplateApplication) {
                                firstTemplateApplication
                            } else {
                                currentTemplate.first().startDate
                            },
                        isTemplate = false,
                        templateName = null,
                        templateDescription = null,
                        predecessorId = null,
                        successorId = null,
                        exceptionOfId = null,
                        hasExceptionId = null,
                        endDate = deletionDate,
                        createdBy = "Automatisch $deletionDateGerman",
                        lastUpdatedBy = "Automatisch $deletionDateGerman",
                        notesManagementRelated =
                            "Automatisch erzeugt, um Rechteinformationen aus ursprünglicher" +
                                " Template-Zuordnung Template https://${config.url}?templateId=$templateId" +
                                " bis zum Item-Löschdatum" +
                                " $deletionDateGerman abzubilden",
                    )
            val newManualRightId = dbConnector.rightDB.insertRight(newManualRight)
            dbConnector.itemDB.insertItem(
                itemId = ItemId(handle = handle, rightId = newManualRightId),
                createdBy = "lori",
            )
        }

        return deletionsAndUpdates
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

    suspend fun getItemRowsByHandleAndRightId(
        handle: String,
        rightId: String,
    ): ItemRow? {
        val rows: List<ItemRow> = dbConnector.rightDB.getItemRowsByHandle(handle)
        return rows.firstOrNull { it.rightId == rightId }
    }

    suspend fun metadataContainsHandle(handle: String): Boolean = dbConnector.metadataDB.metadataContainsHandle(handle)

    suspend fun rightContainsId(rightId: String): Boolean = dbConnector.rightDB.rightContainsId(rightId)

    suspend fun getItemByHandle(handle: String): Item? =
        dbConnector.metadataDB
            .getMetadata(listOf(handle))
            .takeIf { it.isNotEmpty() }
            ?.first()
            ?.let { meta ->
                val rights =
                    dbConnector.rightDB.getItemRowsByHandle(handle).let {
                        dbConnector.rightDB.getRightsByIds(it.map { item -> item.rightId })
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
                getRightsForMetadata(metadataList)
            } ?: emptyList()
    }

    private suspend fun getRightsForMetadata(metadataList: List<ItemMetadata>): List<Item> =
        coroutineScope {
            val metadataToRights: List<Pair<ItemMetadata, Deferred<List<ItemRow>>>> =
                metadataList.map { metadata ->
                    metadata to async(Dispatchers.IO) { dbConnector.rightDB.getItemRowsByHandle(metadata.handle) }
                }

            return@coroutineScope metadataToRights.map { p ->
                val itemRows: List<ItemRow> = p.second.await()
                val rightIdToItemRow: Map<String, ItemRow> = itemRows.associateBy { it.rightId }
                val rights: List<ItemRight> =
                    dbConnector.rightDB
                        .getRightsByIds(itemRows.map { it.rightId })

                val adjustedRights: List<ItemRight> =
                    rights
                        .map { r ->
                            val firstApplicationDate =
                                r.rightId
                                    ?.let {
                                        rightIdToItemRow[it]
                                    }?.createdOn
                                    ?.let { utcOffsetDateTimeToBerlinDate(it) }

                            if (firstApplicationDate == null) {
                                return@map listOf(r)
                            }
                            filterAndAdjustRightsByDate(
                                listOf(r),
                                firstApplicationDate,
                            )
                        }.flatten()
                Item(
                    p.first,
                    adjustedRights,
                )
            }
        }

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

    suspend fun countItemByRightId(rightId: String): Int = dbConnector.itemDB.countItemByRightId(rightId)

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

    suspend fun deleteRight(rightId: String): Int {
        // Delete exceptions first
        val exceptionId: String? = dbConnector.rightDB.getExceptionByRightId(rightId)?.rightId
        exceptionId?.also {
            deleteItemEntriesByRightId(it)
            deleteBookmarkTemplatePairsByRightId(it)
            dbConnector.groupDB.deleteGroupPairsByRightId(it)
        }
        exceptionId?.let {
            dbConnector.rightDB.deleteRightsByIds(listOf(it))
        }
        // Delete rightId
        deleteItemEntriesByRightId(rightId)
        deleteBookmarkTemplatePairsByRightId(rightId)
        dbConnector.groupDB.deleteGroupPairsByRightId(rightId)
        return dbConnector.rightDB.deleteRightsByIds(listOf(rightId))
    }

    suspend fun getRightEntriesByHandle(handle: String): List<ItemRight> {
        val itemTables = dbConnector.rightDB.getItemRowsByHandle(handle)
        val rightIdToItemTable = itemTables.associateBy { it.rightId }
        val rights = dbConnector.rightDB.getRightsByIds(itemTables.map { itemTable -> itemTable.rightId })
        val (templates, nonTemplates) = rights.partition { it.isTemplate }
        val adjustedTemplates =
            templates
                .map { t ->
                    val itemTable = rightIdToItemTable[t.rightId] ?: return emptyList()
                    filterAndAdjustRightsByDate(
                        templatesAndRights = listOf(t),
                        firstApplicationDate =
                            itemTable
                                .createdOn
                                ?.let { utcOffsetDateTimeToBerlinDate(it) }
                                ?: FALLBACK_DATE,
                    )
                }.flatten()
        return adjustedTemplates + nonTemplates
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

    private suspend fun checkRightConflictsOnUpdate(newRight: ItemRight): Boolean {
        if (newRight.isTemplate || newRight.rightId == null) {
            return false // Templates are not verified here.
        }
        // Get all handles
        val handles = dbConnector.itemDB.getHandlesByRightId(newRight.rightId)
        if (handles.size != 1) {
            return false // No handle makes no sense
        }
        // Get all right ids
        val rightIds = dbConnector.itemDB.getRightIdsByHandle(handles.first())
        // Get data for all rights
        val rights: List<ItemRight> =
            dbConnector.rightDB
                .getRightsByIds(rightIds)
                .filter { it.rightId != newRight.rightId } // Don't keep the own right id, because an update is done
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
        noFacets: Boolean = false,
        sortInformation: SortInformation = SortInformation.DEFAULT,
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
                async(Dispatchers.IO) {
                    if (!facetsOnly) {
                        dbConnector.searchDB.searchMetadataItems(
                            searchExpression,
                            limit,
                            offset,
                            metadataSearchFilter,
                            rightSearchFilter.takeIf { noRightInformationFilter == null } ?: emptyList(),
                            noRightInformationFilter,
                            handlesToIgnore,
                            sortInformation,
                        )
                    } else {
                        return@async emptyList()
                    }
                }

            // Collect all publication types, zdbIds and paketSigels
            val facetsDef =
                async(Dispatchers.IO) {
                    if (!noFacets) {
                        dbConnector.searchDB.searchForFacets(
                            searchExpression,
                            metadataSearchFilter,
                            rightSearchFilter,
                            noRightInformationFilter,
                        )
                    } else {
                        return@async FacetTransientSet(
                            accessState = emptyMap(),
                            licenceContracts = 0,
                            ccLicenceNoRestrictions = 0,
                            zbwUserAgreements = 0,
                            noLegalRisks = 0,
                            isPartOfSeries = emptyMap(),
                            licenceUrls = emptyMap(),
                            paketSigels = emptyMap(),
                            publicationType = emptyMap(),
                            templateIdToOccurence = emptyMap(),
                            zdbIds = emptyMap(),
                        )
                    }
                }

            // Combine Metadata entries with their rights
            val items: List<Item> =
                receivedMetadata
                    .await()
                    .takeIf {
                        it.isNotEmpty()
                    }?.map { metadata ->
                        metadata.copy(
                            isbn = filterISBNs(metadata.isbn)?.toList(),
                        )
                    }?.let { metadata ->
                        getRightsForMetadata(metadata)
                    } ?: (emptyList())

            // Acquire the number of results
            val numberOfResults =
                async(Dispatchers.IO) {
                    items
                        .takeIf { it.isNotEmpty() || offset != 0 || facetsOnly }
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
                licenceContracts = facets.licenceContracts,
                ccLicenceNoRestrictions = facets.ccLicenceNoRestrictions,
                noLegalRisks = facets.noLegalRisks,
                zbwUserAgreements = facets.zbwUserAgreements,
                paketSigels = sumUpIndividualMapEntries(facets.paketSigels),
                publicationType = facets.publicationType,
                templateNamesToOcc = getRightIdsByTemplateNames(facets.templateIdToOccurence),
                zdbIds = sumUpIndividualMapEntries(facets.zdbIds),
                licenceUrl = facets.licenceUrls,
                filtersAsQuery =
                    SearchFilter.filtersToString(
                        filters =
                            (metadataSearchFilter + rightSearchFilter + listOf(noRightInformationFilter))
                                .filterNotNull(),
                    ),
                isPartOfSeries = sumUpIndividualMapEntries(facets.isPartOfSeries),
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
        rightIdException: String,
    ): Int =
        dbConnector.rightDB.addExceptionToTemplate(
            rightIdTemplate = rightIdTemplate,
            rightIdException = rightIdException,
        )

    suspend fun removeExceptionFromTemplate(
        rightIdTemplate: String,
        rightIdException: String,
    ): Int {
        val template = dbConnector.rightDB.getRightsByIds(listOf(rightIdTemplate)).firstOrNull()
        if (template == null || template.hasExceptionId == null || template.hasExceptionId != rightIdException) {
            return 0
        }
        if (template.lastAppliedOn != null) {
            throw ResourceConflictException(
                "Das Template \"${template.templateName}\" wurde bereits angewandt. Die Ausnahme kann nicht mehr entfernt werden.",
            )
        }
        return dbConnector.rightDB.removeExceptionTemplateConnection(
            rightIdTemplate = rightIdTemplate,
            rightIdException = rightIdException,
        )
    }

    suspend fun insertBookmark(bookmark: Bookmark): Int {
        val bookmarkIdsSameFilters = dbConnector.bookmarkDB.getBookmarkIdByQuerystring(bookmark.computeQueryString())
        if (bookmarkIdsSameFilters.isEmpty()) {
            return dbConnector.bookmarkDB.insertBookmark(bookmark)
        } else {
            throw ResourceConflictException(
                "Es existiert bereits eine andere gespeicherte Suche mit den identischen Suchfiltern: '${bookmarkIdsSameFilters[0]}'",
            )
        }
    }

    suspend fun getTemplateNamesByBookmark(bookmark: Bookmark): List<RightIdTemplateName> =
        bookmark.rightIdFilter
            ?.rightIds
            ?.let { rightIds: List<String> ->
                getRightsByIds(rightIds)
            }?. filter { r -> r.rightId != null && r.templateName != null }
            ?.map { r: ItemRight ->
                RightIdTemplateName(
                    rightId = r.rightId!!,
                    templateName = r.templateName!!,
                )
            }
            ?: emptyList()

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
                    "Gespeicherte Suche '${bookmark.bookmarkName} ($bookmarkId)' wird noch von folgenden Templates verwendet: " +
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
    ): Int {
        val bookmarkIdsSameFilters = dbConnector.bookmarkDB.getBookmarkIdByQuerystring(bookmark.computeQueryString())
        return if (bookmarkIdsSameFilters.isEmpty() ||
            (bookmarkIdsSameFilters.size == 1 && bookmarkIdsSameFilters[0] == bookmark.bookmarkId)
        ) {
            dbConnector.bookmarkDB.updateBookmarkById(bookmarkId, bookmark)
        } else {
            throw ResourceConflictException(
                "Es existiert bereits eine andere gespeicherte Suche mit den identischen Suchfiltern: '${bookmarkIdsSameFilters[0]}'",
            )
        }
    }

    suspend fun getBookmarkById(bookmarkId: Int): Bookmark? = dbConnector.bookmarkDB.getBookmarksByIds(listOf(bookmarkId)).firstOrNull()

    suspend fun getBookmarkList(
        limit: Int,
        offset: Int,
    ): List<Bookmark> = dbConnector.bookmarkDB.getBookmarkList(limit, offset)

    /**
     * Insert template.
     */
    suspend fun insertTemplate(right: ItemRight): String = insertRight(right)

    suspend fun getTemplateList(
        limit: Int,
        offset: Int,
        draft: Boolean? = null,
        exception: Boolean? = null,
        excludes: List<String>? = null,
        hasException: Boolean? = null,
    ): List<ItemRight> =
        dbConnector.rightDB.getTemplateList(
            limit = limit,
            offset = offset,
            draftFilter = draft,
            exceptionFilter = exception,
            excludes = excludes,
            hasException = hasException,
        )

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

    suspend fun checkForRightErrors(createdBy: String): Int {
        val gapErrorsCount = checkForGAPErrors(createdBy)
        val noRightErrorsCount = checkForNoRightErrors(createdBy)
        val deletionErrorsCount = checkForDeletionErrors(createdBy)
        return gapErrorsCount + noRightErrorsCount + deletionErrorsCount
    }

    suspend fun deleteErrorsByTestId(testId: String): Int = dbConnector.rightErrorDB.deleteErrorByTestId(testId)

    internal suspend fun checkForDeletionErrors(createdBy: String): Int {
        dbConnector.rightErrorDB.deleteErrorsByType(ConflictType.DELETION)
        var errorCount = 0
        val deletedMetadataCount = dbConnector.metadataDB.getDeletedMetadataCount()
        for (offsetCounter in 0..<ceil(deletedMetadataCount.toDouble() / DEFAULT_CHUNK_SIZE).toInt()) {
            val deletedMetadata =
                dbConnector.metadataDB.getDeletedMetadata(
                    offset = offsetCounter * DEFAULT_IMPORT_CHUNK_SIZE,
                    limit = DEFAULT_CHUNK_SIZE,
                )
            val errors =
                deletedMetadata.map { metadata ->
                    RightError(
                        handle = metadata.handle,
                        message = "Handle ist gelöscht worden",
                        errorId = null,
                        createdOn = OffsetDateTime.now(TimezoneUtil.TIME_ZONE_UTC),
                        conflictingWithRightId = "gelöscht, zuletzt importiert am ${metadata.lastUpdatedOn}",
                        conflictByRightId = null,
                        conflictType = ConflictType.DELETION,
                        // TODO(CB): Clarify with Jana how to present multiple values
                        conflictByContext = metadata.paketSigel?.joinToString(separator = ",") ?: metadata.collectionName,
                        testId = null,
                        createdBy = createdBy,
                    )
                }
            dbConnector.rightErrorDB.insertErrorsBatch(errors)
            errorCount += errors.size
        }
        return errorCount
    }

    internal suspend fun checkForNoRightErrors(createdBy: String): Int {
        dbConnector.rightErrorDB.deleteErrorsByType(ConflictType.NO_RIGHT)
        val handlesWithoutRightsCount =
            dbConnector.searchDB.countSearchMetadata(
                searchExpression = null,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
                noRightInformationFilter = NoRightInformationFilter(),
            )
        var errorCount = 0
        for (offsetCounter in 0..<ceil(handlesWithoutRightsCount.toDouble() / DEFAULT_CHUNK_SIZE).toInt()) {
            val metadataWithoutRights =
                dbConnector.searchDB.searchMetadataItems(
                    searchExpression = null,
                    limit = DEFAULT_CHUNK_SIZE,
                    offset = offsetCounter * DEFAULT_CHUNK_SIZE,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = NoRightInformationFilter(),
                    sortInformation = SortInformation.DEFAULT,
                )
            val errors =
                metadataWithoutRights.map { metadata ->
                    RightError(
                        handle = metadata.handle,
                        message = "Handle ${metadata.handle} besitzt keine Rechteinformation.",
                        errorId = null,
                        createdOn =
                            OffsetDateTime.now(
                                TimezoneUtil.TIME_ZONE_UTC,
                            ),
                        conflictingWithRightId = null,
                        conflictByRightId = null,
                        conflictType = ConflictType.NO_RIGHT,
                        conflictByContext = metadata.paketSigel?.joinToString(separator = ",") ?: metadata.collectionName,
                        testId = null,
                        createdBy = createdBy,
                    )
                }
            dbConnector.rightErrorDB.insertErrorsBatch(errors)
            errorCount += errors.size
        }
        return errorCount
    }

    internal suspend fun checkForGAPErrors(createdBy: String): Int {
        dbConnector.rightErrorDB.deleteErrorsByType(ConflictType.GAP)
        val handlesCount = dbConnector.itemDB.getHandlesCount()
        var errorCount = 0

        for (offsetCounter in 0..<ceil(handlesCount.toDouble() / DEFAULT_CHUNK_SIZE).toInt()) {
            val handles: List<String> =
                dbConnector.itemDB.getDistinctHandlesByOffset(
                    limit = DEFAULT_CHUNK_SIZE,
                    offset = offsetCounter * DEFAULT_CHUNK_SIZE,
                )
            val metadata: List<ItemMetadata> = dbConnector.metadataDB.getMetadata(handles)
            val items =
                metadata.map { m ->
                    val items = dbConnector.rightDB.getItemRowsByHandle(m.handle)
                    Item(
                        metadata = m,
                        rights = dbConnector.rightDB.getRightsByIds(items.map { it.rightId }),
                    )
                }
            val errors = items.map { DashboardUtil.checkForGapErrors(it, createdBy) }.flatten()
            dbConnector.rightErrorDB.insertErrorsBatch(errors)
            errorCount += errors.size
        }
        return errorCount
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
            }.sortedBy { it.rightId }
    }

    suspend fun applyTemplates(
        rightIds: List<String>,
        skipTemplateDrafts: Boolean,
        dryRun: Boolean,
        createdBy: String,
    ): List<TemplateApplicationResult> {
        val templateApplication =
            TemplateApplication(
                dbConnector = dbConnector,
                backend = this,
            )
        return rightIds.map { rightId ->
            templateApplication.applyTemplate(
                rightId,
                skipTemplateDrafts,
                dryRun,
                createdBy,
            )
        }
    }

    suspend fun getExceptionByRightId(rightId: String): ItemRight? = dbConnector.rightDB.getExceptionByRightId(rightId)

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
                async(Dispatchers.IO) {
                    dbConnector.rightErrorDB.getErrorList(
                        limit = limit,
                        offset = offset,
                        filters = searchFilters,
                        testId = testId,
                    )
                }
            val totalNumber =
                async(Dispatchers.IO) {
                    dbConnector.rightErrorDB.getCount(filters = searchFilters, testId = testId)
                }

            val occurrenceConflictTypes =
                async(Dispatchers.IO) {
                    dbConnector.rightErrorDB
                        .getOccurrences(
                            column = RightErrorDB.COLUMN_CONFLICTING_TYPE,
                            filters = searchFilters,
                            testId = testId,
                        ).map { ConflictType.valueOf(it) }
                }

            val occurrenceContextNames =
                async(Dispatchers.IO) {
                    dbConnector.rightErrorDB.getOccurrences(
                        column = RightErrorDB.COLUMN_CONFLICT_BY_CONTEXT,
                        filters = searchFilters,
                        testId = testId,
                    )
                }
            return@coroutineScope ErrorQueryResult(
                totalNumberOfResults = totalNumber.await(),
                contextNames =
                    occurrenceContextNames
                        .await()
                        .toSet()
                        .toList()
                        .sorted(),
                conflictTypes = occurrenceConflictTypes.await().toSet(),
                results = results.await(),
            )
        }

    suspend fun createRelationshipsByRight(right: ItemRight) {
        if (right.rightId == null) {
            return
        }
        if (right.predecessorId != null) {
            addRelationship(
                relationship =
                    RelationshipRest(
                        relationship = RelationshipRest.Relationship.predecessor,
                        sourceRightId = right.rightId,
                        targetRightId = right.predecessorId,
                    ),
            )
        }
        if (right.successorId != null) {
            addRelationship(
                relationship =
                    RelationshipRest(
                        relationship = RelationshipRest.Relationship.successor,
                        sourceRightId = right.rightId,
                        targetRightId = right.successorId,
                    ),
            )
        }
    }

    suspend fun addRelationship(relationship: RelationshipRest) {
        val rights =
            dbConnector.rightDB.getRightsByIds(
                listOf(
                    relationship.sourceRightId,
                    relationship.targetRightId,
                ),
            )

        if (rights.size != 2) {
            // If either of the given Right-IDs does not exist, do nothing.
            return
        }
        if (relationship.relationship == RelationshipRest.Relationship.predecessor) {
            dbConnector.rightDB.setPredecessor(
                sourceRightId = relationship.sourceRightId,
                targetRightId = relationship.targetRightId,
            )
            dbConnector.rightDB.setSuccessor(
                sourceRightId = relationship.targetRightId,
                targetRightId = relationship.sourceRightId,
            )
        }

        if (relationship.relationship == RelationshipRest.Relationship.successor) {
            dbConnector.rightDB.setSuccessor(
                sourceRightId = relationship.sourceRightId,
                targetRightId = relationship.targetRightId,
            )
            dbConnector.rightDB.setPredecessor(
                sourceRightId = relationship.targetRightId,
                targetRightId = relationship.sourceRightId,
            )
        }
    }

    suspend fun insertExportJob(exportJob: ExportJob): UUID = UUID.fromString(dbConnector.exportJobDB.insertJob(exportJob))

    suspend fun insertGenericJob(genericJob: GenericJob): UUID = UUID.fromString(dbConnector.genericJobDB.insertJob(genericJob))

    suspend fun getExportJobById(jobId: UUID): ExportJob? = dbConnector.exportJobDB.getJobById(jobId)

    suspend fun updateExportJobById(exportJob: ExportJob): Int =
        dbConnector.exportJobDB.updateJobStatusById(
            exportJob,
        )

    suspend fun updateGenericJobById(genericJob: GenericJob): Int =
        dbConnector.genericJobDB.updateJobStatusById(
            genericJob,
        )

    suspend fun cleanDownloads(instant: Instant): Int {
        val jobs: List<ExportJob> = dbConnector.exportJobDB.getJobsOlderThan(instant)
        jobs.forEach { job ->
            job.getFile()?.delete()
        }
        return dbConnector.exportJobDB.deleteJobsByIds(jobs.map { it.id })
    }

    suspend fun findMissingJobs(): List<JobKind> {
        val jobKinds =
            dbConnector.genericJobDB
                .getSuccessfulJobsSince(
                    Instant.now().minus(Duration.ofDays(1)),
                ).map { it.kind }
        return JobKind.entries.filterNot { it in jobKinds }
    }

    companion object {
        val FALLBACK_DATE: LocalDate = LocalDate.of(2000, 1, 1)

        // Default chunk size when iterating over a huge dataset
        const val DEFAULT_CHUNK_SIZE: Int = 5000

        /**
         * Valid patterns: key:value or key:'value1 value2 ...'.
         * Valid special characters: '-:;'
         */
        val SEARCH_KEY_REGEX =
            Regex(
                """\w+:[^\s"'()]+|\w+:'[^']*'|\w+:"[^"]*"""",
            )

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
            } else if (r2.startDate.toString() == r1.endDate.toString() || r1.startDate.toString() == r2.endDate.toString()) {
                true
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

        fun sumUpIndividualMapEntries(given: Map<List<String>, Int>): Map<String, Int> {
            val parts = given.entries.partition { it.key.size == 1 }
            val uniqueKeys: Map<String, Int> = parts.first.associate { it.key.first() to it.value }
            return parts.second
                .fold(uniqueKeys.toMutableMap()) { acc: MutableMap<String, Int>, current: Map.Entry<List<String>, Int> ->
                    current.key.forEach {
                        acc.merge(it, current.value) { oldValue, newValue -> oldValue + newValue }
                    }
                    acc
                }.toMap()
        }

        /**
         * Right information whose end date lies before the given date will be discarded.
         * If only the start date lies before the date, then the start date will be set to the date.
         *
         * Templates may have ranges which end and/or start before the lifetime of the metadata it has
         * been applied to. Therefore, the start date for this metadata will be adjusted.
         */
        fun filterAndAdjustRightsByDate(
            templatesAndRights: List<ItemRight>,
            firstApplicationDate: LocalDate,
        ): List<ItemRight> {
            val (templates, rights) = templatesAndRights.partition { it.isTemplate }
            val fs =
                templates
                    .filter { right -> right.endDate == null || right.endDate >= firstApplicationDate }
            val templatesAndRightsCorrectStart =
                (fs + rights).map { right ->
                    if (firstApplicationDate > right.startDate) {
                        right.copy(
                            startDate = firstApplicationDate,
                        )
                    } else {
                        right
                    }
                }
            return templatesAndRightsCorrectStart
        }

        /**
         * Remove duplicates. Keep those with - signs
         */
        fun filterISBNs(isbns: List<String>?): Set<String>? {
            if (isbns == null) {
                return null
            }
            val ret: MutableSet<String> = isbns.toMutableSet()
            isbns.forEach { isbn ->
                if (isbn.contains('-')) {
                    ret.remove(isbn.filter { it != '-' })
                }
            }
            return ret.toSet()
        }
    }
}
