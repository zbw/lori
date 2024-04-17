package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert.assertThat
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
class FacetTest : DatabaseTest() {
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
                isTemplate = false,
                templateName = null,
            )
        ),
        itemZDB2 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2001, 1, 1),
                endDate = LocalDate.of(2001, 12, 31),
                isTemplate = false,
                templateName = null,
            )
        ),
        itemZDB3 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2002, 1, 1),
                endDate = LocalDate.of(2002, 12, 31),
                isTemplate = false,
                templateName = null,
            )
        ),
        itemSigel1 to listOf(
            TEST_RIGHT.copy(
                accessState = AccessState.OPEN,
                startDate = LocalDate.of(2003, 1, 1),
                endDate = LocalDate.of(2003, 12, 31),
                isTemplate = false,
                templateName = null,
            )
        ),
        itemSigel2 to listOf(
            TEST_RIGHT.copy(
                accessState = AccessState.RESTRICTED,
                startDate = LocalDate.of(2004, 1, 1),
                endDate = LocalDate.of(2004, 12, 31),
                isTemplate = false,
                templateName = null,
            )
        ),
        itemSigel3 to listOf(
            TEST_RIGHT.copy(
                accessState = AccessState.CLOSED,
                startDate = LocalDate.of(2005, 1, 1),
                endDate = LocalDate.of(2005, 12, 31),
                isTemplate = false,
                templateName = null,
            ),
            TEST_RIGHT.copy(
                accessState = AccessState.OPEN,
                startDate = LocalDate.of(2003, 1, 1),
                endDate = LocalDate.of(2003, 12, 31),
                isTemplate = false,
                templateName = null,
            )
        ),
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
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
                listOf(
                    itemSigel1.publicationType to 1,
                    itemSigel2.publicationType to 1,
                    itemSigel3.publicationType to 1,
                    itemZDB1.publicationType to 1,
                    itemZDB2.publicationType to 1,
                    itemZDB3.publicationType to 1,
                ).toMap(),
                listOf(
                    itemZDB1.paketSigel to 3,
                    itemSigel1.paketSigel to 1,
                    itemSigel2.paketSigel to 1,
                    itemSigel3.paketSigel to 1,
                ).toMap(),
                listOf(
                    itemZDB1.zdbId to 1,
                    itemZDB2.zdbId to 1,
                    itemZDB3.zdbId to 1,
                    itemSigel1.zdbId to 3,
                ).toMap(),
                listOf(
                    AccessState.OPEN to 2,
                    AccessState.RESTRICTED to 1,
                    AccessState.CLOSED to 4,
                ).toMap(),
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
                listOf(
                    itemSigel1.publicationType to 1,
                ).toMap(),
                listOf(
                    itemSigel1.paketSigel to 1,
                ).toMap(),
                listOf(
                    itemSigel1.zdbId to 1,
                ).toMap(),
                listOf(AccessState.OPEN to 1).toMap(),
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
                listOf(
                    itemZDB1.publicationType to 1,
                ).toMap(),
                listOf(
                    itemZDB1.paketSigel to 1,
                ).toMap(),
                listOf(
                    itemZDB1.zdbId to 1,
                ).toMap(),
                listOf(AccessState.CLOSED to 1).toMap(),
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
                emptyMap<PublicationType, Int>(),
                emptyMap<String, Int>(),
                emptyMap<String, Int>(),
                emptyMap<AccessState, Int>(),
                "search for all items, filter by ZDB-Id and PaketSigel"
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM)
    fun testFacets(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        expectedPublicationType: Map<PublicationType, Int>,
        expectedPaketSigelIds: Map<String, Int>,
        expectedZDBIds: Map<String, Int>,
        expectedAccessState: Map<AccessState, Int>,
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
        // Test publication types
        assertThat(
            searchResult.publicationType,
            `is`(
                expectedPublicationType,
            )
        )
        // Test access state
        assertThat(
            searchResult.accessState,
            `is`(
                expectedAccessState
            )
        )
    }

    companion object {
        const val DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM = "DATA_FOR_SEARCH_SIGEL_ZDB_WITH_SEARCHTERM"

        private const val SIGEL_1 = "sigel_1"
        private const val SIGEL_2 = "sigel_2"
        private const val SIGEL_3 = "sigel_3"

        private const val ZDB_1 = "zdb1"
        private const val ZDB_2 = "zdb2"
        private const val ZDB_3 = "zdb3"

        val itemZDB1 = TEST_Metadata.copy(
            metadataId = "zdb1",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val itemSigel1 = TEST_Metadata.copy(
            metadataId = "sigel1",
            collectionName = "common sigel",
            paketSigel = SIGEL_1,
            publicationDate = LocalDate.of(2011, 1, 1),
            publicationType = PublicationType.BOOK_PART,
        )

        val itemZDB2 = TEST_Metadata.copy(
            metadataId = "zdb2",
            collectionName = "common zdb",
            zdbId = ZDB_2,
            publicationDate = LocalDate.of(2012, 1, 1),
            publicationType = PublicationType.CONFERENCE_PAPER,
        )
        val itemSigel2 = TEST_Metadata.copy(
            metadataId = "sigel2",
            collectionName = "common sigel",
            paketSigel = SIGEL_2,
            publicationDate = LocalDate.of(2013, 1, 1),
            publicationType = PublicationType.PERIODICAL_PART,
        )

        val itemZDB3 = TEST_Metadata.copy(
            metadataId = "zdb3",
            collectionName = "common zdb",
            zdbId = ZDB_3,
            publicationDate = LocalDate.of(2014, 1, 1),
            publicationType = PublicationType.PROCEEDINGS,
        )
        val itemSigel3 = TEST_Metadata.copy(
            metadataId = "sigel3",
            collectionName = "common sigel",
            paketSigel = SIGEL_3,
            publicationDate = LocalDate.of(2015, 1, 1),
            publicationType = PublicationType.THESIS,
        )

        val TEST_RIGHT = RightFilterTest.TEST_RIGHT
    }
}
