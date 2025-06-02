package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
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
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test

/**
 * Testing [LicenceUrlFilterLUK]:
 *
 * Created on 14-05-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LicenceURLTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val exampleHttps =
        TEST_Metadata.copy(
            handle = "https",
            licenceUrl = "https://creativecommons.org/licenses/by-nc-nd/3.0/",
        )

    private val exampleHttp =
        TEST_Metadata.copy(
            handle = "http",
            licenceUrl = "http://creativecommons.org/licenses/by-nc-sa/4.0/legalcode",
        )

    private val exampleWWW =
        TEST_Metadata.copy(
            handle = "www",
            licenceUrl = "https://www.creativecommons.org/licenses/by-nc/4.0/",
        )

    private val exampleNoProtocol =
        TEST_Metadata.copy(
            handle = "noProtocol",
            licenceUrl = "dx.doi.org/10.17811/ebl.5.4.2016.145-151",
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            exampleHttps to emptyList(),
            exampleHttp to emptyList(),
            exampleWWW to emptyList(),
            exampleNoProtocol to emptyList(),
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

    @DataProvider(name = DATA_FOR_TEST_LICENCE_URL)
    fun createDataForLicenceURL() =
        arrayOf(
            arrayOf(
                "luk:\"creativecommons\" & luk:\"by-nc-nd\"",
                setOf(exampleHttps),
            ),
            arrayOf(
                "luk:\"creativecommons\" & luk:\"by-nc-nd\"",
                setOf(exampleHttps),
            ),
            arrayOf(
                "luk:\"creativecommons\" & luk:\"by-nc\"",
                setOf(exampleWWW),
            ),
            arrayOf(
                "luk:\"creativecommons\" & luk:\"by\"",
                emptySet<ItemMetadata>(),
            ),
            arrayOf(
                "luk:\"17811\" & luk:\"ebl\"",
                setOf(exampleNoProtocol),
            ),
        )

    @Test(dataProvider = DATA_FOR_TEST_LICENCE_URL)
    fun testLicenceUrl(
        searchTerm: String,
        expected: Set<ItemMetadata>,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm,
                    10,
                    0,
                    emptyList(),
                    emptyList(),
                    null,
                )
            }
        assertThat(
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_TEST_LICENCE_URL = "DATA_FOR_TEST_LICENCE_URL"
    }
}
