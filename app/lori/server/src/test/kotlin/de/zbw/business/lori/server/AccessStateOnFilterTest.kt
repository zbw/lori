package de.zbw.business.lori.server

import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
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
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val itemWithRight =
        TEST_Metadata.copy(
            handle = "item with manual right",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDING,
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            itemWithRight to
                listOf(
                    TEST_RIGHT.copy(
                        accessState = AccessState.OPEN,
                        startDate = LocalDate.of(2025, 1, 1),
                        endDate = LocalDate.of(2025, 1, 30),
                        isTemplate = false,
                        templateName = null,
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
                    null,
                    10,
                    0,
                    emptyList(),
                    rightSearchFilterWithResult,
                    null,
                )
            }

        assertThat(
            searchResult1.results.map { it.metadata }.toSet(),
            `is`(setOf(itemWithRight)),
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
                    null,
                    10,
                    0,
                    emptyList(),
                    rightSearchFilterWithoutResult,
                    null,
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
                    null,
                    10,
                    0,
                    emptyList(),
                    rightSearchFilterNoAccessState,
                    null,
                )
            }

        assertThat(
            searchResult3.results.map { it.metadata }.toSet(),
            `is`(setOf(itemWithRight)),
        )
    }
}
