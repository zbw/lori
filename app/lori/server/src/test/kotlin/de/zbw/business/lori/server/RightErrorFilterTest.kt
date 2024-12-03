package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.ErrorQueryResult
import de.zbw.business.lori.server.type.RightError
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.RightErrorDBTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant

class RightErrorFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private fun getErrorsConflictType(): List<RightError> =
        listOf(
            RIGHT_ERROR.copy(
                conflictByContext = "foo",
                handle = "ct_1",
                conflictType = ConflictType.UNSPECIFIED,
            ),
            RIGHT_ERROR.copy(
                conflictType = ConflictType.UNSPECIFIED,
                conflictByContext = "foo",
                handle = "ct_2",
            ),
        )

    private fun getErrorsCausedByTemplateName(): List<RightError> =
        listOf(
            RIGHT_ERROR.copy(
                conflictByContext = CAUSED_BY_TEMPLATE_NAME,
                handle = "tn_1",
            ),
            RIGHT_ERROR.copy(
                conflictByContext = CAUSED_BY_TEMPLATE_NAME,
                handle = "tn_2",
            ),
        )

    private fun getErrorsCreatedOn(): List<RightError> =
        listOf(
            RIGHT_ERROR_PAST.copy(
                handle = "ep_1",
            ),
            RIGHT_ERROR_PAST.copy(
                handle = "ep_2",
            ),
        )

    private fun getInitialErrors(): List<RightError> =
        listOf(
            getErrorsCausedByTemplateName(),
            getErrorsConflictType(),
            getErrorsCreatedOn(),
        ).flatten()

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            getInitialErrors().forEach {
                backend.dbConnector.rightErrorDB.insertError(it)
            }
        }

    @DataProvider(name = DATA_FOR_QUERY_ERRORS)
    fun createDataForQueryErrors() =
        arrayOf(
            arrayOf(
                listOf(
                    DashboardTemplateNameFilter(
                        listOf(
                            CAUSED_BY_TEMPLATE_NAME,
                        ),
                    ),
                ),
                ErrorQueryResult(
                    totalNumberOfResults = 2,
                    contextNames = setOf(CAUSED_BY_TEMPLATE_NAME),
                    conflictTypes = setOf(ConflictType.DATE_OVERLAP),
                    results = getErrorsCausedByTemplateName(),
                ),
                "Query for errors conflicted by same template",
            ),
            arrayOf(
                listOf(
                    DashboardConflictTypeFilter(
                        listOf(
                            ConflictType.UNSPECIFIED,
                        ),
                    ),
                ),
                ErrorQueryResult(
                    totalNumberOfResults = 2,
                    contextNames = setOf("foo"),
                    conflictTypes = setOf(ConflictType.UNSPECIFIED),
                    results = getErrorsConflictType(),
                ),
                "Query for errors of a specific conflict type",
            ),
            arrayOf(
                listOf(
                    DashboardTimeIntervalStartFilter(
                        RightErrorDBTest.NOW.minusDays(DAYS_PAST + 1).toLocalDate(),
                    ),
                    DashboardTimeIntervalEndFilter(
                        RightErrorDBTest.NOW.minusDays(1).toLocalDate(),
                    ),
                ),
                ErrorQueryResult(
                    totalNumberOfResults = 2,
                    contextNames = setOf(RIGHT_ERROR_PAST.conflictByContext!!),
                    conflictTypes = setOf(RIGHT_ERROR_PAST.conflictType),
                    results = getErrorsCreatedOn(),
                ),
                "Query for errors in a time interval",
            ),
            arrayOf(
                listOf(
                    DashboardTimeIntervalStartFilter(
                        RightErrorDBTest.NOW.minusDays(DAYS_PAST).toLocalDate(),
                    ),
                    DashboardTimeIntervalEndFilter(
                        RightErrorDBTest.NOW.minusDays(DAYS_PAST).toLocalDate(),
                    ),
                ),
                ErrorQueryResult(
                    totalNumberOfResults = 2,
                    contextNames = setOf(RIGHT_ERROR_PAST.conflictByContext!!),
                    conflictTypes = setOf(RIGHT_ERROR_PAST.conflictType),
                    results = getErrorsCreatedOn(),
                ),
                "Query for errors on one day",
            ),
        )

    @Test(dataProvider = DATA_FOR_QUERY_ERRORS)
    fun testQueryErrors(
        searchFilters: List<DashboardSearchFilter>,
        expected: ErrorQueryResult,
        reason: String,
    ) = runBlocking {
        val received: ErrorQueryResult =
            backend.getRightErrorList(
                limit = 100,
                offset = 0,
                searchFilters = searchFilters,
                testId = null,
            )
        assertThat(
            reason,
            received.totalNumberOfResults,
            `is`(expected.totalNumberOfResults),
        )
        assertThat(
            reason,
            received.conflictTypes,
            `is`(expected.conflictTypes),
        )
        assertThat(
            reason,
            received.contextNames,
            `is`(expected.contextNames),
        )
        assertThat(
            reason,
            received.results.map { it.copy(errorId = null) },
            `is`(expected.results),
        )
    }

    companion object {
        const val DATA_FOR_QUERY_ERRORS = "DATA_FOR_QUERY_ERRORS"
        const val CAUSED_BY_TEMPLATE_NAME = "Template 555nase"
        val RIGHT_ERROR =
            RightError(
                errorId = null,
                message = "Timing conflict",
                conflictingWithRightId = "sourceRightId",
                conflictByRightId = "conflictingRightId",
                handle = "somehandle",
                createdOn = RightErrorDBTest.NOW,
                conflictType = ConflictType.DATE_OVERLAP,
                conflictByContext = "template name",
                testId = null,
                createdBy = "user1",
            )
        const val DAYS_PAST = 45L
        val RIGHT_ERROR_PAST =
            RightError(
                errorId = null,
                message = "Timing conflict",
                conflictingWithRightId = "sourceRightId",
                conflictByRightId = "conflictingRightId",
                handle = "somehandle",
                createdOn = RightErrorDBTest.NOW.minusDays(DAYS_PAST),
                conflictType = ConflictType.DATE_OVERLAP,
                conflictByContext = "template name",
                testId = null,
                createdBy = "user1",
            )
    }
}
