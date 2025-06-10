package de.zbw.business.lori.server

import de.zbw.business.lori.server.ApplyTemplateTest.Companion.TEST_RIGHT
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant

/**
 * Test wildcard functionality.
 */
class SearchWithWildcards : DatabaseTest() {
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
            item1 to
                listOf(
                    TEST_RIGHT.copy(
                        templateName = "BOREC series handle",
                        isTemplate = true,
                    ),
                ),
            item2 to emptyList(),
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
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

    @DataProvider(name = DATA_FOR_WILDCARD_TESTS)
    fun createDataForWildcardTest() =
        arrayOf(
            arrayOf(
                "lur:by-nc-nd*",
                setOf(
                    item1,
                ),
                "Licence URL with wildcard",
            ),
            arrayOf(
                "ser:big*",
                setOf(
                    item1,
                ),
                "Series with wildcard",
            ),
            arrayOf(
                "tpl:borec*",
                setOf(
                    item1,
                ),
                "Template Name with wildcard",
            ),
            arrayOf(
                "tit:inno*",
                setOf(
                    item1,
                ),
                "title with wildcard",
            ),
            arrayOf(
                "sig:fo*",
                setOf(
                    item1,
                ),
                "one entry in array with wildcard",
            ),
            arrayOf(
                "sig:'fo*,ba*'",
                setOf(
                    item1,
                ),
                "multiple values with wildcard",
            ),
            arrayOf(
                "sig:fo* & sig:ba*",
                setOf(
                    item1,
                ),
                "conjugate values with wildcard",
            ),
            arrayOf(
                "sig:blub%",
                setOf(
                    item2,
                ),
                "find item with special character % -> Test escaping",
            ),
            arrayOf(
                "sig:wild_card",
                setOf(
                    item2,
                ),
                "find item with special character _ -> Test escaping",
            ),
            arrayOf(
                "sig:f*o",
                emptySet<ItemMetadata>(),
                "ignore wildcard in the middle",
            ),
            arrayOf(
                "sig:*oo",
                emptySet<ItemMetadata>(),
                "ignore wildcard at the start",
            ),
            arrayOf(
                "${FilterType.PPN.keyAlias}:EBP107*",
                setOf(
                    item2,
                ),
                "find ppn with wildcard",
            ),
            arrayOf(
                "${FilterType.ISBN.keyAlias}:978-1-84*",
                setOf(
                    item2,
                ),
                "find isbn with wildcard",
            ),
            arrayOf(
                "${FilterType.DOI.keyAlias}:10.1108/S05*",
                setOf(
                    item2,
                ),
                "find doi with wildcard",
            ),
        )

    @Test(dataProvider = DATA_FOR_WILDCARD_TESTS)
    fun testWildcards(
        searchTerm: String,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm,
                    10,
                    0,
                )
            }

        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    companion object {
        const val DATA_FOR_WILDCARD_TESTS = "DATA_FOR_WILDCARD_TESTS"
        val item1 =
            TEST_Metadata.copy(
                title = "Innovations Title",
                handle = "paket sigel array",
                paketSigel = listOf("fooo", "bar", "baz"),
                isPartOfSeries = listOf("bigseries"),
                licenceUrlFilter = "by-nc-nd/4.0/",
            )
        val item2 =
            TEST_Metadata.copy(
                handle = "paket sigel array wildcards",
                paketSigel = listOf("blub%", "wild_card"),
                ppn = "EBP107179776",
                doi = listOf("10.1108/S0573-8555(2004)0000262002"),
                isbn = listOf("978-1-84950-841-4"),
            )
    }
}
