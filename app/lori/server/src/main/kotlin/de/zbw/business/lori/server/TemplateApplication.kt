package de.zbw.business.lori.server

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import de.zbw.api.lori.server.utils.Constants
import de.zbw.business.lori.server.LoriServerBackend.Companion.findItemsWithConflicts
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ComparisonOperator
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.ItemRow
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchGrammar
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.SortInformation
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.fold
import kotlin.math.ceil

class TemplateApplication(
    internal val dbConnector: DatabaseConnector,
    internal val backend: LoriServerBackend,
) {
    internal suspend fun applyTemplate(
        rightId: String,
        skipTemplateDrafts: Boolean,
        dryRun: Boolean,
        createdBy: String,
    ): TemplateApplicationResult {
        LOG.info("Start applying Template $rightId")
        // Get Right object
        val right: ItemRight =
            dbConnector.rightDB.getRightsByIds(listOf(rightId)).firstOrNull()
                ?: return TemplateApplicationResult(
                    rightId = rightId,
                    templateName = "",
                    testId = null,
                    appliedMetadataHandles = emptyList(),
                    errors = emptyList(),
                    numberOfErrors = 0,
                    exceptionTemplateApplicationResult = null,
                    skippedApplication = true,
                )
        if (skipTemplateDrafts && right.lastAppliedOn == null) {
            // Draft will be skipped for now.
            return TemplateApplicationResult(
                rightId = rightId,
                templateName = right.templateName ?: "",
                testId = null,
                appliedMetadataHandles = emptyList(),
                errors = emptyList(),
                numberOfErrors = 0,
                exceptionTemplateApplicationResult = null,
                skippedApplication = true,
            )
        }
        if (right.endDate != null && right.endDate < LocalDate.now(TimezoneUtil.TIME_ZONE_BERLIN)) {
            LOG.info("Template ${right.rightId}: Not applied due to end date lying in the past.")
            return TemplateApplicationResult(
                rightId = rightId,
                templateName = right.templateName ?: "",
                testId = null,
                appliedMetadataHandles = emptyList(),
                errors = emptyList(),
                numberOfErrors = 0,
                exceptionTemplateApplicationResult = null,
                skippedApplication = true,
            )
        }
        // Exceptions
        val exceptionTemplate: ItemRight? = dbConnector.rightDB.getExceptionByRightId(rightId)
        val exceptionTemplateApplicationResult: TemplateApplicationResult? =
            exceptionTemplate?.let { excTemp ->
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
            dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightIds(
                exceptionTemplate?.let { listOf(it.rightId) }?.filterNotNull() ?: emptyList(),
            )
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
                        sortInformation = SortInformation.DEFAULT,
                    )
                }.toSet()

        // Receive all bookmark ids
        val bookmarkIds: List<Int> = dbConnector.bookmarkTemplateDB.getBookmarkIdsByRightId(rightId)
        val bookmarks: List<Bookmark> = dbConnector.bookmarkDB.getBookmarksByIds(bookmarkIds)

        val testId =
            if (dryRun) {
                UUID.randomUUID().toString()
            } else {
                null
            }
        val beforeApplicationTimestamp = Instant.now()
        val results: List<TemplateApplicationResult> =
            bookmarks.map { bookmark ->
                applyTemplateByBookmark(
                    bookmark = bookmark,
                    createdBy = createdBy,
                    dryRun = dryRun,
                    right = right,
                    searchResultsExceptionIds = searchResultsExceptions,
                    testId = testId,
                )
            }
        // Check if some entries were not reapplied
        replaceOutOfDateApplications(template = right, atLeastLastUpdatedOn = beforeApplicationTimestamp)

        return results.fold(
            initial =
                TemplateApplicationResult(
                    rightId = right.rightId!!,
                    templateName = right.templateName ?: "",
                    testId = testId,
                    appliedMetadataHandles = emptyList(),
                    errors = emptyList(),
                    numberOfErrors = 0,
                    exceptionTemplateApplicationResult = exceptionTemplateApplicationResult,
                ),
        ) { r, acc ->
            acc.mAppend(r)
        }
    }

    suspend fun applyTemplateByBookmark(
        bookmark: Bookmark,
        createdBy: String,
        dryRun: Boolean,
        right: ItemRight,
        searchResultsExceptionIds: Set<String>,
        testId: String?,
    ): TemplateApplicationResult =
        coroutineScope {
            val endDateTemplateAfterCreatedOnFilter: CreatedOnFilter? =
                right.endDate?.let { end ->
                    CreatedOnFilter(
                        comparisonOp = ComparisonOperator.LESS_OR_EQUAL,
                        createdOn =
                            end
                                .atStartOfDay(
                                    TimezoneUtil.TIME_ZONE_UTC,
                                ).toInstant(),
                    )
                }
            val ignoreDeletedItemsFilter = DeletionsFilter(on = false)

            val facetsResult: SearchQueryResult =
                backend.searchQuery(
                    facetsOnly = true, // Only receive facets
                    searchTerm = bookmark.searchTerm,
                    limit = null,
                    offset = null,
                    metadataSearchFilter =
                        (
                            bookmark.getAllMetadataFilter() + endDateTemplateAfterCreatedOnFilter + ignoreDeletedItemsFilter
                        ).filterNotNull(),
                    rightSearchFilter = bookmark.getAllRightFilter(),
                    noRightInformationFilter = bookmark.noRightInformationFilter,
                    handlesToIgnore = searchResultsExceptionIds.toList(),
                    sortInformation = SortInformation.DEFAULT,
                )

            if (!dryRun) {
                // Update last_applied_on field
                dbConnector.rightDB.updateAppliedOnByTemplateId(right.rightId!!)
            }

            val deferredResults = mutableListOf<Deferred<TemplateApplicationResult>>()
            for (offset in 0..<ceil(facetsResult.numberOfResults.toDouble() / LIMIT).toInt()) {
                deferredResults +=
                    async {
                        semaphore.withPermit {
                            LOG.info("Template ${right.rightId}: Apply entries ${offset * LIMIT} to ${offset * LIMIT + LIMIT}")
                            applyTemplateByBookmarkAndOffset(
                                bookmark = bookmark,
                                offset = offset * LIMIT,
                                dryRun = dryRun,
                                searchResultsExceptionIds = searchResultsExceptionIds,
                                right = right,
                                createdBy = createdBy,
                                testId = testId,
                                additionalMetadataSearchFilters =
                                    listOfNotNull(
                                        endDateTemplateAfterCreatedOnFilter,
                                        ignoreDeletedItemsFilter,
                                    ),
                            )
                        }
                    }
            }
            deferredResults
                .awaitAll()
                .fold(
                    initial =
                        TemplateApplicationResult(
                            rightId = right.rightId!!,
                            templateName = right.templateName ?: "",
                            testId = null,
                            appliedMetadataHandles = emptyList(),
                            errors = emptyList(),
                            numberOfErrors = 0,
                            exceptionTemplateApplicationResult = null,
                        ),
                ) { r, acc ->
                    acc.mAppend(r)
                }.also {
                    LOG.info("Finished application for bookmark ${bookmark.bookmarkId} on Template ${right.rightId}")
                }
        }

    suspend fun applyTemplateByBookmarkAndOffset(
        right: ItemRight,
        bookmark: Bookmark,
        offset: Int,
        dryRun: Boolean,
        searchResultsExceptionIds: Set<String>,
        createdBy: String,
        testId: String?,
        additionalMetadataSearchFilters: List<MetadataSearchFilter>,
    ): TemplateApplicationResult {
        val searchResults: Set<Item> =
            backend
                .searchQuery(
                    searchTerm = bookmark.searchTerm,
                    limit = LIMIT,
                    offset = offset,
                    metadataSearchFilter =
                        (bookmark.getAllMetadataFilter() + additionalMetadataSearchFilters),
                    rightSearchFilter = bookmark.getAllRightFilter(),
                    noRightInformationFilter = bookmark.noRightInformationFilter,
                    handlesToIgnore = searchResultsExceptionIds.toList(),
                    sortInformation = SortInformation.DEFAULT,
                ).results
                .toSet()

        val rightId = right.rightId!!
        if (!dryRun) {
            // Connect Template to all results
            val itemsWithConflicts: Map<Item, List<RightError>> =
                findItemsWithConflicts(searchResults, right, null, createdBy)
            val searchResultsWithoutConflict: Set<Item> = searchResults.subtract(itemsWithConflicts.keys)
            dbConnector.rightErrorDB.deleteByCausingRightId(rightId)
            dbConnector.rightErrorDB.insertErrorsBatch(itemsWithConflicts.values.flatten())
            dbConnector.itemDB.upsertItemBatch(
                createdBy = createdBy,
                itemIds =
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
                exceptionTemplateApplicationResult = null,
                templateName = right.templateName ?: "Missing Template Name",
                testId = null,
                numberOfErrors = itemsWithConflicts.values.flatten().size,
            )
        } else {
            val itemsWithConflicts: Map<Item, List<RightError>> =
                findItemsWithConflicts(searchResults, right, testId, createdBy)
            val searchResultsWithoutConflict: Set<Item> = searchResults.subtract(itemsWithConflicts.keys)
            dbConnector.rightErrorDB.insertErrorsBatch(itemsWithConflicts.values.flatten())
            return TemplateApplicationResult(
                rightId = rightId,
                // TODO(CB): Don't send back thousands of errors for now
                errors = emptyList(),
                appliedMetadataHandles = searchResultsWithoutConflict.map { it.metadata.handle },
                exceptionTemplateApplicationResult = null,
                templateName = right.templateName ?: "Missing Template Name",
                testId = testId,
                numberOfErrors = itemsWithConflicts.values.flatten().size,
            )
        }
    }

    suspend fun replaceOutOfDateApplications(
        template: ItemRight,
        atLeastLastUpdatedOn: Instant,
    ) {
        val itemRows =
            dbConnector.itemDB
                .getItemsByLastUpdatedBeforeAndRightId(template.rightId!!, atLeastLastUpdatedOn)
        val deletedHandles = dbConnector.metadataDB.getDeletedMetadataByHandles(itemRows.map { it.handle }).toSet()

        itemRows.forEach { itemRow: ItemRow ->
            if (itemRow.handle in deletedHandles) return@forEach
            // Get the start date which is displayed for this handle in the UI because
            // it might differ from the start date of the template
            val startDateDisplayed =
                LoriServerBackend
                    .filterAndAdjustTemplateDates(
                        templatesAndRights = listOf(template),
                        firstApplicationDate = TimezoneUtil.utcOffsetDateTimeToBerlinDate(itemRow.createdOn!!),
                    ).firstOrNull()
                    ?.startDate
            dbConnector.itemDB.deleteItem(itemRow.handle, itemRow.rightId)
            if (startDateDisplayed != null) {
                val localDateLastImport =
                    TimezoneUtil.utcOffsetDateTimeToBerlinDate(
                        OffsetDateTime.ofInstant(
                            itemRow.lastUpdatedOn!!.toInstant(),
                            TimezoneUtil.TIME_ZONE_BERLIN,
                        ),
                    )
                val localDateNow = LocalDate.now(TimezoneUtil.TIME_ZONE_BERLIN)
                val newEndDate =
                    if (localDateNow.minusDays(1L) > localDateLastImport) {
                        TimezoneUtil.utcOffsetDateTimeToBerlinDate(
                            OffsetDateTime.ofInstant(
                                Instant.now(),
                                TimezoneUtil.TIME_ZONE_BERLIN,
                            ),
                        )
                    } else {
                        TimezoneUtil.utcOffsetDateTimeToBerlinDate(itemRow.lastUpdatedOn)
                    }
                val oldNotesManagementRelated = template.notesManagementRelated?.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: ""
                val newManualRight =
                    template.copy(
                        startDate = startDateDisplayed,
                        isTemplate = false,
                        templateName = null,
                        templateDescription = null,
                        notesManagementRelated =
                            oldNotesManagementRelated +
                                "Automatisch erzeugt, um Rechteinformationen aus in der Vergangenheit existierender Template-Zuordnung" +
                                " zu Template https://${backend.config.url}?templateId=${template.rightId}" +
                                " abzubilden",
                        endDate = newEndDate,
                        createdBy = Constants.AUTHOR_AUTOMATIC,
                        lastUpdatedBy = Constants.AUTHOR_AUTOMATIC,
                    )
                LOG.info("Replacing template entry ${template.rightId} of item ${itemRow.handle} with a manual right")
                val newManualRightId = dbConnector.rightDB.insertRight(newManualRight)
                dbConnector.itemDB.insertItem(
                    itemId = ItemId(handle = itemRow.handle, rightId = newManualRightId),
                    createdBy = "lori",
                )
            }
        }
    }

    companion object {
        const val LIMIT = 500
        private const val MAX_PARALLEL_CONNECTIONS = 5
        private val semaphore = Semaphore(MAX_PARALLEL_CONNECTIONS)
        internal val LOG: Logger = LogManager.getLogger(TemplateApplication::class.java)
    }
}
