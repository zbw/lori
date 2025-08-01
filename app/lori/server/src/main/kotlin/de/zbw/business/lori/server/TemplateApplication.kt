package de.zbw.business.lori.server

import com.github.h0tk3y.betterParse.grammar.tryParseToEnd
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.Parsed
import de.zbw.business.lori.server.LoriServerBackend.Companion.findItemsWithConflicts
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.ParsingException
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.type.SearchGrammar
import de.zbw.business.lori.server.type.SortInformation
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.persistence.lori.server.DatabaseConnector
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
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
    ): TemplateApplicationResult? {
        LOG.info("Start applying Template $rightId")
        // Get Right object
        val right: ItemRight =
            dbConnector.rightDB.getRightsByIds(listOf(rightId)).firstOrNull() ?: return null
        if (skipTemplateDrafts && right.lastAppliedOn == null) {
            // Draft will be skipped for now.
            return null
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
        return results.fold(
            initial =
                TemplateApplicationResult(
                    rightId = right.rightId!!,
                    templateName = right.templateName ?: "",
                    testId = null,
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
            val facetsResult =
                backend.searchQuery(
                    searchTerm = bookmark.searchTerm,
                    limit = null,
                    offset = null,
                    metadataSearchFilter = bookmark.getAllMetadataFilter(),
                    rightSearchFilter = bookmark.getAllRightFilter(),
                    noRightInformationFilter = bookmark.noRightInformationFilter,
                    handlesToIgnore = searchResultsExceptionIds.toList(),
                    facetsOnly = true,
                    sortInformation = SortInformation.DEFAULT,
                )

            if (!dryRun) {
                // Update last_applied_on field
                dbConnector.rightDB.updateAppliedOnByTemplateId(right.rightId!!)
            }

            val deferredResults = mutableListOf<Deferred<TemplateApplicationResult>>()
            for (offset in 0..ceil(facetsResult.numberOfResults.toDouble() / LIMIT).toInt() - 1) {
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
    ): TemplateApplicationResult {
        val searchResults: Set<Item> =
            runBlocking {
                backend
                    .searchQuery(
                        searchTerm = bookmark.searchTerm,
                        limit = LIMIT,
                        offset = offset,
                        metadataSearchFilter = bookmark.getAllMetadataFilter(),
                        rightSearchFilter = bookmark.getAllRightFilter(),
                        noRightInformationFilter = bookmark.noRightInformationFilter,
                        handlesToIgnore = searchResultsExceptionIds.toList(),
                        sortInformation = SortInformation.DEFAULT,
                    ).results
            }.toSet()

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

    companion object {
        const val LIMIT = 500
        private const val MAX_PARALLEL_CONNECTIONS = 5
        private val semaphore = Semaphore(MAX_PARALLEL_CONNECTIONS)
        internal val LOG: Logger = LogManager.getLogger(TemplateApplication::class.java)
    }
}
