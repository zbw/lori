package de.zbw.business.lori.server

import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.SortInformation
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing [AccessStateOnDateFilter] which returns only items that have at least one right
 * information with the given access state on a given date.
 *
 * Created on 10-01-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessStateOnFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                batchConnectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val metadataStandard =
        TEST_Metadata.copy(
            handle = "111159/74",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDING,
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            metadataStandard to
                listOf(
                    TEST_RIGHT.copy(
                        accessState = AccessState.OPEN,
                        startDate = LocalDate.of(2025, 1, 1),
                        endDate = LocalDate.of(2025, 1, 30),
                        isTemplate = false,
                        templateName = null,
                    ),
                    TEST_RIGHT.copy(
                        accessState = AccessState.OPEN,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 3, 31),
                        isTemplate = true,
                        templateName = "2022",
                    ),
                ),
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns LocalDate.of(2021, 7, 1)
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
    fun testAccessStateOnDateFilter() {
        val rightSearchFilterWithResult =
            listOf(
                AccessStateOnDateFilter(
                    date = LocalDate.of(2025, 1, 4),
                    accessState = AccessState.OPEN,
                ),
            )
        val searchResult1: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilterWithResult,
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )
            }

        assertThat(
            searchResult1.results.map { it.metadata }.toSet(),
            `is`(setOf(metadataStandard)),
        )

        val rightSearchFilterWithoutResult =
            listOf(
                AccessStateOnDateFilter(
                    date = LocalDate.of(2025, 2, 4),
                    accessState = AccessState.OPEN,
                ),
            )
        val searchResult2: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilterWithoutResult,
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )
            }

        assertThat(
            searchResult2.results.map { it.metadata }.toSet(),
            `is`(emptySet()),
        )

        // Use filter as ValidOn
        val rightSearchFilterNoAccessState =
            listOf(
                AccessStateOnDateFilter(
                    date = LocalDate.of(2025, 1, 4),
                    accessState = null,
                ),
            )
        val searchResult3: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilterNoAccessState,
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )
            }

        assertThat(
            searchResult3.results.map { it.metadata }.toSet(),
            `is`(setOf(metadataStandard)),
        )

        // Valid on before created on
        val rightSearchFilterBefore =
            listOf(
                AccessStateOnDateFilter(
                    date = LocalDate.of(2022, 1, 4),
                    accessState = AccessState.OPEN,
                ),
            )
        val searchResultBefore: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilterBefore,
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )
            }

        assertThat(
            searchResultBefore.results.map { it.metadata }.toSet(),
            `is`(emptySet()),
        )

        // Valid on before created on
        val rightSearchFilterInBetween =
            listOf(
                AccessStateOnDateFilter(
                    date = LocalDate.of(2022, 3, 4),
                    accessState = AccessState.OPEN,
                ),
            )
        val searchResultInBetween: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilterInBetween,
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )
            }

        assertThat(
            searchResultInBetween.results.map { it.metadata }.toSet(),
            `is`(setOf(metadataStandard)),
        )
    }

    @Test
    fun testFilterWithCreatedOnAfterStartDate() =
        runBlocking {
            // When a metadata entry got imported while the template was already active this
            // filter should apply from the imported date onwards.

            // given
            val metadataCreatedOnAfterStartDate =
                TEST_Metadata.copy(
                    handle = "111159/76",
                    collectionName = "subject2",
                    publicationType = PublicationType.PROCEEDING,
                )

            val right =
                TEST_RIGHT.copy(
                    accessState = AccessState.OPEN,
                    startDate = LocalDate.of(2002, 1, 1),
                    endDate = null,
                    isTemplate = false,
                    templateName = null,
                )

            // set current date
            val createdOnDate =
                OffsetDateTime
                    .of(
                        2002,
                        3,
                        2,
                        0,
                        0,
                        0,
                        0,
                        ZoneOffset.UTC,
                    )
            mockkStatic(Instant::class)
            every { Instant.now() } returns
                createdOnDate
                    .toInstant()

            backend.insertMetadataElement(metadataCreatedOnAfterStartDate)
            val rightId = backend.insertRight(right)
            backend.insertItemEntry(metadataCreatedOnAfterStartDate.handle, rightId)

            // when
            val rightSearchFilterWithResult =
                listOf(
                    AccessStateOnDateFilter(
                        date = LocalDate.of(createdOnDate.year, createdOnDate.month, createdOnDate.dayOfMonth),
                        accessState = AccessState.OPEN,
                    ),
                )

            // Set local date as well
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns LocalDate.of(createdOnDate.year, createdOnDate.month, createdOnDate.dayOfMonth)

            val searchResult1: SearchQueryResult =
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilterWithResult,
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )

            assertThat(
                searchResult1.results.map { it.metadata.handle }.toSet(),
                `is`(setOf(metadataCreatedOnAfterStartDate.handle)),
            )

            // Set AccessDateOn filter on day ahead and verify a
            val searchResult2: SearchQueryResult =
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter =
                        listOf(
                            AccessStateOnDateFilter(
                                date = LocalDate.of(createdOnDate.year, createdOnDate.month, createdOnDate.dayOfMonth.plus(1)),
                                accessState = AccessState.OPEN,
                            ),
                        ),
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )

            assertThat(
                searchResult2.results.map { it.metadata.handle }.toSet(),
                `is`(setOf(metadataCreatedOnAfterStartDate.handle)),
            )

            // Set AccessDateOn filter on day before created_on and verify an empty result
            val searchResult3: SearchQueryResult =
                backend.searchQuery(
                    searchTerm = null,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter =
                        listOf(
                            AccessStateOnDateFilter(
                                date =
                                    LocalDate.of(createdOnDate.year, createdOnDate.month, createdOnDate.dayOfMonth.minus(1)),
                                accessState = AccessState.OPEN,
                            ),
                        ),
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )

            assertThat(
                searchResult3.results.map { it.metadata.handle }.toSet(),
                `is`(emptySet<String>()),
            )
        }
}
