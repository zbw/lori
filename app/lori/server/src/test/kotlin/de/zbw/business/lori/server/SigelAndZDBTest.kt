package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseConnectorTest
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Testing if search requests return the
 * expected Paket-Sigel and ZDB-Ids.
 *
 * Created on 10-28-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SigelAndZDBTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        itemZDB1 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2000, 1, 1),
                endDate = LocalDate.of(2000, 12, 31),
            )
        ),
        itemZDB2 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2001, 1, 1),
                endDate = LocalDate.of(2001, 12, 31),
            )
        ),
        itemZDB3 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2002, 1, 1),
                endDate = LocalDate.of(2002, 12, 31),
            )
        ),
        itemSigel1 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2003, 1, 1),
                endDate = LocalDate.of(2003, 12, 31),
            )
        ),
        itemSigel2 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2004, 1, 1),
                endDate = LocalDate.of(2004, 12, 31),
            )
        ),
        itemSigel3 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2005, 1, 1),
                endDate = LocalDate.of(2005, 12, 31),
            )
        ),
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns DatabaseConnectorTest.NOW.toInstant()
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2021, 7, 1)
        getInitialMetadata().forEach { entry ->
            backend.insertMetadataElement(entry.key)
            entry.value.forEach { right ->
                val r = backend.insertRight(right)
                backend.insertItemEntry(entry.key.metadataId, r)
            }
        }
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM)
    fun createDataForSearchSigelZDBWithSearchTerm() =
        arrayOf(
            arrayOf(
                "col:'common'",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                setOf(
                    itemSigel1,
                    itemSigel2,
                    itemSigel3,
                    itemZDB1,
                    itemZDB2,
                    itemZDB3,
                ),
                6,
                setOf(
                    itemZDB1.paketSigel,
                    itemSigel1.paketSigel,
                    itemSigel2.paketSigel,
                    itemSigel3.paketSigel,
                ),
                setOf(
                    itemZDB1.zdbId,
                    itemZDB2.zdbId,
                    itemZDB3.zdbId,
                    itemSigel1.zdbId,
                ),
                "search for all items, no filter"
            ),
            arrayOf(
                "col:'common'",
                listOf(
                    PaketSigelFilter(
                        listOf(
                            itemSigel1.paketSigel!!
                        )
                    ),
                ),
                emptyList<RightSearchFilter>(),
                setOf(
                    itemSigel1,
                ),
                1,
                setOf(
                    itemZDB1.paketSigel,
                    itemSigel1.paketSigel,
                    itemSigel2.paketSigel,
                    itemSigel3.paketSigel,
                ),
                setOf(
                    itemZDB1.zdbId,
                    itemZDB2.zdbId,
                    itemZDB3.zdbId,
                    itemSigel1.zdbId,
                ),
                "search for all items, filter by PaketSigel Id"
            ),
            arrayOf(
                "col:'common'",
                listOf(
                    ZDBIdFilter(
                        listOf(
                            itemZDB1.zdbId!!
                        )
                    ),
                ),
                emptyList<RightSearchFilter>(),
                setOf(
                    itemZDB1,
                ),
                1,
                setOf(
                    itemZDB1.paketSigel,
                    itemSigel1.paketSigel,
                    itemSigel2.paketSigel,
                    itemSigel3.paketSigel,
                ),
                setOf(
                    itemZDB1.zdbId,
                    itemZDB2.zdbId,
                    itemZDB3.zdbId,
                    itemSigel1.zdbId,
                ),
                "search for all items, filter by ZDB-Id"
            ),
            arrayOf(
                "col:'common'",
                listOf(
                    PaketSigelFilter(
                        listOf(
                            itemSigel1.paketSigel!!
                        )
                    ),
                    ZDBIdFilter(
                        listOf(
                            itemZDB1.zdbId!!
                        )
                    ),
                ),
                emptyList<RightSearchFilter>(),
                emptySet<ItemMetadata>(),
                0,
                setOf(
                    itemZDB1.paketSigel,
                    itemSigel1.paketSigel,
                    itemSigel2.paketSigel,
                    itemSigel3.paketSigel,
                ),
                setOf(
                    itemZDB1.zdbId,
                    itemZDB2.zdbId,
                    itemZDB3.zdbId,
                    itemSigel1.zdbId,
                ),
                "search for all items, filter by ZDB-Id and PaketSigel"
            ),
        )


    @Test(dataProvider = DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM)
    fun testSearchWithRightFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        expectedPaketSigelIds: Set<String>,
        expectedZDBIds: Set<String>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult = backend.searchQuery(
            givenSearchTerm,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            searchResult.numberOfResults,
            `is`(
                expectedNumberOfResults
            ),
        )
        assertThat(
            searchResult.paketSigels,
            `is`(
                expectedPaketSigelIds
            )
        )
        assertThat(
            searchResult.zdbIds,
            `is`(
                expectedZDBIds
            )
        )
    }

    companion object {
        const val DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM = "DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM"

        const val SIGEL_1 = "sigel_1"
        const val SIGEL_2 = "sigel_2"
        const val SIGEL_3 = "sigel_3"

        const val ZDB_1 = "zdb1"
        const val ZDB_2 = "zdb2"
        const val ZDB_3 = "zdb3"

        val itemZDB1 = DatabaseConnectorTest.TEST_Metadata.copy(
            metadataId = "zdb1",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
        )
        val itemSigel1 = DatabaseConnectorTest.TEST_Metadata.copy(
            metadataId = "sigel1",
            collectionName = "common sigel",
            paketSigel = SIGEL_1,
            publicationDate = LocalDate.of(2011, 1, 1),
        )

        val itemZDB2 = DatabaseConnectorTest.TEST_Metadata.copy(
            metadataId = "zdb2",
            collectionName = "common zdb",
            zdbId = ZDB_2,
            publicationDate = LocalDate.of(2012, 1, 1),
        )
        val itemSigel2 = DatabaseConnectorTest.TEST_Metadata.copy(
            metadataId = "sigel2",
            collectionName = "common sigel",
            paketSigel = SIGEL_2,
            publicationDate = LocalDate.of(2013, 1, 1),
        )

        val itemZDB3 = DatabaseConnectorTest.TEST_Metadata.copy(
            metadataId = "zdb3",
            collectionName = "common zdb",
            zdbId = ZDB_3,
            publicationDate = LocalDate.of(2014, 1, 1),
        )
        val itemSigel3 = DatabaseConnectorTest.TEST_Metadata.copy(
            metadataId = "sigel3",
            collectionName = "common sigel",
            paketSigel = SIGEL_3,
            publicationDate = LocalDate.of(2015, 1, 1),
        )

        val TEST_RIGHT = RightFilterTest.TEST_RIGHT
    }
}