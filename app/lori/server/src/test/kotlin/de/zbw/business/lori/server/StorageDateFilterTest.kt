package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
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
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

/**
 * Testing [StorageDateFilter]:
 *
 * Created on 24-11-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class StorageDateFilterTest : DatabaseTest() {
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
            withStorageDate to emptyList(),
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

    @DataProvider(name = DATA_FOR_STORAGE_DATE_FILTER)
    fun createDataForStorageDate() =
        arrayOf(
            arrayOf(
                "${FilterType.STORAGE_DATE.keyAlias}:\"${withStorageDate.storageDate!!.minusDays(1L).format(ISO_LOCAL_DATE)}--\"",
                setOf(withStorageDate),
                "From date one day off",
            ),
            arrayOf(
                "${FilterType.STORAGE_DATE.keyAlias}:" +
                    "\"${withStorageDate.storageDate.format(ISO_LOCAL_DATE)}" +
                    "--\"",
                setOf(withStorageDate),
                "Same from date",
            ),
            arrayOf(
                "${FilterType.STORAGE_DATE.keyAlias}:" +
                    "\"--" +
                    "${withStorageDate.storageDate.format(ISO_LOCAL_DATE)}\"",
                setOf(withStorageDate),
                "Same to date",
            ),
            arrayOf(
                "${FilterType.STORAGE_DATE.keyAlias}:" +
                    "\"${withStorageDate.storageDate.format(ISO_LOCAL_DATE)}" +
                    "--" +
                    "${withStorageDate.storageDate.plusYears(1L).format(ISO_LOCAL_DATE)}\"",
                setOf(withStorageDate),
                "Valid from--to range",
            ),
            arrayOf(
                "${FilterType.STORAGE_DATE.keyAlias}:" +
                    "\"${withStorageDate.storageDate.plusDays(1L).format(ISO_LOCAL_DATE)}" +
                    "--\"",
                emptySet<ItemMetadata>(),
                "No result with from",
            ),
            arrayOf(
                "${FilterType.STORAGE_DATE.keyAlias}:" +
                    "\"--" +
                    "${withStorageDate.storageDate.minusDays(1L).format(ISO_LOCAL_DATE)}\"",
                emptySet<ItemMetadata>(),
                "No result with to",
            ),
        )

    @Test(dataProvider = DATA_FOR_STORAGE_DATE_FILTER)
    fun testStorageDateFilter(
        searchTerm: String,
        expected: Set<ItemMetadata>,
        reason: String,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = searchTerm,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = null,
                    sortInformation = SortInformation.DEFAULT,
                )
            }
        assertThat(
            reason,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_STORAGE_DATE_FILTER = "DATA_FOR_STORAGE_DATE_FILTER"

        val withStorageDate =
            TEST_Metadata.copy(
                handle = "11159/186",
                storageDate =
                    OffsetDateTime.of(
                        2020,
                        3,
                        1,
                        5,
                        11,
                        12,
                        0,
                        ZoneOffset.UTC,
                    )!!,
            )
    }
}
