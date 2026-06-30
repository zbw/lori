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
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test

class ArraysNoValueTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                batchConnectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            noZDBID to emptyList(),
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

    @DataProvider(name = DATA_FOR_TEST_ARRAY_NO_VALUE_TEST)
    fun createDataForLicenceURL() =
        arrayOf(
            arrayOf(
                "!zdb:*",
                setOf(noZDBID),
            ),
            arrayOf(
                "zdb:*",
                emptySet<ItemMetadata>(),
            ),
        )

    @Test(dataProvider = DATA_FOR_TEST_ARRAY_NO_VALUE_TEST)
    fun testArraysWithoutValue(
        searchTerm: String,
        expected: Set<ItemMetadata>,
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
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_TEST_ARRAY_NO_VALUE_TEST = "DATA_FOR_TEST_ARRAY_NO_VALUE_TEST"
        private val noZDBID =
            TEST_Metadata.copy(
                handle = "11159/606",
                zdbIds = null,
            )
    }
}
