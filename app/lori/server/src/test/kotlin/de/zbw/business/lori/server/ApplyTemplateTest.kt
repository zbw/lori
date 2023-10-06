package de.zbw.business.lori.server

import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest
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
        every { Instant.now() } returns ItemDBTest.NOW.toInstant()
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
        val rightId = backend.insertRight(TEST_RIGHT.copy(templateId = 6))
        val templateId: Int = backend.getRightsByIds(listOf(rightId)).first().templateId!!

        // Connect Bookmark and Template
        backend.insertBookmarkTemplatePair(
            bookmarkId = bookmarkId,
            templateId = templateId,
        )

        val received: List<String> = backend.applyTemplate(templateId)
        assertThat(
            received,
            `is`(listOf(item1ZDB1.metadataId))
        )

        // Verify that new right is assigned to metadata id
        val rightIds = backend.getRightEntriesByMetadataId(item1ZDB1.metadataId).map { it.rightId }
        assertTrue(rightIds.contains(rightId))

        assertThat(
            backend.getRightByTemplateId(templateId)!!.lastAppliedOn,
            `is`(ItemDBTest.NOW),
        )

        // Repeat Apply Operation without duplicate entries errors
        val received2: List<String> = backend.applyTemplate(templateId)
        assertThat(
            received2,
            `is`(listOf(item1ZDB1.metadataId))
        )

        // Add two new items to database matching bookmark
        backend.insertMetadataElements(
            listOf(
                item2ZDB1,
                item3ZDB1
            )
        )
        // Update old item from database so it no longer matches for bookmark
        backend.upsertMetaData(listOf(item1ZDB1.copy(zdbId = "foobar")))

        // Apply Template
        val received3: List<String> = backend.applyTemplate(templateId)
        assertThat(
            received3,
            `is`(
                listOf(
                    item2ZDB1.metadataId,
                    item3ZDB1.metadataId,
                )
            )
        )
        // Verify that only the new items are connected to template
        assertThat(
            backend.dbConnector.itemDB.countItemByRightId(rightId),
            `is`(2),
        )

        val applyAllReceived: Map<Int, List<String>> = backend.applyAllTemplates()
        assertThat(
            applyAllReceived.values.flatten().toSet(),
            `is`(
                setOf(
                    item2ZDB1.metadataId,
                    item3ZDB1.metadataId,
                )
            )
        )
    }

    companion object {
        const val ZDB_1 = "zdb1"
        val TEST_RIGHT = RightFilterTest.TEST_RIGHT
        val item1ZDB1 = TEST_METADATA.copy(
            metadataId = "zdb1",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item2ZDB1 = TEST_METADATA.copy(
            metadataId = "zdb2",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
        val item3ZDB1 = TEST_METADATA.copy(
            metadataId = "zdb3",
            collectionName = "common zdb",
            zdbId = ZDB_1,
            publicationDate = LocalDate.of(2010, 1, 1),
            publicationType = PublicationType.BOOK,
        )
    }
}
