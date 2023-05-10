package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.Template
import de.zbw.persistence.lori.server.BookmarkDBTest.Companion.TEST_BOOKMARK
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_RIGHT
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant

/**
 * Testing [TemplateDB].
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class TemplateDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    )
    private val templateDB = dbConnector.templateDB
    private val bookmarkDB = dbConnector.bookmarkDB

    @BeforeClass
    fun beforeTests() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns ItemDBTest.NOW.toInstant()
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testTemplateRoundtrip() {
        // Case: Create and Read
        // when
        val generatedIds = templateDB.insertTemplate(TEST_TEMPLATE)
        val receivedTemplates = templateDB.getTemplatesByIds(listOf(generatedIds.templateId))
        val expected = TEST_TEMPLATE.copy(
            templateId = generatedIds.templateId,
            right = TEST_RIGHT.copy(rightId = generatedIds.rightId)
        )
        // then
        assertThat(
            receivedTemplates.first().toString(),
            `is`(expected.toString())
        )

        // Case: Update
        // when
        val expectedUpdated = TEST_TEMPLATE.copy(
            templateId = generatedIds.templateId,
            description = "fooo",
            right = expected.right.copy(licenceContract = "bar")
        )
        val updatedNumber: Int = templateDB.updateTemplateById(
            generatedIds.templateId, expectedUpdated
        )
        // then
        assertThat(updatedNumber, `is`(1))
        assertThat(
            templateDB.getTemplatesByIds(listOf(generatedIds.templateId)).first().toString(),
            `is`(expectedUpdated.toString())
        )

        // Case: Delete
        // when
        val countDeleted = templateDB.deleteTemplateById(generatedIds.templateId)
        // then
        assertThat(countDeleted, `is`(1))
        assertThat(
            templateDB.getTemplatesByIds(listOf(generatedIds.templateId)),
            `is`(emptyList())
        )
    }

    @Test
    fun testTemplateGetList() {
        templateDB.insertTemplate(TEST_TEMPLATE.copy(templateName = "aa"))
        templateDB.insertTemplate(TEST_TEMPLATE.copy(templateName = "ab"))
        templateDB.insertTemplate(TEST_TEMPLATE.copy(templateName = "ac"))
        val ids4 = templateDB.insertTemplate(TEST_TEMPLATE.copy(templateName = "ad"))
        val ids5 = templateDB.insertTemplate(TEST_TEMPLATE.copy(templateName = "ae"))
        val expected = listOf(
            TEST_TEMPLATE.copy(
                templateName = "ad",
                templateId = ids4.templateId,
                right = TEST_RIGHT.copy(rightId = ids4.rightId)
            ),
            TEST_TEMPLATE.copy(
                templateName = "ae",
                templateId = ids5.templateId,
                right = TEST_RIGHT.copy(rightId = ids5.rightId)
            ),
        )

        val received: List<Template> = templateDB.getTemplateList(2, 3)
        assertThat(
            received,
            `is`(expected),
        )
    }

    @Test
    fun testTemplateBookmarkPairRoundtrip() {
        // Create a Bookmark and Template
        val templateId = templateDB.insertTemplate(TEST_TEMPLATE).templateId
        val bookmarkId = bookmarkDB.insertBookmark(TEST_BOOKMARK)

        // Create Entry in Pair column
        templateDB.insertTemplateBookmarkPair(
            BookmarkTemplate(
                templateId = templateId,
                bookmarkId = bookmarkId,
            )
        )

        // Query Table
        assertThat(
            templateDB.getBookmarkIdsByTemplateId(templateId),
            `is`(listOf(bookmarkId))
        )

        // Delete Pair
        assertThat(
            templateDB.deleteTemplateBookmarkPair(
                BookmarkTemplate(
                    templateId = templateId,
                    bookmarkId = bookmarkId,
                )
            ),
            `is`(1)
        )
        assertThat(
            templateDB.getBookmarkIdsByTemplateId(templateId),
            `is`(emptyList())
        )

        // Delete Template and Bookmark
        templateDB.deleteTemplateById(templateId)
        bookmarkDB.deleteBookmarkById(bookmarkId)
    }

    @Test
    fun testBatchInsertOperation() {
        // Initial insert
        val templateId1 = templateDB.insertTemplate(TEST_TEMPLATE).templateId
        val bookmarkId1 = bookmarkDB.insertBookmark(TEST_BOOKMARK)

        val templateId2 = templateDB.insertTemplate(TEST_TEMPLATE).templateId
        val bookmarkId2 = bookmarkDB.insertBookmark(TEST_BOOKMARK)

        val given = listOf(
            BookmarkTemplate(
                templateId = templateId1,
                bookmarkId = bookmarkId1,
            ),
            BookmarkTemplate(
                templateId = templateId2,
                bookmarkId = bookmarkId2,
            ),
        )
        val received1: Int = templateDB.upsertTemplateBookmarkBatch(given)
        assertThat(
            received1,
            `is`(given.size),
        )

        // Upsert entries
        val templateId3 = templateDB.insertTemplate(TEST_TEMPLATE).templateId
        val bookmarkId3 = bookmarkDB.insertBookmark(TEST_BOOKMARK)
        val given2: List<BookmarkTemplate> = given +
            listOf(
                BookmarkTemplate(
                    templateId = templateId3,
                    bookmarkId = bookmarkId3,
                )
            )
        val received2: Int = templateDB.upsertTemplateBookmarkBatch(given2)
        assertThat(
            received2,
            `is`(given2.size - given.size),
        )
    }

    companion object {
        val TEST_TEMPLATE = Template(
            templateId = 1,
            templateName = "test",
            description = "some description",
            right = TEST_RIGHT
        )
    }
}
