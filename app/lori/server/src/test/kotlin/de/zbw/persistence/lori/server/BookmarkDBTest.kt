package de.zbw.persistence.lori.server

import de.zbw.lori.model.BookmarkRest
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [BookmarkDB].
 *
 * Created on 03-16-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkDBTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    ).bookmarkDB

    @Test
    fun testBookmarkRoundtrip() {
        // Case: Create and Read
        // when
        val generatedId = dbConnector.insertBookmark(TEST_BOOKMARK)
        val receivedBookmarks = dbConnector.getBookmarksByIds(listOf(generatedId))
        // then
        assertThat(receivedBookmarks.first(), `is`(TEST_BOOKMARK.copy(bookmarkId = generatedId)))

        // Case: Update
        val expectedBMUpdated = TEST_BOOKMARK.copy(filterNoRightInformation = true)
        val updatedBMs = dbConnector.updateBookmarksById(generatedId, expectedBMUpdated)
        assertThat(updatedBMs, `is`(1))
        assertThat(
            expectedBMUpdated.copy(bookmarkId = generatedId),
            `is`(
                dbConnector.getBookmarksByIds(listOf(generatedId)).first()
            )
        )
        // Case: Delete
        val countDeleted = dbConnector.deleteBookmarkById(generatedId)
        assertThat(countDeleted, `is`(1))
        val receivedBookmarksAfterDeletion = dbConnector.getBookmarksByIds(listOf(generatedId))
        // then
        assertThat(receivedBookmarksAfterDeletion, `is`(emptyList()))
    }

    companion object {
        val TEST_BOOKMARK = BookmarkRest(
            bookmarkId = 1,
            bookmarkName = "test",
            searchTerm = "tit:someTitle",
            filterPublicationDate = "BOOK,ARTICLE",
            filterAccessState = "OPEN,RESTRICTED",
            filterTemporalValidity = "True",
            filterStartDate = "2020-01-01",
            filterEndDate = "2021-12-31",
            filterFormalRule = "someRule",
            filterPaketSigel = "sigel",
            filterZDBId = "zdbId1,zdbId2",
            filterNoRightInformation = false,
        )
    }
}
