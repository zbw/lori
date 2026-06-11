package de.zbw.persistence.lori.server

import StatisticsService
import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.FacetTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.FacetTest.Companion.itemZDB1
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

class StatisticsServiceTest : DatabaseTest() {
    val connectionPool = ConnectionPool(testDataSource)
    val statisticsService = StatisticsService(connectionPool, connectionPool, tracer = tracer)

    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = connectionPool,
                batchConnectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            itemZDB1 to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2000, 1, 1),
                        endDate = LocalDate.of(2000, 12, 31),
                        accessState = AccessState.OPEN,
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            itemNoRight to emptyList(),
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns LocalDate.of(2021, 7, 1)
            getInitialMetadata().forEach { entry ->
                backend.insertMetadataElement(entry.key)
                entry.value.forEach { right ->
                    val r = backend.insertRight(right)
                    backend.insertItemEntry(entry.key.handle, r)
                }
            }
        }

    @Test
    fun testGetStatisticsWithMetadataFilter() =
        runBlocking {
            val response =
                statisticsService.getStatisticsWithMetadataFilter(
                    searchExpression = null,
                    metadataSearchFilters =
                        listOf(
                            PublicationTypeFilter(
                                listOf(PublicationType.BOOK),
                            ),
                        ),
                )

            assertThat(
                response.metadataStats
                    .first { it.metric == "publication_type" }
                    .count,
                `is`(1),
            )
        }

    @Test
    fun testGetStatisticsWithRightFilter() =
        runBlocking {
            val response =
                statisticsService.getStatisticsWithRightsFilter(
                    searchExpression = null,
                    rightSearchFilters =
                        listOf(
                            AccessStateFilter(listOf(AccessState.OPEN)),
                        ),
                    noRightInformationFilter = null,
                )

            assertThat(
                response.metadataStats
                    .first { it.metric == "publication_type" }
                    .count,
                `is`(1),
            )
        }

    @Test
    fun testGetStatisticsWithBothFilter() =
        runBlocking {
            val response =
                statisticsService.getStatisticsWithBothFilters(
                    searchExpression = null,
                    rightSearchFilters =
                        listOf(
                            AccessStateFilter(listOf(AccessState.OPEN)),
                        ),
                    metadataSearchFilters =
                        listOf(
                            PublicationTypeFilter(
                                listOf(PublicationType.BOOK),
                            ),
                        ),
                    noRightInformationFilter = null,
                )

            assertThat(
                response.metadataStats
                    .first { it.metric == "publication_type" }
                    .count,
                `is`(1),
            )
        }

    @Test
    fun testGetStatisticsWithNoFilter() =
        runBlocking {
            val refreshResult =
                statisticsService.refreshStatisticsMaterializedViews()
            assertTrue(refreshResult.success)
            val response =
                statisticsService.getStatisticsNoFilter()

            assertThat(
                response.metadataStats
                    .first { it.metric == "publication_type" }
                    .count,
                `is`(1),
            )
        }

    @Test
    fun testGetStatisticsWithNoRightsFilter() =
        runBlocking {
            val response =
                statisticsService.getStatisticsWithoutItems(
                    searchExpression = null,
                    metadataSearchFilters =
                        listOf(
                            PublicationTypeFilter(
                                listOf(PublicationType.ARTICLE),
                            ),
                        ),
                )

            assertThat(
                response.metadataStats
                    .first { it.metric == "publication_type" }
                    .value,
                `is`("ARTICLE"),
            )
        }

    companion object {
        val itemNoRight =
            TEST_Metadata.copy(
                handle = "11159/77104",
                collectionName = "common zdb",
                zdbIds = emptyList(),
                publicationYear = 2010,
                publicationType = PublicationType.ARTICLE,
            )

        private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.StatisticsServiceTest")
    }
}
