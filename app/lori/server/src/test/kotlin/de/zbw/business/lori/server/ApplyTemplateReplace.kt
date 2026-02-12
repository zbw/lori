package de.zbw.business.lori.server

import de.zbw.business.lori.server.ApplyTemplateTest.Companion.ZDB_2
import de.zbw.business.lori.server.MetadataDeleteTest.Companion.SIGEL_1
import de.zbw.business.lori.server.MetadataDeleteTest.Companion.ZDB_1
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
import org.testng.AssertJUnit.assertFalse
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ApplyTemplateReplace : DatabaseTest() {
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

    @BeforeMethod
    fun beforeMethod() {
        runBlocking {
            backend.dbConnector.cleanAllTables()
        }
    }

    @DataProvider(name = DATA_FOR_REPLACE_TEMPLATE)
    fun createDataForReplaceTemplate() =
        arrayOf(
            arrayOf(
                LocalDate.of(2025, 12, 17),
                TEMPLATE_APPLICATION_DATE,
                LocalDate.of(2025, 12, 18),
                TEMPLATE_APPLICATION_DATE.plusDays(1),
                Pair(LocalDate.of(2025, 12, 17), LocalDate.of(2025, 12, 17)),
                "Last update no longer than a day ago -> endDate == current day - 1",
            ),
            arrayOf(
                LocalDate.of(2025, 12, 17),
                TEMPLATE_APPLICATION_DATE,
                LocalDate.of(2025, 12, 20),
                TEMPLATE_APPLICATION_DATE.plusDays(3),
                Pair(LocalDate.of(2025, 12, 17), LocalDate.of(2025, 12, 20)),
                "Last update longer than a day ago -> endDate == current day",
            ),
        )

    @Test(dataProvider = DATA_FOR_REPLACE_TEMPLATE)
    fun testReplacing(
        firstApplicationLocalDate: LocalDate,
        firstApplicationDate: OffsetDateTime,
        secondApplicationLocalDate: LocalDate,
        secondApplicationDate: OffsetDateTime,
        expectedTimes: Pair<LocalDate, LocalDate>,
        reason: String,
    ) = runBlocking {
        mockkStatic(Instant::class)
        mockkStatic(LocalDate::class)
        every { LocalDate.now(any<ZoneId>()) } returns firstApplicationLocalDate
        every { Instant.now() } returns firstApplicationDate.toInstant()

        val metadataZdb1 =
            TEST_Metadata.copy(
                handle = "11159/7086",
                deleted = false,
                zdbIds = listOf(ZDB_1),
                paketSigel = listOf(SIGEL_1),
            )

        val metadataZdb1To2 =
            TEST_Metadata.copy(
                handle = "11159/7087",
                deleted = false,
                zdbIds = listOf(ZDB_1),
                paketSigel = listOf(SIGEL_1),
            )

        backend.insertMetadataElement(metadataZdb1)
        backend.insertMetadataElement(metadataZdb1To2)

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

        // Create Template
        val templateIdZDB =
            backend.insertTemplate(
                TEST_RIGHT.copy(
                    templateName = "zdbTemplate",
                    isTemplate = true,
                    startDate = LocalDate.of(2025, 12, 1),
                    endDate = LocalDate.of(2026, 1, 9),
                ),
            )

        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkIdZDB,
            rightId = templateIdZDB,
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
            receivedTemplateZDB.appliedMetadataHandles.toSet(),
            `is`(setOf(metadataZdb1.handle, metadataZdb1To2.handle)),
        )

        // when
        backend.upsertMetadata(
            listOf(
                metadataZdb1To2.copy(
                    zdbIds = listOf(ZDB_2),
                ),
            ),
        )

        every { LocalDate.now(any<ZoneId>()) } returns secondApplicationLocalDate
        every { Instant.now() } returns secondApplicationDate.toInstant()

        // then
        val receivedTemplateZDBAfter =
            templateApplication.applyTemplate(
                templateIdZDB,
                skipTemplateDrafts = false,
                dryRun = false,
                createdBy = "user1",
            )
        assertThat(
            receivedTemplateZDBAfter.appliedMetadataHandles.toSet(),
            `is`(setOf(metadataZdb1.handle)),
        )

        val result = backend.getItemList(2, 0).filter { it.metadata.handle == metadataZdb1To2.handle }
        assertThat(
            result.first().rights.size,
            `is`(1),
        )

        assertFalse(
            result
                .first()
                .rights
                .first()
                .isTemplate,
        )

        val times =
            result.first().rights.sortedBy { it.startDate }.map {
                Pair(it.startDate, it.endDate)
            }

        assertThat(
            reason,
            times,
            `is`(
                listOf(
                    expectedTimes.first to expectedTimes.second,
                ),
            ),
        )
    }

    companion object {
        const val DATA_FOR_REPLACE_TEMPLATE = "DATA_FOR_REPLACE_TEMPLATE"
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
    }
}
