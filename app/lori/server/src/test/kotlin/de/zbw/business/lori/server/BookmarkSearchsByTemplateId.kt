package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.Template
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
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
class BookmarkSearchsByTemplateId : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        item1ZDB1 to emptyList(),
        item2ZDB1 to emptyList(),
        item1ZDB2 to emptyList(),
    )

    @BeforeClass
    fun fillDB() {
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

    // 1. Create two bookmarks (x)
    // 2. Create template (x)
    // 3. Connect those (x)
    // 4. ItemsA matching first bookmark
    // 5. ItemsB matching second bookmark
    @Test
    fun testFoo() {
        // 1. Create two bookmarks
        val bookmarkId1 = backend.insertBookmark(
            Bookmark(
                bookmarkName = "matchZDB1",
                bookmarkId = 0,
                zdbIdFilter = ZDBIdFilter(
                    zdbIds = listOf(
                        ZDB_1,
                    )
                )
            )
        )

        val bookmarkId2 = backend.insertBookmark(
            Bookmark(
                bookmarkName = "matchZDB2",
                bookmarkId = 0,
                zdbIdFilter = ZDBIdFilter(
                    zdbIds = listOf(
                        ZDB_2,
                    )
                )
            )
        )

        // 2. Create Template
        val templateRightId = backend.insertTemplate(
            template = Template(
                templateName = "applyTemplate",
                createdOn = null,
                createdBy = null,
                lastAppliedOn = null,
                lastUpdatedBy = "someuser",
                lastUpdatedOn = null,
                right = FacetTest.TEST_RIGHT,
            )
        )

        // 3. Connect Bookmarks with Template
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId1,
            templateId = templateRightId.templateId,
        )
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId2,
            templateId = templateRightId.templateId,
        )

        // When
        val result: SearchQueryResult = backend.getSearchResultsByTemplateId(
            templateId = templateRightId.templateId,
            limit = 10,
            offset = 0,
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
