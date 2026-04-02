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
            item3 to emptyList(),
            item4 to emptyList(),
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
                "${FilterType.DOI.keyAlias}:10.1108/S05*",
                setOf(
                    item2.handle,
                ),
                "find doi with wildcard",
            ),
            arrayOf(
                "sig:wild_card",
                setOf(
                    item2.handle,
                ),
                "find item with special character _ -> Test escaping",
            ),
            arrayOf(
                "sig:blub%",
                setOf(
                    item2.handle,
                ),
                "find item with special character % -> Test escaping",
            ),
            arrayOf(
                "sig:fo* & sig:ba*",
                setOf(
                    item1.handle,
                ),
                "conjugate values with wildcard",
            ),
            arrayOf(
                "ser:big*",
                setOf(
                    item1.handle,
                ),
                "Series with wildcard",
            ),
            arrayOf(
                "lur:by-nc-nd*",
                setOf(
                    item1.handle,
                ),
                "Licence URL with wildcard",
            ),
            arrayOf(
                "tpl:borec*",
                setOf(
                    item1.handle,
                ),
                "Template Name with wildcard",
            ),
            arrayOf(
                "tit:inno*",
                setOf(
                    item1.handle,
                ),
                "title with wildcard",
            ),
            arrayOf(
                "sig:fo*",
                setOf(
                    item1.handle,
                ),
                "one entry in array with wildcard",
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
                    item2.handle,
                ),
                "find ppn with wildcard",
            ),
            arrayOf(
                "${FilterType.ISBN.keyAlias}:978-1-84*",
                setOf(
                    item2.handle,
                ),
                "find isbn with wildcard",
            ),
            arrayOf(
                "${FilterType.PAKET_SIGEL.keyAlias}:'zdb-1-dhw'",
                setOf(
                    item4.handle,
                ),
                "do not find sigel 'zdb-1-dhww'",
            ),
            arrayOf(
                "${FilterType.PAKET_SIGEL.keyAlias}:'zdb-1-dhww'",
                setOf(
                    item3.handle,
                ),
                "do not find sigel 'zdb-1-dhw'",
            ),
            arrayOf(
                "${FilterType.PAKET_SIGEL.keyAlias}:'zdb-1-dhw*,bar'",
                setOf(
                    item1.handle,
                    item3.handle,
                    item4.handle,
                ),
                "allow wildcard in lists",
            ),
        )

    @Test(dataProvider = DATA_FOR_WILDCARD_TESTS)
    fun testWildcards(
        searchTerm: String,
        expectedResult: Set<String>,
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
            searchResult.results.map { it.metadata.handle }.toSet(),
            `is`(expectedResult),
        )
    }

    companion object {
        const val DATA_FOR_WILDCARD_TESTS = "DATA_FOR_WILDCARD_TESTS"
        val item1 =
            TEST_Metadata.copy(
                title = "Innovations Title",
                handle = "11159/7921",
                paketSigel = listOf("fooo", "bar", "baz"),
                isPartOfSeries = listOf("bigseries"),
                licenceUrlFilter = "by-nc-nd/4.0/",
            )
        val item2 =
            TEST_Metadata.copy(
                handle = "11159/7922",
                paketSigel = listOf("blub%", "wild_card"),
                ppn = "EBP107179776",
                doi = listOf("10.1108/S0573-8555(2004)0000262002"),
                isbn = listOf("978-1-84950-841-4"),
            )
        val item3 =
            TEST_Metadata.copy(
                handle = "11159/7923",
                paketSigel = listOf("zdb-1-ewe", "zdb-1-dhww"),
            )
        val item4 =
            TEST_Metadata.copy(
                handle = "11159/7924",
                paketSigel = listOf("zdb-1-dhw"),
            )
    }
}
