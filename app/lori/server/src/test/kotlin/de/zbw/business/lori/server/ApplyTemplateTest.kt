package de.zbw.business.lori.server

import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNull
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertTrue

/**
 * Test applying a template.
 *
 * Created on 06-12-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ApplyTemplateTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            item1ZDB1 to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2000, 1, 1),
                        endDate = LocalDate.of(2000, 12, 31),
                    ),
                ),
            item1ZDB2 to emptyList(),
            item2ZDB2 to emptyList(),
            item1ZDB3 to emptyList(),
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns ItemDBTest.NOW.toInstant()
            getInitialMetadata().forEach { entry ->
                backend.insertMetadataElement(entry.key)
                entry.value.forEach { right ->
                    val r = backend.insertRight(right)
                    backend.insertItemEntry(entry.key.handle, r)
                }
            }
        }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testApplyTemplate() =
        runBlocking {
            // Create Bookmark
            val bookmarkId =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "applyBookmark",
                        bookmarkId = 0,
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_1,
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
            val rightId = backend.insertTemplate(TEST_RIGHT.copy(templateName = "test", isTemplate = true))

            // Connect Bookmark and Template
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkId,
                rightId = rightId,
            )

            val received =
                backend.applyTemplate(
                    rightId,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                received!!.appliedMetadataHandles,
                `is`(listOf(item1ZDB1.handle)),
            )

            // Verify that new right is assigned to metadata id
            val rightIds = backend.getRightEntriesByHandle(item1ZDB1.handle).map { it.rightId }
            assertTrue(rightIds.contains(rightId))

            assertThat(
                backend.getRightById(rightId)!!.lastAppliedOn,
                `is`(ItemDBTest.NOW),
            )

            // Repeat Apply Operation without duplicate entries errors
            val received2: TemplateApplicationResult? =
                backend.applyTemplate(
                    rightId,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                received2!!.appliedMetadataHandles,
                `is`(listOf(item1ZDB1.handle)),
            )

            // Add two new items to database matching bookmark
            backend.insertMetadataElements(
                listOf(
                    item2ZDB1,
                    item3ZDB1,
                ),
            )
            // Update old item from database so it no longer matches for bookmark
            backend.upsertMetadata(listOf(item1ZDB1.copy(zdbIds = listOf("foobar"))))

            // Apply Template
            val received3: TemplateApplicationResult? =
                backend.applyTemplate(
                    rightId,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                received3!!.appliedMetadataHandles,
                `is`(
                    listOf(
                        item2ZDB1.handle,
                        item3ZDB1.handle,
                    ),
                ),
            )
            // Verify that only the new items are connected to template
            assertThat(
                backend.dbConnector.itemDB.countItemByRightId(rightId),
                `is`(2),
            )

            val applyAllReceived: List<TemplateApplicationResult> =
                backend.applyAllTemplates(
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                applyAllReceived.map { it.appliedMetadataHandles }.flatten().toSet(),
                `is`(
                    setOf(
                        item2ZDB1.handle,
                        item3ZDB1.handle,
                    ),
                ),
            )

            // Create conflicting template
            val rightIdConflict =
                backend.insertTemplate(TEST_RIGHT.copy(isTemplate = true, templateName = "conflicting"))

            // Connect Bookmark and Template
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkId,
                rightId = rightIdConflict,
            )
            val receivedConflict =
                backend.applyTemplate(
                    rightIdConflict,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                receivedConflict!!.errors.size,
                `is`(2),
            )
        }

    @Test
    fun testApplyTemplateWithException() =
        runBlocking {
            // Create Bookmarks
            val bookmarkIdUpper =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "allZDB2",
                        bookmarkId = 10,
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_2,
                                    ),
                            ),
                    ),
                )

            val bookmarkIdException =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "zdb2AndHandle",
                        bookmarkId = 20,
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_2,
                                    ),
                            ),
                        searchTerm = "hdl:bar",
                    ),
                )

            // Create Templates
            val rightIdUpper =
                backend.insertTemplate(TEST_RIGHT.copy(templateName = "upper", isTemplate = true))

            // Connect Bookmarks and Templates
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdUpper,
                rightId = rightIdUpper,
            )

            // Without exception
            val rightIdException =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "exception",
                        isTemplate = true,
                        exceptionOfId = rightIdUpper,
                    ),
                )

            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdException,
                rightId = rightIdException,
            )

            val receivedUpperWithExc =
                backend.applyTemplate(
                    rightIdUpper,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )!!
            assertThat(
                receivedUpperWithExc.appliedMetadataHandles.toSet(),
                `is`(setOf(item1ZDB2.handle)),
            )
            assertThat(
                receivedUpperWithExc.exceptionTemplateApplicationResult?.appliedMetadataHandles?.toSet(),
                `is`(setOf(item2ZDB2.handle)),
            )
            val receivedException =
                backend.applyTemplate(
                    rightIdException,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )!!
            assertThat(
                receivedException.appliedMetadataHandles,
                `is`(listOf(item2ZDB2.handle)),
            )
        }

    @Test
    fun testDontApplyDrafts() =
        runBlocking {
            val bookmarkId =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "bookmarkDraft",
                        bookmarkId = 0,
                        searchTerm = "col:'common zdb'",
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_1,
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
            val rightId =
                backend.insertTemplate(TEST_RIGHT.copy(templateName = "foobar", isTemplate = true))

            // Connect Bookmark and Template
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkId,
                rightId = rightId,
            )

            assertNull(
                backend.applyTemplate(
                    rightId,
                    skipTemplateDrafts = true,
                    dryRun = false,
                    createdBy = "user1",
                ),
            )
        }

    @Test
    fun testDryRun() =
        runBlocking {
            // Create Bookmark
            val bookmarkId =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "applyBookmarkForDryRun",
                        bookmarkId = 99,
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_3,
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
            val rightId =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "testDryRun",
                        isTemplate = true,
                        endDate = TEST_RIGHT.startDate.plusYears(1L),
                        startDate = TEST_RIGHT.startDate.plusYears(1L),
                    ),
                )

            // Connect Bookmark and Template
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkId,
                rightId = rightId,
            )

            val received =
                backend.applyTemplate(
                    rightId,
                    skipTemplateDrafts = false,
                    dryRun = true,
                    createdBy = "user1",
                )
            assertThat(
                received!!.appliedMetadataHandles,
                `is`(listOf(item1ZDB3.handle)),
            )

            // Verify that no right is assigned to metadata id
            val rightIds = backend.getRightEntriesByHandle(item1ZDB3.handle).map { it.rightId }
            assertFalse(rightIds.contains(rightId))

            assertNull(
                backend.getRightById(rightId)!!.lastAppliedOn,
            )
        }

    companion object {
        const val ZDB_1 = "zdb1"
        const val ZDB_2 = "zdb2"
        const val ZDB_3 = "zdb3"
        val TEST_RIGHT = RightFilterTest.TEST_RIGHT
        val item1ZDB1 =
            TEST_METADATA.copy(
                handle = "item1_zdb1",
                collectionName = "common zdb",
                zdbIds = listOf(ZDB_1),
                publicationYear = 2010,
                publicationType = PublicationType.BOOK,
            )
        val item2ZDB1 =
            TEST_METADATA.copy(
                handle = "item2_zdb2",
                collectionName = "common zdb",
                zdbIds = listOf(ZDB_1),
                publicationYear = 2010,
                publicationType = PublicationType.BOOK,
            )
        val item3ZDB1 =
            TEST_METADATA.copy(
                handle = "item3_zdb3",
                collectionName = "common zdb",
                zdbIds = listOf(ZDB_1),
                publicationYear = 2010,
                publicationType = PublicationType.BOOK,
            )
        val item1ZDB2 =
            TEST_METADATA.copy(
                handle = "foo-zdb2",
                zdbIds = listOf(ZDB_2),
            )
        val item2ZDB2 =
            TEST_METADATA.copy(
                handle = "bar-zdb2",
                zdbIds = listOf(ZDB_2),
            )
        val item1ZDB3 =
            TEST_METADATA.copy(
                handle = "item1_zdb3",
                zdbIds = listOf(ZDB_3),
            )
    }
}
