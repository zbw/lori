package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest
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
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant

/**
 * Test if logical operations in queries work as expected.
 *
 * Created on 08-09-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LogicalOperationQueriesTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LogicalOperationQueriesTest"),
            ),
            mockk(),
        )

    private val initialData =
        listOf(
            ItemDBTest.TEST_Metadata.copy(
                collectionName = "subject1",
                handle = "subject1",
                publicationType = PublicationType.PROCEEDING,
                publicationYear = 2022,
            ),
            ItemDBTest.TEST_Metadata.copy(
                collectionName = "subject2 subject3",
                handle = "subject2&3",
                publicationType = PublicationType.WORKING_PAPER,
                publicationYear = 2020,
            ),
            ItemDBTest.TEST_Metadata.copy(
                collectionName = "subject4",
                handle = "subject4",
                publicationType = PublicationType.WORKING_PAPER,
                publicationYear = 2020,
            ),
        )

    private fun getInitialMetadata() =
        listOf(
            initialData,
        ).flatten()

    @BeforeClass
    fun fillDB() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns ItemDBTest.NOW.toInstant()
            getInitialMetadata().forEach {
                backend.insertMetadataElement(it)
            }
        }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_LOGIC_OP)
    fun createDataForLogicalOperation() =
        arrayOf(
            arrayOf(
                "col:'subject1' | col:'subject4'",
                listOf(initialData[0], initialData[2]).toSet(),
                2,
                "Test simple OR operator",
            ),
            arrayOf(
                "(col:subject2 | col:subject4) & col:subject3",
                listOf(initialData[1]).toSet(),
                1,
                "Group with parentheses. Combine AND and OR operator",
            ),
            arrayOf(
                "(col:subject2 | col:subject4) & !col:subject3",
                listOf(initialData[2]).toSet(),
                1,
                "Test NOT operator",
            ),
        )

    @Test(dataProvider = DATA_FOR_LOGIC_OP)
    fun testLogicalOperationsInQueries(
        searchTerm: String,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val (numberOfResults, searchResult) =
            runBlocking {
                backend.searchQuery(
                    searchTerm,
                    10,
                    0,
                )
            }

        // then
        assertThat(
            description,
            searchResult.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            description,
            numberOfResults,
            `is`(expectedNumberOfResults),
        )
    }

    companion object {
        const val DATA_FOR_LOGIC_OP = "DATA_FOR_LOGIC_OP"
    }
}
