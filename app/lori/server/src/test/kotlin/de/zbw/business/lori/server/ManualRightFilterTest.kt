package de.zbw.business.lori.server

import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
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
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Testing [ManualRightFilter] which returns only items that have at least one right
 * information created by hand.
 *
 * Created on 08-01-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ManualRightFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )
    private val itemRightManual =
        TEST_Metadata.copy(
            handle = "item with manual right",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDING,
        )

    private val itemWithTemplate =
        TEST_Metadata.copy(
            handle = "item only with template",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDING,
        )

    private val itemNoRight =
        TEST_Metadata.copy(
            handle = "no rights",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDING,
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            itemRightManual to listOf(TEST_RIGHT.copy(isTemplate = false, templateName = null)),
            itemWithTemplate to listOf(TEST_RIGHT.copy(isTemplate = true)),
            itemNoRight to emptyList(),
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

    @DataProvider(name = DATA_FOR_TEST_MANUAL_RIGHT_FILTER)
    fun createDataForManualRightFilter() =
        arrayOf(
            arrayOf(
                "man:on",
                emptyList<RightSearchFilter>(),
                setOf(itemRightManual),
                "Test keyword search",
            ),
            arrayOf(
                null,
                listOf(
                    ManualRightFilter(),
                ),
                setOf(itemRightManual),
                "Test filter search",
            ),
            arrayOf(
                null,
                emptyList<RightSearchFilter>(),
                setOf(itemRightManual, itemNoRight, itemWithTemplate),
                "Empty search",
            ),
        )

    @Test(dataProvider = DATA_FOR_TEST_MANUAL_RIGHT_FILTER)
    fun testManualRightFilter(
        givenSearchTerm: String?,
        rightSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        reason: String,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    givenSearchTerm,
                    10,
                    0,
                    emptyList(),
                    rightSearchFilter,
                    null,
                )
            }
        assertThat(
            reason,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            reason,
            searchResult.results.map { it.metadata }.size,
            `is`(expectedResult.size),
        )
    }

    companion object {
        const val DATA_FOR_TEST_MANUAL_RIGHT_FILTER = "DATA_FOR_TEST_MANUAL_RIGHT_FILTER"
    }
}
