package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.Either
import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Testing if Metadata will be marked as deleted depending on last_updated_on column.
 *
 * Created on 20-01-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class MetadataDeleteTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk {
                every { url } returns "my_url"
            },
        )

    private val templateApplication =
        TemplateApplication(
            dbConnector = backend.dbConnector,
            backend = backend,
        )

    @AfterClass
    fun afterTests() {
        runBlocking {
            backend.dbConnector.cleanAllTables()
        }
        unmockkAll()
    }

    @Test
    fun testUpdateMetadataAsDeleted() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.minusDays(14L).toInstant()
            val deletedMetadata =
                TEST_Metadata.copy(
                    handle = "11159/7080",
                    deleted = false,
                )
            backend.insertMetadataElement(deletedMetadata)
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            val upToDateMetadata =
                TEST_Metadata.copy(
                    handle = "11159/7081",
                    deleted = false,
                )
            backend.insertMetadataElement(upToDateMetadata)

            // When
            val markedAsDeleted = backend.updateMetadataAsDeleted(NOW.toInstant())

            // Then
            assertThat(
                markedAsDeleted,
                `is`(2),
            )

            val receivedMetadata = backend.getMetadataElementsByIds(listOf(deletedMetadata.handle, upToDateMetadata.handle))

            // Delete every entry to clean up table
            backend.dbConnector.metadataDB.deleteMetadata(
                listOf(
                    deletedMetadata.handle,
                    upToDateMetadata.handle,
                ),
            )
            assertThat(
                receivedMetadata.toSet(),
                `is`(
                    setOf(
                        upToDateMetadata.copy(createdOn = NOW, lastUpdatedOn = NOW),
                        deletedMetadata.copy(deleted = true, createdOn = NOW.minusDays(14L), lastUpdatedOn = NOW.minusDays(14L)),
                    ),
                ),
            )
        }

    @Test
    fun testAllDeletionCases() =
        runBlocking {
            mockkStatic(Instant::class)
            mockkStatic(LocalDate::class)
            every { LocalDate.now(any<ZoneId>()) } returns LocalDate.of(2025, 12, 17)
            every { Instant.now() } returns TEMPLATE_APPLICATION_DATE.toInstant()

            val newlyDeletedMetadata =
                TEST_Metadata.copy(
                    handle = "11159/7086",
                    deleted = false,
                    zdbIds = listOf(ZDB_1),
                    paketSigel = listOf(SIGEL_1),
                )

            backend.insertMetadataElement(newlyDeletedMetadata)

            // Add manual right entries
            val rights =
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2026, 1, 10),
                        endDate = LocalDate.of(2026, 1, 31),
                    ),
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 12, 31),
                    ),
                )
            rights.forEach { right ->
                val r = backend.insertRight(right)
                when (val ret = backend.insertItemEntry(newlyDeletedMetadata.handle, r)) {
                    is Either.Left -> {
                        error("Error on inserting a right information: ${ret.value}")
                    }

                    is Either.Right<*> -> {}
                }
            }

            // Create bookmarks
            val bookmarkIdZDB =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "bookmark_zdb",
                        bookmarkId = 0,
                        zdbIdFilters =
                            listOf(
                                ZDBIdFilter(
                                    zdbId = ZDB_1,
                                ),
                            ),
                        lastUpdatedOn =
                            OffsetDateTime.of(
                                2022,
                                3,
                                2,
                                1,
                                1,
                                0,
                                0,
                                ZoneOffset.UTC,
                            ),
                        lastUpdatedBy = "user2",
                        createdBy = "user1",
                        createdOn =
                            OffsetDateTime.of(
                                2022,
                                3,
                                2,
                                1,
                                1,
                                0,
                                0,
                                ZoneOffset.UTC,
                            ),
                    ),
                )

            val bookmarkIdSigel =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "bookmark_sigel",
                        bookmarkId = 0,
                        paketSigelFilters =
                            listOf(
                                PaketSigelFilter(
                                    paketSigel = SIGEL_1,
                                ),
                            ),
                        lastUpdatedOn =
                            OffsetDateTime.of(
                                2022,
                                3,
                                2,
                                1,
                                1,
                                0,
                                0,
                                ZoneOffset.UTC,
                            ),
                        lastUpdatedBy = "user2",
                        createdBy = "user1",
                        createdOn =
                            OffsetDateTime.of(
                                2022,
                                3,
                                2,
                                1,
                                1,
                                0,
                                0,
                                ZoneOffset.UTC,
                            ),
                    ),
                )

            // Create Template
            val templateIdZDB =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "zdbTemplate",
                        isTemplate = true,
                        startDate = LocalDate.of(2025, 12, 31),
                        endDate = LocalDate.of(2026, 1, 9),
                    ),
                )
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdZDB,
                rightId = templateIdZDB,
            )

            val templateIdSigel =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "sigelTemplate",
                        isTemplate = true,
                        startDate = LocalDate.of(2025, 12, 18),
                        endDate = LocalDate.of(2025, 12, 30),
                    ),
                )

            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdSigel,
                rightId = templateIdSigel,
            )

            // Apply templates
            val receivedTemplateZDB =
                templateApplication.applyTemplate(
                    templateIdZDB,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                receivedTemplateZDB.appliedMetadataHandles,
                `is`(listOf(newlyDeletedMetadata.handle)),
            )

            val receivedTemplateSigel =
                templateApplication.applyTemplate(
                    templateIdSigel,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                receivedTemplateSigel.appliedMetadataHandles,
                `is`(listOf(newlyDeletedMetadata.handle)),
            )

            val result = backend.getItemList(1, 0)
            assertThat(
                result.first().rights.size,
                `is`(4),
            )

            // when
            every { LocalDate.now(any<ZoneId>()) } returns LocalDate.of(2026, 1, 1)
            every { Instant.now() } returns DELETION_DATE.toInstant()

            val markedAsDeleted: Int = backend.updateMetadataAsDeleted(DELETION_DATE.toInstant())
            assertThat(
                markedAsDeleted,
                `is`(1),
            )
            val resultAfterDeletion = backend.getItemList(1, 0)
            assertThat(
                resultAfterDeletion.first().rights.size,
                `is`(3),
            )

            assertTrue(
                resultAfterDeletion.first().rights.all { !it.isTemplate },
            )

            val times =
                resultAfterDeletion.first().rights.sortedBy { it.startDate }.map {
                    Pair(it.startDate, it.endDate)
                }
            assertThat(
                times,
                `is`(
                    listOf(
                        Pair(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                        Pair(LocalDate.of(2025, 12, 18), LocalDate.of(2025, 12, 30)),
                        Pair(LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 31)),
                    ),
                ),
            )
        }

    companion object {
        val NOW: OffsetDateTime =
            OffsetDateTime.of(
                2026,
                1,
                9,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!

        val TEMPLATE_APPLICATION_DATE: OffsetDateTime =
            OffsetDateTime.of(
                2025,
                12,
                17,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!

        val DELETION_DATE: OffsetDateTime =
            OffsetDateTime.of(
                2026,
                1,
                1,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!

        const val ZDB_1 = "zdb1"
        const val SIGEL_1 = "sigel1"
        val TODAY: LocalDate = LocalDate.of(2026, 1, 9)
    }
}
