package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
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
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        item1ZDB1 to listOf(TEST_RIGHT.copy(rightId = "a")),
        item2ZDB1 to listOf(TEST_RIGHT.copy(rightId = "b")),
        item1ZDB2 to listOf(TEST_RIGHT.copy(rightId = "c")),
    )

    @BeforeClass
    fun fillDB() {
        val templateRightId = backend.insertTemplate(
            right = TEST_RIGHT.copy(rightId = "Test Template")
        )
        getInitialMetadata().forEach { entry ->
            backend.insertMetadataElement(entry.key)
            entry.value.forEach { right ->
                val r = backend.insertRight(right.copy(templateId = templateRightId.templateId))
                backend.insertItemEntry(entry.key.metadataId, r)
            }
        }
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testSearchByTemplateId() {
        // Get template id (created in fillDB() function)
        val templateId = backend.getTemplateList(10, 0).first().templateId

        // When
        val result: SearchQueryResult = backend.searchQuery(
            searchTerm = "",
            limit = 10,
            offset = 0,
            metadataSearchFilter = emptyList(),
            rightSearchFilter = listOf(TemplateIdFilter(templateId!!)),
        )

        assertThat(
            result.numberOfResults,
            `is`(3),
        )
    }

    companion object {
        const val ZDB_1 = "zdb1"
        const val ZDB_2 = "zdb2"
        val item1ZDB1 = LoriServerBackendTest.TEST_METADATA.copy(
            metadataId = "zdb1",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item2ZDB1 = LoriServerBackendTest.TEST_METADATA.copy(
            metadataId = "zdb2",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item1ZDB2 = LoriServerBackendTest.TEST_METADATA.copy(
            metadataId = "zdb3",
            collectionName = "common zdb",
            zdbId = ZDB_2,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
    }
}
