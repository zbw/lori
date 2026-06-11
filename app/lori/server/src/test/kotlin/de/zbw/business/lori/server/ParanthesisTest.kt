package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.business.lori.server.type.SortInformation
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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Ensure that [SearchExpression]s are packed in paranthesis.
 *
 * Created on 12-04-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ParanthesisTest : DatabaseTest() {
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
            standardArticle to emptyList(),
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

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_TESTING_PARENTHESIS)
    fun createParenthesisTest() =
        arrayOf(
            arrayOf(
                "${FilterType.ZDB_ID.keyAlias}:'${ZDB_ID_UNUSED}' | ${FilterType.ZDB_ID.keyAlias}:'$ZDB_ID'",
                NoRightInformationFilter(),
                setOf(standardArticle),
                "Article should be found because no rights and zdb id",
            ),
            arrayOf(
                "${FilterType.ZDB_ID.keyAlias}:'${ZDB_ID_UNUSED}' | ${FilterType.ZDB_ID.keyAlias}:'$ZDB_ID' & ${FilterType.NO_RIGHTS.keyAlias}:on",
                null,
                setOf(standardArticle),
                "Article should not be found because no parantheses",
            ),
        )

    @Test(dataProvider = DATA_FOR_TESTING_PARENTHESIS)
    fun testParenthesis(
        searchTerm: String?,
        noRightInformationFilter: NoRightInformationFilter?,
        expectedMetadata: Set<ItemMetadata>,
        reason: String,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = searchTerm,
                    limit = 10,
                    offset = 0,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = emptyList(),
                    noRightInformationFilter = noRightInformationFilter,
                    sortInformation = SortInformation.DEFAULT,
                )
            }
        assertThat(
            reason,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedMetadata),
        )
    }

    companion object {
        const val ZDB_ID = "123456789"
        const val ZDB_ID_UNUSED = "12789"
        private val standardArticle =
            TEST_Metadata.copy(
                handle = "11159/3002",
                publicationType = PublicationType.ARTICLE,
                zdbIds = listOf(ZDB_ID),
            )

        const val DATA_FOR_TESTING_PARENTHESIS = "DATA_FOR_TESTING_PARENTHESIS"
    }
}
