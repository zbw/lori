package de.zbw.business.lori.server

import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Testing NoRightFilter which  Return only items that DON'T have any
 * Right Information attached.
 *
 * Created on 03-02-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class NoRightFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connection = dataSource.connection,
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )
    private val itemRightRestricted =
        TEST_Metadata.copy(
            metadataId = "restricted right",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDINGS,
        )
    private val itemNoRight =
        TEST_Metadata.copy(
            metadataId = "no rights",
            collectionName = "subject1",
            publicationType = PublicationType.PROCEEDINGS,
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            itemRightRestricted to listOf(TEST_RIGHT.copy(accessState = AccessState.RESTRICTED)),
            itemNoRight to emptyList(),
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

    @DataProvider(name = DATA_FOR_SEARCH_WITH_NO_RIGHT_FILTER)
    fun createDataForSearchWithNoRightFilter() =
        arrayOf(
            arrayOf(
                "col:subject1",
                emptyList<MetadataSearchFilter>(),
                null,
                setOf(itemRightRestricted, itemNoRight),
            ),
            arrayOf(
                "col:subject1",
                emptyList<MetadataSearchFilter>(),
                NoRightInformationFilter(),
                setOf(itemNoRight),
            ),
            arrayOf(
                "col:subject1",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                        ),
                    ),
                ),
                null,
                setOf(itemRightRestricted, itemNoRight),
            ),
            arrayOf(
                "col:subject1",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDINGS,
                        ),
                    ),
                ),
                NoRightInformationFilter(),
                setOf(itemNoRight),
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_WITH_NO_RIGHT_FILTER)
    fun testSearchWithNoRightFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedResult: Set<ItemMetadata>,
    ) {
        val searchResult: SearchQueryResult =
            backend.searchQuery(
                givenSearchTerm,
                10,
                0,
                metadataSearchFilter,
                emptyList(),
                noRightInformationFilter,
            )
        assertThat(
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    companion object {
        const val DATA_FOR_SEARCH_WITH_NO_RIGHT_FILTER = "DATA_FOR_SEARCH_WITH_RIGHT_NO_RIGHT_FILTER"
    }
}
