package de.zbw.business.lori.server

import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.type.FormalRule
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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Testing [FormalRuleFilter]:
 *
 * Created on 15-05-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class FormalRuleFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val ccLicenceMetadata =
        TEST_Metadata.copy(
            handle = "CC licence",
            licenceUrlFilter = "by/3.0/igo/",
            licenceUrl = "https://creativecommons.org/licenses/by/3.0/igo/",
        )

    private val noLegalRiskMetadata =
        TEST_Metadata.copy(
            handle = "no legal risk",
            licenceUrlFilter = "blub",
            licenceUrl = "blub",
        )

    private val noRestrictedOCLRight =
        TEST_RIGHT.copy(
            restrictedOpenContentLicence = false,
            templateName = null,
        )

    private val noLegalRiskRight =
        TEST_RIGHT.copy(
            hasLegalRisk = false,
            templateName = null,
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            ccLicenceMetadata to listOf(noRestrictedOCLRight),
            noLegalRiskMetadata to listOf(noLegalRiskRight),
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

    @DataProvider(name = DATA_FOR_TESTING_CC_LICENCE_NO_RESTRICTION)
    fun createDataCCNoRestriction() =
        arrayOf(
            arrayOf(
                null,
                listOf(FormalRuleFilter(listOf(FormalRule.CC_LICENCE_NO_RESTRICTION))),
                setOf(ccLicenceMetadata),
                1,
                "search which will return item",
            ),
            arrayOf(
                "tit:fooobarrr",
                listOf(FormalRuleFilter(listOf(FormalRule.CC_LICENCE_NO_RESTRICTION))),
                emptySet<ItemMetadata>(),
                0,
                "search which will not return an item",
            ),
            arrayOf(
                "",
                emptyList<RightSearchFilter>(),
                setOf(ccLicenceMetadata, noLegalRiskMetadata),
                1,
                "Empty (default search)",
            ),
        )

    @Test(dataProvider = DATA_FOR_TESTING_CC_LICENCE_NO_RESTRICTION)
    fun testCCNoRestriction(
        searchTerm: String?,
        rightFilters: List<RightSearchFilter>,
        expectedMetadata: Set<ItemMetadata>,
        expectedFacet: Int,
        reason: String,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm,
                    10,
                    0,
                    emptyList(),
                    rightFilters,
                    null,
                )
            }
        assertThat(
            reason,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedMetadata),
        )
        assertThat(
            reason,
            searchResult.ccLicenceNoRestrictions,
            `is`(expectedFacet),
        )
    }

    @DataProvider(name = DATA_FOR_TESTING_LEGAL_RISK)
    fun createDataForLegalRisk() =
        arrayOf(
            arrayOf(
                null,
                listOf(FormalRuleFilter(listOf(FormalRule.COPYRIGHT_EXCEPTION_RISKFREE))),
                setOf(noLegalRiskMetadata),
                1,
                "search which will return item",
            ),
        )

    @Test(dataProvider = DATA_FOR_TESTING_LEGAL_RISK)
    fun testLegalRisk(
        searchTerm: String?,
        rightFilters: List<RightSearchFilter>,
        expectedMetadata: Set<ItemMetadata>,
        expectedFacet: Int,
        reason: String,
    ) {
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm,
                    10,
                    0,
                    emptyList(),
                    rightFilters,
                    null,
                )
            }
        assertThat(
            reason,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedMetadata),
        )
        assertThat(
            reason,
            searchResult.noLegalRisks,
            `is`(expectedFacet),
        )
    }

    companion object {
        const val DATA_FOR_TESTING_CC_LICENCE_NO_RESTRICTION = "DATA_FOR_TESTING_CC_LICENCE_NO_RESTRICTION"
        const val DATA_FOR_TESTING_LEGAL_RISK = "DATA_FOR_TESTING_LEGAL_RISK"
    }
}
