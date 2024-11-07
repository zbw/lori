package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.DashboardConflictTypeFilter
import de.zbw.business.lori.server.DashboardSearchFilter
import de.zbw.business.lori.server.DashboardTemplateNameFilter
import de.zbw.business.lori.server.DashboardTimeIntervalEndFilter
import de.zbw.business.lori.server.DashboardTimeIntervalStartFilter
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.RightError
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_RIGHT_ERROR
import de.zbw.persistence.lori.server.RightErrorDB.Companion.COLUMN_CONFLICTING_TYPE
import de.zbw.persistence.lori.server.RightErrorDB.Companion.COLUMN_CONFLICT_BY_CONTEXT
import de.zbw.persistence.lori.server.RightErrorDB.Companion.COLUMN_CREATED_ON
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterTest
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing [RightErrorDB].
 *
 * Created on 01-17-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightErrorDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).rightErrorDB

    @AfterTest
    fun afterTest() {
        unmockkAll()
    }

    @Test
    fun testRightErrorRoundtrip() =
        runBlocking {
            // Mock time
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            assertThat(
                dbConnector.getErrorList(10, 0).size,
                `is`(0),
            )
            val errorId = dbConnector.insertError(TEST_RIGHT_ERROR)
            val receivedError: RightError? = dbConnector.getErrorList(10, 0).firstOrNull()
            assertThat(
                receivedError?.toString() ?: "",
                `is`(TEST_RIGHT_ERROR.copy(errorId = errorId, createdOn = NOW).toString()),
            )

            // Delete by AGE
            dbConnector.deleteErrorsByAge(NOW.plusDays(1).toInstant())
            assertThat(
                dbConnector.getErrorList(10, 0).size,
                `is`(0),
            )
            // Delete by Id
            val errorId2 = dbConnector.insertError(TEST_RIGHT_ERROR)
            assertThat(
                dbConnector.getErrorList(10, 0).size,
                `is`(1),
            )
            dbConnector.deleteErrorById(errorId2)
            assertThat(
                dbConnector.getErrorList(10, 0).size,
                `is`(0),
            )

            // Test Deletion by Template/RightId causing the errors
            val templateError1 = TEST_RIGHT_ERROR.copy(conflictByRightId = "foo")
            val templateError2 = TEST_RIGHT_ERROR.copy(conflictByRightId = "bar")
            dbConnector.insertError(templateError1)
            val errorIdTemplate2 = dbConnector.insertError(templateError2)
            assertThat(
                dbConnector.getErrorList(10, 0).size,
                `is`(2),
            )
            dbConnector.deleteByCausingRightId(templateError1.conflictByRightId!!)
            assertThat(
                dbConnector.getErrorList(10, 0).size,
                `is`(1),
            )
            assertThat(
                dbConnector.getErrorList(10, 0).firstOrNull()?.toString() ?: "",
                `is`(templateError2.copy(errorId = errorIdTemplate2, createdOn = NOW).toString()),
            )
        }

    @DataProvider(name = DATA_FOR_BUILD_FILTER_QUERY)
    fun createDataForBuildFilterQuery() =
        arrayOf(
            arrayOf(
                emptyList<DashboardSearchFilter>(),
                "${RightErrorDB.STATEMENT_GET_RIGHT_LIST_SELECT} ORDER BY error_id LIMIT ? OFFSET ?;",
                "No filters selected",
            ),
            arrayOf(
                listOf(
                    DashboardTemplateNameFilter(listOf("templateName1", "templateName2")),
                    DashboardConflictTypeFilter(listOf(ConflictType.DATE_OVERLAP)),
                    DashboardTimeIntervalStartFilter(EXAMPLE_DATE),
                    DashboardTimeIntervalEndFilter(EXAMPLE_DATE.plusDays(30)),
                ),
                RightErrorDB.STATEMENT_GET_RIGHT_LIST_SELECT +
                    " WHERE ($COLUMN_CONFLICT_BY_CONTEXT = ? OR $COLUMN_CONFLICT_BY_CONTEXT = ?)" +
                    " AND ($COLUMN_CONFLICTING_TYPE = ?) AND ($COLUMN_CREATED_ON >= ?) AND ($COLUMN_CREATED_ON < ?)" +
                    " ORDER BY error_id LIMIT ? OFFSET ?;",
                "All filters",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_FILTER_QUERY)
    fun testBuildFilterQuery(
        filters: List<DashboardSearchFilter>,
        expectedQuery: String,
        reason: String,
    ) {
        assertThat(
            reason,
            RightErrorDB.buildFilterQuery(filters),
            `is`(expectedQuery),
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun createDataForBuildOccurrenceQuery() =
        arrayOf(
            arrayOf(
                COLUMN_CONFLICTING_TYPE,
                emptyList<DashboardSearchFilter>(),
                "SELECT $COLUMN_CONFLICTING_TYPE" +
                    " FROM $TABLE_NAME_RIGHT_ERROR" +
                    " GROUP BY $COLUMN_CONFLICTING_TYPE;",
                "No filters selected",
            ),
            arrayOf(
                COLUMN_CONFLICT_BY_CONTEXT,
                listOf(
                    DashboardTemplateNameFilter(listOf("templateName1", "templateName2")),
                    DashboardConflictTypeFilter(listOf(ConflictType.DATE_OVERLAP)),
                    DashboardTimeIntervalStartFilter(EXAMPLE_DATE),
                    DashboardTimeIntervalEndFilter(EXAMPLE_DATE.plusDays(30)),
                ),
                "SELECT $COLUMN_CONFLICT_BY_CONTEXT" +
                    " FROM $TABLE_NAME_RIGHT_ERROR" +
                    " WHERE ($COLUMN_CONFLICT_BY_CONTEXT = ? OR $COLUMN_CONFLICT_BY_CONTEXT = ?)" +
                    " AND ($COLUMN_CONFLICTING_TYPE = ?) AND ($COLUMN_CREATED_ON >= ?) AND ($COLUMN_CREATED_ON < ?)" +
                    " GROUP BY $COLUMN_CONFLICT_BY_CONTEXT;",
                "All filters",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_OCCURRENCE_QUERY)
    fun testBuildOccurrenceQuery(
        column: String,
        filters: List<DashboardSearchFilter>,
        expectedQuery: String,
        reason: String,
    ) {
        assertThat(
            reason,
            RightErrorDB.buildOccurrenceQuery(column, filters),
            `is`(expectedQuery),
        )
    }

    companion object {
        const val DATA_FOR_BUILD_FILTER_QUERY = "DATA_FOR_BUILD_FILTER_QUERY"
        const val DATA_FOR_BUILD_OCCURRENCE_QUERY = "DATA_FOR_BUILD_OCCURRENCE_QUERY"
        val NOW: OffsetDateTime =
            OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            )!!

        val TEST_RIGHT_ERROR =
            RightError(
                errorId = null,
                message = "Timing conflict",
                conflictingWithRightId = "sourceRightId",
                conflictByRightId = "conflictingRightId",
                handle = "somehandle",
                createdOn = NOW,
                conflictType = ConflictType.DATE_OVERLAP,
                conflictByContext = "template name",
            )
        val EXAMPLE_DATE: LocalDate =
            LocalDate.of(
                2022,
                3,
                1,
            )!!
    }
}
