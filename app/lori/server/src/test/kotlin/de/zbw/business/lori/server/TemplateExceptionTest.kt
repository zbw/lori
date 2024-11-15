package de.zbw.business.lori.server

import de.zbw.business.lori.server.ApplyTemplateTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.ZDB_2
import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.Test
import kotlin.test.assertNotNull

class TemplateExceptionTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer(""),
            ),
            mockk(),
        )

    @Test
    fun testExceptionRoundTrip() =
        runBlocking {
            // Insert two metadata entries
            val handle1 = backend.insertMetadataElement(item1ZDB2)
            val handle2 = backend.insertMetadataElement(item2ZDB2)

            // Create Bookmarks
            val bookmarkIdUpper =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "allZDB2",
                        bookmarkId = 10,
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_2,
                                    ),
                            ),
                    ),
                )

            val bookmarkIdException =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "zdb2AndHandle",
                        bookmarkId = 20,
                        zdbIdFilter =
                            ZDBIdFilter(
                                zdbIds =
                                    listOf(
                                        ZDB_2,
                                    ),
                            ),
                        searchTerm = "hdl:bar",
                    ),
                )

            // Create Templates
            val rightIdUpper =
                backend.insertTemplate(TEST_RIGHT.copy(templateName = "upper", isTemplate = true))

            // Connect Bookmarks and Templates
            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdUpper,
                rightId = rightIdUpper,
            )

            val rightIdException =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "exception",
                        isTemplate = true,
                        exceptionFrom = rightIdUpper,
                    ),
                )

            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkIdException,
                rightId = rightIdException,
            )

            // Connect Metadata with Templates -> Usually done via Apply, but we keep it simple here
            backend.insertItemEntry(handle = handle1, rightIdUpper)
            backend.insertItemEntry(handle = handle2, rightIdException)

            // Delete Rights -> All foreign key connections need to be removed
            backend.deleteRight(rightIdUpper)

            // Then
            assertNotNull(
                backend.getBookmarkById(bookmarkIdException),
            )

            assertNotNull(
                backend.getBookmarkById(bookmarkIdUpper),
            )
            assertThat(
                backend.getRightsByIds(listOf(rightIdUpper, rightIdException)),
                `is`(emptyList()),
            )
        }

    companion object {
        val item1ZDB2 =
            TEST_METADATA.copy(
                handle = "foo-zdb2",
                zdbIdJournal = ZDB_2,
            )
        val item2ZDB2 =
            TEST_METADATA.copy(
                handle = "bar-zdb2",
                zdbIdJournal = ZDB_2,
            )
    }
}
