package de.zbw.persistence.lori.server

import de.zbw.api.lori.server.route.QueryParameterParser
import de.zbw.api.lori.server.utils.RestConverterUtil
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test
import java.time.Instant

/**
 * Testing [BookmarkDB].
 *
 * Created on 03-16-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).bookmarkDB

    private fun mockkCurrentTime(instant: Instant) {
        mockkStatic(Instant::class)
        every { Instant.now() } returns instant
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun testBookmarkRoundtrip() =
        runBlocking {
            // Case: Create and Read
            val createTime = NOW.toInstant()
            mockkCurrentTime(createTime)
            // when
            val generatedId = dbConnector.insertBookmark(TEST_BOOKMARK.copy(createdBy = "user1", lastUpdatedBy = "user1"))
            val receivedBookmarks = dbConnector.getBookmarksByIds(listOf(generatedId))
            // then
            assertThat(
                receivedBookmarks.first().toString(),
                `is`(
                    TEST_BOOKMARK
                        .copy(
                            bookmarkId = generatedId,
                            createdBy = "user1",
                            createdOn = NOW,
                            lastUpdatedBy = "user1",
                            lastUpdatedOn = NOW,
                        ).toString(),
                ),
            )

            // Case: Update
            val updateTime = NOW.plusDays(1).toInstant()
            mockkCurrentTime(updateTime)
            val expectedBMUpdated =
                TEST_BOOKMARK.copy(
                    noRightInformationFilter = NoRightInformationFilter(),
                    lastUpdatedBy = "user2",
                    createdBy = "foobar",
                )
            val updatedBMs = dbConnector.updateBookmarkById(generatedId, expectedBMUpdated)
            assertThat(updatedBMs, `is`(1))
            assertThat(
                expectedBMUpdated
                    .copy(
                        bookmarkId = generatedId,
                        createdBy = "user1",
                        createdOn = NOW,
                        lastUpdatedBy = "user2",
                        lastUpdatedOn = NOW.plusDays(1),
                    ).toString(),
                `is`(
                    dbConnector.getBookmarksByIds(listOf(generatedId)).first().toString(),
                ),
            )
            // Case: Delete
            val countDeleted = dbConnector.deleteBookmarkById(generatedId)
            assertThat(countDeleted, `is`(1))
            val receivedBookmarksAfterDeletion = dbConnector.getBookmarksByIds(listOf(generatedId))
            // then
            assertThat(receivedBookmarksAfterDeletion, `is`(emptyList()))
        }

    @Test
    fun testGetBookmarkList() =
        runBlocking {
            val bookmark1 = TEST_BOOKMARK.copy(description = "foo")
            val createTime = NOW.toInstant()
            mockkCurrentTime(createTime)
            val receivedId1 = dbConnector.insertBookmark(bookmark1)
            val expected1 = bookmark1.copy(bookmarkId = receivedId1)
            assertThat(
                dbConnector.getBookmarkList(50, 0).toString(),
                `is`(
                    listOf(
                        expected1.copy(lastUpdatedOn = NOW, createdOn = NOW),
                    ).toString(),
                ),
            )

            val bookmark2 = TEST_BOOKMARK.copy(description = "bar")
            val receivedId2 = dbConnector.insertBookmark(bookmark2)
            val expected2 = bookmark2.copy(bookmarkId = receivedId2)

            assertThat(
                dbConnector.getBookmarkList(50, 0).toString(),
                `is`(
                    listOf(
                        expected1.copy(lastUpdatedOn = NOW, createdOn = NOW),
                        expected2.copy(lastUpdatedOn = NOW, createdOn = NOW),
                    ).toString(),
                ),
            )
        }

    companion object {
        val TEST_BOOKMARK =
            Bookmark(
                bookmarkId = 1,
                bookmarkName = "test",
                description = "some description",
                searchTerm = "tit:someTitle",
                publicationDateFilter = QueryParameterParser.parsePublicationDateFilter("2020-2030"),
                publicationTypeFilter = QueryParameterParser.parsePublicationTypeFilter("BOOK,ARTICLE"),
                accessStateFilter = QueryParameterParser.parseAccessStateFilter("OPEN,RESTRICTED"),
                temporalValidityFilter = QueryParameterParser.parseTemporalValidity("FUTURE,PAST"),
                validOnFilter = QueryParameterParser.parseRightValidOnFilter("2018-04-01"),
                startDateFilter = QueryParameterParser.parseStartDateFilter("2020-01-01"),
                endDateFilter = QueryParameterParser.parseEndDateFilter("2021-12-31"),
                formalRuleFilter = QueryParameterParser.parseFormalRuleFilter("ZBW_USER_AGREEMENT"),
                paketSigelFilter = QueryParameterParser.parsePaketSigelFilter("sigel"),
                zdbIdFilter = QueryParameterParser.parseZDBIdFilter("zdbId1,zdbId2"),
                noRightInformationFilter = QueryParameterParser.parseNoRightInformationFilter("false"),
                createdBy = null,
                createdOn = null,
                lastUpdatedBy = null,
                lastUpdatedOn = null,
                seriesFilter = QueryParameterParser.parseSeriesFilter("some series"),
                templateNameFilter = QueryParameterParser.parseTemplateNameFilter("some template"),
                licenceURLFilter =
                    QueryParameterParser.parseLicenceUrlFilter(
                        RestConverterUtil.prepareLicenceUrlFilter(
                            "http://creativecommons.org/licenses/by/3.0/au",
                        ),
                    ),
            )
    }
}
