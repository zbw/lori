package de.zbw.business.lori.server

import de.zbw.business.lori.server.ApplyTemplateTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.item1ZDB1
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.item1ZDB2
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.item2ZDB2
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Testing error detection.
 *
 * Created on 11-05-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class FindErrorsTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                batchConnectionPool = ConnectionPool(testDataSource),
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            item1ZDB1 to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2000, 1, 1),
                        endDate = LocalDate.of(2000, 12, 31),
                        templateName = null,
                    ),
                ),
            item1ZDB2 to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2000, 9, 1),
                        endDate = LocalDate.of(2000, 9, 30),
                        templateName = null,
                    ),
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2000, 10, 15),
                        endDate = LocalDate.of(2000, 10, 31),
                        templateName = null,
                    ),
                ),
            item2ZDB2 to emptyList(),
            item2ZDB2.copy(handle = "11159/200") to emptyList(),
            item2ZDB2.copy(handle = "11159/201", deleted = true) to emptyList(),
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns ItemDBTest.NOW.toInstant()
            getInitialMetadata().forEach { entry ->
                backend.insertMetadataElement(entry.key)
                entry.value.forEach { right ->
                    val r = backend.insertRight(right)
                    backend.insertItemEntry(entry.key.handle, r)
                }
            }
        }

    @Test
    fun testFindGapErrors() =
        runBlocking {
            assertThat(
                backend.checkForNoRightErrors("user1"),
                `is`(2),
            )
            val receivedErrors = backend.checkForRightErrors("user1")
            assertThat(
                receivedErrors,
                `is`(5),
            )
            assertThat(
                backend.checkForGAPErrors("user1"),
                `is`(2),
            )

            val dbErrors =
                backend.getRightErrorList(
                    limit = 10,
                    offset = 0,
                    searchFilters = emptyList(),
                    testId = null,
                )
            assertThat(
                dbErrors.results.size,
                `is`(5),
            )

            // Second execution
            backend.checkForRightErrors("user1")
            assertThat(
                "Old entries are deleted beforehand",
                backend
                    .getRightErrorList(
                        limit = 10,
                        offset = 0,
                        searchFilters = emptyList(),
                        testId = null,
                    ).results.size,
                `is`(5),
            )
        }
}
