package de.zbw.business.lori.server

import de.zbw.api.lori.server.exception.ResourceConflictException
import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.api.lori.server.type.RestConverterTest.Companion.TEST_BOOKMARK
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

/**
 * Inserting or updating bookmarks can cause conflicts if another search
 * with the exact same filters already exists.
 *
 * Created on 01-30-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkConflictTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    @AfterMethod
    fun cleanDB() =
        runBlocking {
            val ids = backend.getBookmarkList(100, 0).map { it.bookmarkId }
            ids.forEach {
                backend.deleteBookmark(it)
            }
        }

    @Test(expectedExceptions = [ResourceConflictException::class])
    fun conflictInsertion() =
        runBlocking {
            // Given
            backend.insertBookmark(TEST_BOOKMARK.copy(bookmarkName = "testConflictInsertion"))
            // when
            backend.insertBookmark(
                TEST_BOOKMARK.copy(
                    bookmarkName = "will cause conflict",
                ),
            )

            // then -> should not be reached
            Assert.fail()
        }

    @Test(expectedExceptions = [ResourceConflictException::class])
    fun testConflictUpdate() =
        runBlocking {
            // Given
            val firstBookmark = TEST_BOOKMARK.copy(bookmarkName = "testConflictUpdate")
            val firstBookmarkId = backend.insertBookmark(firstBookmark)
            backend.insertBookmark(
                TEST_BOOKMARK.copy(
                    bookmarkName = "testUpdateTo",
                    accessStateFilter = QueryParameterParser.parseAccessStateFilter("CLOSED"),
                ),
            )
            // when
            backend.updateBookmark(
                firstBookmarkId,
                firstBookmark.copy(
                    accessStateFilter = QueryParameterParser.parseAccessStateFilter("CLOSED"),
                ),
            )

            // then -> should not be reached
            Assert.fail()
        }

    @Test
    fun testNoConflictOwnUpdate() =
        runBlocking {
            // Given
            val firstBookmark = TEST_BOOKMARK.copy(bookmarkName = "testConflictUpdate")
            val firstBookmarkId = backend.insertBookmark(firstBookmark)

            // when
            val numberOfUpdates =
                backend.updateBookmark(
                    firstBookmarkId,
                    firstBookmark.copy(
                        accessStateFilter = QueryParameterParser.parseAccessStateFilter("CLOSED"),
                    ),
                )

            // then
            assertThat(
                numberOfUpdates,
                `is`(1),
            )
        }
}
