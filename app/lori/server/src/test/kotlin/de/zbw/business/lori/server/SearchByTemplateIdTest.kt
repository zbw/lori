package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_RIGHT
import io.mockk.mockk
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDate

/**
 * Test receiving all search results saved in bookmarks
 * related to a Template ID.
 *
 * Created on 07-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SearchByTemplateIdTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connection = dataSource.connection,
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private var rightIds: List<String> = emptyList()

    @BeforeClass
    fun fillDB() {
        rightIds =
            initialTemplates.map {
                backend.insertTemplate(
                    it,
                )
            }
        initialItems.forEach { entry: Map.Entry<ItemMetadata, List<Int>> ->
            val metadataId: String = backend.insertMetadataElement(entry.key)
            entry.value.forEach { templateKey ->
                backend.insertItemEntry(metadataId, rightIds[templateKey])
            }
        }
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_SEARCH_BY_TEMPLATE_ID)
    fun createDataForSearchByTemplateId() =
        arrayOf(
            arrayOf(
                0,
                1,
            ),
            arrayOf(
                1,
                2,
            ),
            arrayOf(
                2,
                1,
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_BY_TEMPLATE_ID)
    fun testSearchByTemplateId(
        templateIdIdx: Int,
        expectedNumberOfResults: Int,
    ) {
        // When
        val result: SearchQueryResult =
            backend.searchQuery(
                searchTerm = "",
                limit = 10,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter =
                    listOf(
                        TemplateNameFilter(
                            listOf(
                                rightIds[templateIdIdx],
                            ),
                        ),
                    ),
            )

        assertThat(
            result.numberOfResults,
            `is`(expectedNumberOfResults),
        )
    }

    // TODO(CB): Test with multiple templates assigned
    companion object {
        const val DATA_FOR_SEARCH_BY_TEMPLATE_ID = "DATA_FOR_SEARCH_BY_TEMPLATE_ID"
        val EXAMPLE_DATE: LocalDate =
            LocalDate.of(
                2022,
                3,
                1,
            )!!

        val initialTemplates =
            listOf(
                TEST_RIGHT.copy(
                    rightId = "a",
                    startDate = EXAMPLE_DATE.minusDays(10),
                    endDate = EXAMPLE_DATE.minusDays(9),
                    isTemplate = true,
                    templateName = "1",
                ),
                TEST_RIGHT.copy(
                    rightId = "b",
                    startDate = EXAMPLE_DATE.minusDays(8),
                    endDate = EXAMPLE_DATE.minusDays(7),
                    isTemplate = true,
                    templateName = "2",
                ),
                TEST_RIGHT.copy(
                    rightId = "c",
                    startDate = EXAMPLE_DATE.minusDays(6),
                    endDate = EXAMPLE_DATE.minusDays(5),
                    isTemplate = true,
                    templateName = "3",
                ),
            )
        val initialItems =
            mapOf(
                LoriServerBackendTest.TEST_METADATA.copy(
                    metadataId = "1",
                ) to listOf(0, 1),
                LoriServerBackendTest.TEST_METADATA.copy(
                    metadataId = "2",
                ) to listOf(1),
                LoriServerBackendTest.TEST_METADATA.copy(
                    metadataId = "3",
                ) to listOf(2),
            )
    }
}
