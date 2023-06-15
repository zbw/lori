package de.zbw.business.lori.server

import de.zbw.business.lori.server.FacetTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.LoriServerBackendTest.Companion.NOW
import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_Metadata
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.Template
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertTrue

/**
 * Test applying a template.
 *
 * Created on 06-12-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ApplyTemplateTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        item1ZDB1 to listOf(
            TEST_RIGHT.copy(
                startDate = LocalDate.of(2000, 1, 1),
                endDate = LocalDate.of(2000, 12, 31),
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

    @Test
    fun testApplyTemplate() {
        // Create Bookmark
        val bookmarkId = backend.insertBookmark(
            Bookmark(
                bookmarkName = "applyBookmark",
                bookmarkId = 0,
                zdbIdFilter = ZDBIdFilter(
                    zdbIds = listOf(
                        ZDB_1,
                    )
                )
            )
        )

        // Create Template
        val templateRightId = backend.insertTemplate(
            template = Template(
                templateName = "applyTemplate",
                right = TEST_RIGHT
            )
        )

        // Connect Bookmark and Template
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId,
            templateId = templateRightId.templateId,
        )

        val received: List<String> = backend.applyTemplate(templateRightId.templateId)
        assertThat(
            received,
            `is`(listOf(item1ZDB1.metadataId))
        )

        // Verify that new right is assigned to metadata id
        val rightIds = backend.getRightEntriesByMetadataId(item1ZDB1.metadataId).map { it.rightId }
        assertTrue(rightIds.contains(templateRightId.rightId))

        // Repeat Apply Operation without duplicate entries errors
        val received2: List<String> = backend.applyTemplate(templateRightId.templateId)
        assertThat(
            received2,
            `is`(listOf(item1ZDB1.metadataId))
        )
    }

    companion object {
        const val ZDB_1 = "zdb1"
        val item1ZDB1 = TEST_Metadata.copy(
            metadataId = "zdb1",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
    }
}
