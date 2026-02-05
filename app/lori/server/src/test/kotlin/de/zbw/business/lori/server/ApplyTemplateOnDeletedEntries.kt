package de.zbw.business.lori.server

import de.zbw.business.lori.server.ApplyTemplateTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.ZDB_3
import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ApplyTemplateOnDeletedEntries : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk {
                every { url } returns "foo.bar"
            },
        )
    private val templateApplication =
        TemplateApplication(
            dbConnector = backend.dbConnector,
            backend = backend,
        )

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun deleteWhileTemplateIsActive() =
        runBlocking {
            // Set created_on date
            mockkStatic(Instant::class)
            every { Instant.now() } returns CREATED_ON.toInstant()

            // Insert Metadata
            backend.insertMetadataElement(itemDeletedWhileTemplateIsActive)

            // Set first application date
            mockkStatic(Instant::class)
            every { Instant.now() } returns CREATED_ON.plusDays(3L).toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns CREATED_ON.plusDays(3L).toLocalDate()
            every { LocalDate.now(any<ZoneId>()) } returns LocalDate.of(2022, 1, 3)

            // Create bookmark and Template
            val bookmarkId =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "zdb3",
                        bookmarkId = 99,
                        zdbIdFilters =
                            listOf(
                                ZDBIdFilter(
                                    zdbId = ZDB_3,
                                ),
                            ),
                        lastUpdatedOn = null,
                        lastUpdatedBy = "user2",
                        createdBy = "user1",
                        createdOn = null,
                    ),
                )

            // Create Template
            val rightId =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "testDryRun",
                        isTemplate = true,
                        endDate = LocalDate.of(CREATED_ON.year, 12, 31),
                        startDate = LocalDate.of(CREATED_ON.year, 2, 1),
                    ),
                )

            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkId,
                rightId = rightId,
            )

            val received =
                templateApplication.applyTemplate(
                    rightId,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                received.appliedMetadataHandles,
                `is`(listOf(itemDeletedWhileTemplateIsActive.handle)),
            )

            // Set date of deletion
            mockkStatic(Instant::class)
            every { Instant.now() } returns DELETION_DATE.toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns DELETION_DATE.toLocalDate()

            // Mark metadata as deleted (same method used as in fullimport)
            val countDeleted = backend.updateMetadataAsDeleted(DELETION_DATE.toInstant())
            assertThat(
                countDeleted,
                `is`(1),
            )

            // Set date of next application
            mockkStatic(Instant::class)
            every { Instant.now() } returns DELETION_DATE.plusDays(1L).toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns DELETION_DATE.plusDays(1L).toLocalDate()

            // Ensure the template no longer gets applied
            val receivedAfterDeletion =
                templateApplication.applyTemplate(
                    rightId,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                receivedAfterDeletion.appliedMetadataHandles,
                `is`(emptyList()),
            )

            // Go to the next year
            mockkStatic(Instant::class)
            every { Instant.now() } returns FUTURE_TEMPLATE_START_DATE.toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns FUTURE_TEMPLATE_START_DATE.toLocalDate()

            // Create bookmark and Template
            val bookmarkIdFuture =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "zdb3withpublicationtype",
                        bookmarkId = 99,
                        zdbIdFilters =
                            listOf(
                                ZDBIdFilter(
                                    zdbId = ZDB_3,
                                ),
                            ),
                        publicationTypeFilter =
                            PublicationTypeFilter(listOf(itemDeletedWhileTemplateIsActive.publicationType)),
                        lastUpdatedOn = null,
                        lastUpdatedBy = "user2",
                        createdBy = "user1",
                        createdOn = null,
                    ),
                )

            // Create Template
            val rightIdFuture =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "templateAfterDeletionRange",
                        isTemplate = true,
                        endDate = LocalDate.of(CREATED_ON.year, 12, 31),
                        startDate = LocalDate.of(CREATED_ON.year, 2, 1),
                    ),
                )

            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdFuture,
                rightId = rightIdFuture,
            )

            val receivedFuture =
                templateApplication.applyTemplate(
                    rightIdFuture,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )
            assertThat(
                receivedFuture.appliedMetadataHandles,
                `is`(emptyList<String>()),
            )
        }

    companion object {
        val itemDeletedWhileTemplateIsActive =
            TEST_METADATA.copy(
                handle = "11159/6",
                zdbIds = listOf(ZDB_3),
            )
        val CREATED_ON: OffsetDateTime =
            OffsetDateTime.of(
                2022,
                1,
                1,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!
        val DELETION_DATE: OffsetDateTime =
            OffsetDateTime.of(
                2022,
                5,
                12,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!
        val FUTURE_TEMPLATE_START_DATE: OffsetDateTime =
            OffsetDateTime.of(
                2023,
                1,
                1,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!
    }
}
