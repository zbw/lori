package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.ZDB_1
import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
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
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.Boolean
import kotlin.Pair
import kotlin.collections.listOf

class ApplyTemplateResolveConflictTest : DatabaseTest() {
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
        fillDB()
    }

    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns ItemDBTest.NOW.minusYears(1L).toInstant()
            getInitialMetadata().forEach { entry ->
                backend.insertMetadataElement(entry.key)
                entry.value.forEach { right ->
                    val r = backend.insertRight(right)
                    backend.insertItemEntry(entry.key.handle, r)
                }
            }
        }

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            item1ZDB1 to
                listOf(
                    TEST_RIGHT.copy(
                        isTemplate = false,
                        endDate = null,
                    ),
                ),
        )

    @DataProvider(name = DATA_FOR_APPLY_RESOLVABLE)
    fun createDateForResolvingConflicts(): Array<Array<Any?>> =
        arrayOf(
            arrayOf(
                TEST_RIGHT.copy(
                    templateName = "date_conflict",
                    isTemplate = true,
                    endDate = TEST_RIGHT.startDate.plusYears(1L),
                    startDate = TEST_RIGHT.startDate,
                ),
                false,
                setOf(item1ZDB1.handle),
                listOf(
                    Pair(true, Pair(LocalDate.of(2022, 2, 28), LocalDate.of(2023, 2, 28))),
                ),
                "Template starts on same date as manual right -> remove old right",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    templateName = "date_conflict",
                    isTemplate = true,
                    endDate = TEST_RIGHT.startDate.plusYears(1L),
                    startDate = TEST_RIGHT.startDate,
                ),
                true,
                setOf(item1ZDB1.handle),
                listOf(
                    Pair(true, Pair(LocalDate.of(2022, 2, 28), LocalDate.of(2023, 2, 28))),
                ),
                "Ensure that dryRun does not return any errors",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    templateName = "date_conflict",
                    isTemplate = true,
                    endDate = TEST_RIGHT.startDate.plusYears(1L),
                    startDate = TEST_RIGHT.startDate.plusDays(14L),
                ),
                false,
                setOf(item1ZDB1.handle),
                listOf(
                    Pair(false, Pair(LocalDate.of(2022, 2, 28), LocalDate.of(2022, 3, 13))),
                    Pair(true, Pair(LocalDate.of(2022, 3, 14), LocalDate.of(2023, 2, 28))),
                ),
                "Template starts after start date of manual right and overlaps -> end old right",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    templateName = "date_conflict",
                    isTemplate = true,
                    endDate = TEST_RIGHT.startDate.plusYears(1L),
                    startDate = TEST_RIGHT.startDate.minusDays(14L),
                ),
                false,
                emptySet<String>(),
                listOf(
                    Pair(false, Pair(LocalDate.of(2022, 2, 28), null)),
                ),
                "Template starts before start date of manual right and overlaps -> don't change anything",
            ),
        )

    @Test(dataProvider = DATA_FOR_APPLY_RESOLVABLE)
    fun testResolveConflict(
        template: ItemRight,
        dryRun: Boolean,
        expectedHandles: Set<String>,
        expectedRightTimes: List<Pair<Boolean, Pair<LocalDate, LocalDate?>>>,
        reason: String,
    ) = runBlocking {
        // Arrange
        mockkStatic(LocalDate::class)
        every { LocalDate.now(any<ZoneId>()) } returns FIRST_APPLICATION_DATE_LOCAL

        // Create Bookmark
        val bookmarkId =
            backend.insertBookmark(
                Bookmark(
                    bookmarkName = "bookmark_zdb1",
                    bookmarkId = 99,
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
        val rightId =
            backend.insertTemplate(
                template,
            )

        // Connect Bookmark and Template
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId,
            rightId = rightId,
        )

        // Act
        val received =
            templateApplication.applyTemplate(
                rightId,
                skipTemplateDrafts = false,
                dryRun = dryRun,
                createdBy = "user1",
            )

        // Assert
        assertThat(
            reason,
            received.appliedMetadataHandles.toSet(),
            `is`(expectedHandles),
        )

        if (dryRun) {
            assertThat(
                received.numberOfErrors,
                `is`(0),
            )
        } else {
            val item: Item = backend.getItemByHandle(item1ZDB1.handle)!!

            val templateAndTimes: List<Pair<Boolean, Pair<LocalDate, LocalDate?>>> =
                item.rights.sortedBy { it.startDate }.map {
                    Pair(it.isTemplate, Pair(it.startDate, it.endDate))
                }
            assertThat(reason, templateAndTimes, `is`(expectedRightTimes))
        }
    }

    companion object {
        val item1ZDB1 =
            TEST_METADATA.copy(
                handle = "11159/1",
                collectionName = "common zdb",
                zdbIds = listOf(ZDB_1),
                publicationYear = 2010,
                publicationType = PublicationType.BOOK,
            )

        val TEST_RIGHT =
            ItemRight(
                rightId = "123",
                accessState = AccessState.CLOSED,
                basisAccessState = BasisAccessState.LICENCE_CONTRACT,
                basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
                createdBy = "user1",
                createdOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        1,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                hasLegalRisk = true,
                endDate = null,
                exceptionOfId = null,
                hasExceptionId = null,
                isTemplate = false,
                firstAppliedOn =
                    OffsetDateTime.of(
                        2021,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                lastAppliedOn =
                    OffsetDateTime.of(
                        2022,
                        5,
                        4,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                lastUpdatedBy = "user2",
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
                startDate = RestConverterTest.TODAY.minusDays(1),
                licenceContract = "some contract",
                notesGeneral = "Some general notes",
                notesFormalRules = "Some formal rule notes",
                notesProcessDocumentation = "Some process documentation",
                notesManagementRelated = "Some management related notes",
                predecessorId = null,
                restrictedOpenContentLicence = false,
                successorId = null,
                zbwUserAgreement = true,
                templateDescription = null,
                templateName = null,
                groups = null,
                groupIds = emptyList(),
            )

        val FIRST_APPLICATION_DATE_LOCAL: LocalDate = TEST_RIGHT.startDate.minusYears(1)
        const val DATA_FOR_APPLY_RESOLVABLE = "DATE_FOR_APPLY_RESOLVABLE"
    }
}
