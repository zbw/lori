package de.zbw.api.lori.server.type.rest

import de.zbw.api.lori.server.type.DACollection
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.DAItem
import de.zbw.api.lori.server.type.MetadataValidationError
import de.zbw.api.lori.server.type.RestConverter
import de.zbw.api.lori.server.type.RestConverter.extractMetadata
import de.zbw.api.lori.server.utils.RestConverterUtil.prepareLicenceUrlFilter
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.OffsetDateTime

/**
 * Converter utilities for DAItem to Business Logic conversion.
 *
 * Created on 04-09-2026.
 * @author AI Assistant
 */
object DAItemConverter {
    /**
     * Converts a DAItem to ItemMetadata.
     *
     * @param daItem The DAItem to convert
     * @param daCommunity The associated DACommunity
     * @param daCollection The associated DACollection
     * @param validationErrorMap Map to collect validation errors
     * @param mutexForLogging Mutex for thread-safe logging
     * @return ItemMetadata if successful, null otherwise
     */
    suspend fun toBusiness(
        daItem: DAItem,
        daCommunity: DACommunity,
        daCollection: DACollection,
        validationErrorMap: MutableMap<MetadataValidationError, List<String>>,
        mutexForLogging: Mutex,
    ): ItemMetadata? {
        val metadata = daItem.metadata
        val handle =
            extractMetadata(KEY_HANDLE, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_HANDLES,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val publicationType =
            try {
                extractMetadata(KEY_PUBLICATION_TYPE, metadata)?.let {
                    if (it.size > 1) {
                        logError(
                            daItem.handle,
                            MetadataValidationError.MULTIPLE_PUBLICATION_TYPES,
                            mutexForLogging,
                            validationErrorMap,
                        )
                    }
                    PublicationType.valueOf(
                        it[0]
                            .uppercase()
                            .replace(oldChar = ' ', newChar = '_')
                            .replace("PROCEEDINGS", "PROCEEDING"),
                    )
                }
            } catch (iae: IllegalArgumentException) {
                mutexForLogging.withLock {
                    logError(
                        daItem.handle,
                        MetadataValidationError.UNKNOWN_PUBLICATION_TYPE,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                throw iae
            }

        val publicationYear: Int? =
            extractMetadata(KEY_PUBLICATION_YEAR, metadata)
                ?.let {
                    if (it.size > 1) {
                        logError(
                            daItem.handle,
                            MetadataValidationError.MULTIPLE_PUBLICATION_YEARS,
                            mutexForLogging,
                            validationErrorMap,
                        )
                    }
                    it[0]
                }?.let {
                    val publicationDate = RestConverter.parseToDate(it)
                    if (publicationDate == null) {
                        logError(
                            daItem.handle,
                            MetadataValidationError.INVALID_ISSUED,
                            mutexForLogging,
                            validationErrorMap,
                        )
                        null
                    } else {
                        publicationDate.year
                    }
                } ?: let {
                logError(
                    daItem.handle,
                    MetadataValidationError.MISSING_DATE_ISSUED_FIELD,
                    mutexForLogging,
                    validationErrorMap,
                )
                null
            }

        val title =
            extractMetadata(KEY_TITLE, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_TITLES,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val ppn =
            extractMetadata(KEY_PPN, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_PPNS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val ppnBook =
            extractMetadata(KEY_PPN_BOOK, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_BOOK_PPNS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val ppnJournal =
            extractMetadata(KEY_PPN_JOURNAL, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_JOURNAL_PPNS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val ppnSeries =
            extractMetadata(KEY_PPN_SERIES, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_SERIES_PPNS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val partOfJournal =
            extractMetadata(KEY_IS_PART_OF_JOURNAL, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_JOURNALS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val partOfBook =
            extractMetadata(KEY_IS_PART_OF_BOOK, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_BOOKS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val econstorIssue =
            extractMetadata(KEY_ECONSTOR_ISSUE, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_ECONSTOR_ISSUE,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val econstorVolume =
            extractMetadata(KEY_ECONSTOR_VOLUME, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_ECONSTOR_VOLUME,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        val econbizId =
            extractMetadata(KEY_ECONBIZID, metadata)?.let {
                if (it.size > 1) {
                    logError(
                        daItem.handle,
                        MetadataValidationError.MULTIPLE_ECONBIZIDS,
                        mutexForLogging,
                        validationErrorMap,
                    )
                }
                it[0]
            }

        if (econbizId == null && ppn == null) {
            logError(
                daItem.handle,
                MetadataValidationError.MISSING_ECONBIZID_AND_PPN,
                mutexForLogging,
                validationErrorMap,
            )
        }

        return if (
            handle == null ||
            publicationType == null ||
            title == null
        ) {
            logError(
                daItem.handle,
                MetadataValidationError.MISSING_REQUIRED_FIELD,
                mutexForLogging,
                validationErrorMap,
            )
            null
        } else {
            val subDACommunity: DACommunity? = daCommunity.subcommunities?.firstOrNull()
            val licenceUrl =
                extractMetadata(KEY_LICENSE, metadata)?.let {
                    if (it.size > 1) {
                        logError(
                            daItem.handle,
                            MetadataValidationError.MULTIPLE_LICENCEURLS,
                            mutexForLogging,
                            validationErrorMap,
                        )
                    }
                    it[0]
                }

            val isbns =
                extractMetadata(KEY_ISBN, metadata)
                    ?.foldRight(emptyList<String>()) { isbn, acc ->
                        if (isbn.contains("-")) {
                            acc + isbn + isbn.filter { it != '-' }
                        } else {
                            acc + isbn
                        }
                    }

            ItemMetadata(
                collectionHandle =
                    daCollection.handle?.let {
                        RestConverter.parseHandle(it)
                    },
                collectionName = daCollection.name,
                communityHandle =
                    daCommunity.handle?.let {
                        RestConverter.parseHandle(it)
                    },
                communityName = daCommunity.name,
                createdBy = null,
                createdOn = null,
                deleted = daItem.withdrawn?.toBoolean() == true,
                pids =
                    extractMetadata(KEY_PID, metadata),
                econbizId = econbizId,
                econstorIssue = econstorIssue,
                econstorVolume = econstorVolume,
                handle = RestConverter.parseHandle(handle),
                isbn = isbns,
                issn = extractMetadata(KEY_ISSN, metadata),
                isPartOfSeries =
                    extractMetadata(KEY_IS_PART_OF_SERIES, metadata),
                isPartOfBook = partOfBook,
                isPartOfJournal = partOfJournal,
                lastUpdatedBy = null,
                lastUpdatedOn = null,
                licenceUrl = licenceUrl,
                licenceUrlFilter = prepareLicenceUrlFilter(licenceUrl),
                paketSigel = extractMetadata(KEY_PAKET_SIGEL, metadata),
                ppn = ppn,
                ppnBook = ppnBook,
                ppnJournal = ppnJournal,
                ppnSeries = ppnSeries,
                publicationType = publicationType,
                publicationYear = publicationYear,
                subCommunityHandle =
                    subDACommunity?.handle?.let {
                        RestConverter.parseHandle(it)
                    },
                subCommunityName = subDACommunity?.name,
                storageDate =
                    extractMetadata(KEY_STORAGE_DATE, metadata)
                        ?.let {
                            if (it.size > 1) {
                                mutexForLogging.withLock {
                                    validationErrorMap.merge(
                                        MetadataValidationError.MULTIPLE_STORAGE_DATES,
                                        listOf(daItem.handle ?: "Unknown handle"),
                                    ) { oldValue, newValue ->
                                        oldValue + newValue
                                    }
                                }
                            }
                            OffsetDateTime.parse(it[0])
                        },
                title = title,
                zdbIds =
                    listOfNotNull(
                        extractMetadata(KEY_ZDB_ID_JOURNAL, metadata),
                        extractMetadata(KEY_ZDB_ID_SERIES, metadata),
                    ).flatten(),
            )
        }
    }

    suspend fun logError(
        handle: String?,
        validationError: MetadataValidationError,
        mutexForLogging: Mutex,
        validationErrorMap: MutableMap<MetadataValidationError, List<String>>,
    ) {
        mutexForLogging.withLock {
            validationErrorMap.merge(
                validationError,
                listOf(handle ?: "Unknown handle"),
            ) { oldValue, newValue ->
                oldValue + newValue
            }
        }
    }

    const val KEY_HANDLE = "dc.identifier.uri"
    const val KEY_ECONBIZID = "dc.identifier.econbizid"
    const val KEY_ECONSTOR_ISSUE = "econstor.citation.issue"
    const val KEY_ECONSTOR_VOLUME = "econstor.citation.volume"
    const val KEY_IS_PART_OF_BOOK = "dc.relation.ispartofbook"
    const val KEY_IS_PART_OF_JOURNAL = "dc.relation.ispartofjournal"
    const val KEY_IS_PART_OF_SERIES = "dc.relation.ispartofseries"
    const val KEY_ISBN = "dc.identifier.isbn"
    const val KEY_ISSN = "dc.relation.issn"
    const val KEY_LICENSE = "dc.rights.license"
    const val KEY_PPN = "dc.identifier.ppn"
    const val KEY_PPN_BOOK = "dc.relation.bookppn"
    const val KEY_PPN_JOURNAL = "dc.relation.journalppn"
    const val KEY_PPN_SERIES = "dc.relation.seriesppn"
    const val KEY_PID = "dc.identifier.pi"
    const val KEY_PUBLICATION_TYPE = "dc.type"
    const val KEY_PUBLICATION_YEAR = "dc.date.issued"
    const val KEY_PAKET_SIGEL = "dc.identifier.packageid"
    const val KEY_STORAGE_DATE = "dc.date.accessioned"
    const val KEY_TITLE = "dc.title"
    const val KEY_ZDB_ID_JOURNAL = "dc.relation.journalzdbid"
    const val KEY_ZDB_ID_SERIES = "dc.relation.serieszdbid"
}
