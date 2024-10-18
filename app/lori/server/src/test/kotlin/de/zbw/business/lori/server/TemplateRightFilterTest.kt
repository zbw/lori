package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Testing template filter.
 *
 * Created on 12-15-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class TemplateRightFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val itemRightWithTemplate =
        TEST_Metadata.copy(
            handle = "withTemplate",
            collectionName = "subject3",
        )
    private val itemRightWithoutTemplate =
        TEST_Metadata.copy(
            handle = "withoutTemplate",
            collectionName = "subject3",
        )

    private fun getInitialData(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            itemRightWithTemplate to
                listOf(
                    RightFilterTest.TEST_RIGHT.copy(isTemplate = true, templateName = "some template"),
                ),
            itemRightWithoutTemplate to
                listOf(
                    RightFilterTest.TEST_RIGHT.copy(isTemplate = false),
                ),
        )

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            mockkStatic(LocalDate::class)
            every { LocalDate.now() } returns LocalDate.of(2021, 7, 1)
            getInitialData().forEach { entry ->
                backend.insertMetadataElement(entry.key)
                entry.value.forEach { right ->
                    val r = backend.insertTemplate(right)
                    backend.insertItemEntry(entry.key.handle, r)
                }
            }
        }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testTemplateFilter() =
        runBlocking {
            val templateName: String = backend.getTemplateList(10, 0).first().templateName!!
            val templateNameFilter = listOf(TemplateNameFilter(listOf(templateName)))
            val searchResult: SearchQueryResult =
                runBlocking {
                    backend.searchQuery(
                        "col:subject3",
                        10,
                        0,
                        emptyList(),
                        templateNameFilter,
                    )
                }

            assertThat(
                searchResult.results.map { it.metadata }.toSet(),
                `is`(setOf(itemRightWithTemplate)),
            )
            val searchResult2: SearchQueryResult =
                runBlocking {
                    backend.searchQuery(
                        "col:subject3 & tpl:\"$templateName\"",
                        10,
                        0,
                        emptyList(),
                        emptyList(),
                    )
                }

            assertThat(
                searchResult2.results.map { it.metadata }.toSet(),
                `is`(setOf(itemRightWithTemplate)),
            )
        }
}
