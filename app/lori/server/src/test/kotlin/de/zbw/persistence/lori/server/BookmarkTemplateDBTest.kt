package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.RightFilterTest
import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.persistence.lori.server.BookmarkDBTest.Companion.TEST_BOOKMARK
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
 * Testing [BookmarkTemplateDB].
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkTemplateDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("foo"),
        )
    private val bookmarkTemplateDB = dbConnector.bookmarkTemplateDB
    private val bookmarkDB = dbConnector.bookmarkDB
    private val rightDB = dbConnector.rightDB

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
    fun testTemplateBookmarkPairRoundtrip() {
        // Create a Bookmark and Template
        val rightId = rightDB.insertRight(TEST_RIGHT)
        val bookmarkId = bookmarkDB.insertBookmark(TEST_BOOKMARK)

        // Create Entry in Pair column
        bookmarkTemplateDB.insertTemplateBookmarkPair(
            BookmarkTemplate(
                rightId = rightId,
                bookmarkId = bookmarkId,
            ),
        )

        // Query Table
        assertThat(
            bookmarkTemplateDB.getBookmarkIdsByRightId(rightId),
            `is`(listOf(bookmarkId)),
        )

        // Delete Pair
        assertThat(
            bookmarkTemplateDB.deleteTemplateBookmarkPair(
                BookmarkTemplate(
                    rightId = rightId,
                    bookmarkId = bookmarkId,
                ),
            ),
            `is`(1),
        )
        assertThat(
            bookmarkTemplateDB.getBookmarkIdsByRightId(rightId),
            `is`(emptyList()),
        )

        // Insert second bookmark  and test if deleting by template id works
        val bookmarkId2 = bookmarkDB.insertBookmark(TEST_BOOKMARK)
        bookmarkTemplateDB.insertTemplateBookmarkPair(
            BookmarkTemplate(
                rightId = rightId,
                bookmarkId = bookmarkId,
            ),
        )
        bookmarkTemplateDB.insertTemplateBookmarkPair(
            BookmarkTemplate(
                rightId = rightId,
                bookmarkId = bookmarkId2,
            ),
        )
        assertThat(
            bookmarkTemplateDB.getBookmarkIdsByRightId(rightId),
            `is`(listOf(bookmarkId, bookmarkId2)),
        )

        assertThat(
            bookmarkTemplateDB.getRightIdsByBookmarkId(bookmarkId),
            `is`(listOf(rightId)),
        )

        val deleted = bookmarkTemplateDB.deletePairsByRightId(rightId)
        assertThat(
            deleted,
            `is`(2),
        )

        assertThat(
            bookmarkTemplateDB.getBookmarkIdsByRightId(rightId),
            `is`(emptyList()),
        )

        assertThat(
            bookmarkTemplateDB.getRightIdsByBookmarkId(bookmarkId),
            `is`(emptyList()),
        )

        // Delete Template and Bookmark
        rightDB.deleteRightsByIds(listOf(rightId))
        bookmarkDB.deleteBookmarkById(bookmarkId)
    }

    companion object {
        val TEST_RIGHT = RightFilterTest.TEST_RIGHT
    }
}
