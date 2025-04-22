package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
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
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing filters concerning rights.
 *
 * Created on 10-12-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightFilterTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk(),
        )

    private val itemRightRestricted =
        TEST_Metadata.copy(
            handle = "restricted right",
            collectionName = "subject1 subject2",
            publicationType = PublicationType.PROCEEDING,
        )
    private val itemRightRestrictedOpen =
        TEST_Metadata.copy(
            handle = "restricted and open right",
            collectionName = "subject3",
            publicationType = PublicationType.PROCEEDING,
        )
    private val tempValFilterPresent =
        TEST_Metadata.copy(
            handle = "validity filter present",
            collectionName = "validity",
        )

    private val tempValFilterPast =
        TEST_Metadata.copy(
            handle = "validity filter post",
            collectionName = "validity",
        )

    private val tempValFilterFuture =
        TEST_Metadata.copy(
            handle = "validity filter future",
            collectionName = "validity",
        )

    private val tempValFilterPastNoEnd =
        TEST_Metadata.copy(
            handle = "validity filter future no end",
            collectionName = "validity",
        )

    private val startEndDateFilter =
        TEST_Metadata.copy(
            handle = "start and end date At",
            collectionName = "startAndEnd",
        )

    private val formalRuleLicenceContract =
        TEST_Metadata.copy(
            handle = "formal rule filter licence contract",
            collectionName = "formalRuleLicence formal",
        )

    private val formalRuleUserAgreement =
        TEST_Metadata.copy(
            handle = "formal rule filter user agreement",
            collectionName = "formalRuleUserAgreement formal",
        )

    private val formalRuleOCL =
        TEST_Metadata.copy(
            handle = "formal rule filter ocl",
            collectionName = "ocl formal",
        )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> =
        mapOf(
            itemRightRestricted to
                listOf(
                    TEST_RIGHT.copy(
                        accessState = AccessState.RESTRICTED,
                    ),
                ),
            itemRightRestrictedOpen to
                listOf(
                    TEST_RIGHT.copy(
                        accessState = AccessState.RESTRICTED,
                        startDate = LocalDate.of(2025, 6, 1),
                        endDate = LocalDate.of(2025, 9, 1),
                        isTemplate = false,
                        templateName = null,
                    ),
                    TEST_RIGHT.copy(
                        accessState = AccessState.OPEN,
                        startDate = LocalDate.of(2024, 6, 1),
                        endDate = LocalDate.of(2024, 9, 1),
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            tempValFilterPresent to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2021, 6, 1),
                        endDate = LocalDate.of(2021, 9, 1),
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            tempValFilterPast to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2021, 2, 1),
                        endDate = LocalDate.of(2021, 3, 1),
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            tempValFilterFuture to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2021, 10, 1),
                        endDate = LocalDate.of(2021, 12, 1),
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            tempValFilterPastNoEnd to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2018, 10, 1),
                        endDate = null,
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            startEndDateFilter to
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2000, 10, 1),
                        endDate = LocalDate.of(2000, 12, 1),
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            formalRuleLicenceContract to
                listOf(
                    TEST_RIGHT.copy(
                        licenceContract = "licence",
                        zbwUserAgreement = false,
                        isTemplate = false,
                        templateName = null,
                    ),
                ),
            formalRuleUserAgreement to
                listOf(
                    TEST_RIGHT.copy(
                        zbwUserAgreement = true,
                        licenceContract = null,
                        isTemplate = false,
                        templateName = null,
                        restrictedOpenContentLicence = true,
                    ),
                ),
            formalRuleOCL to
                listOf(
                    TEST_RIGHT.copy(
                        licenceContract = null,
                        zbwUserAgreement = false,
                        isTemplate = false,
                        templateName = null,
                        restrictedOpenContentLicence = true,
                    ),
                ),
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

    @DataProvider(name = DATA_FOR_SEARCH_WITH_RIGHT_FILTER)
    fun createDataForSearchWithRightFilter() =
        arrayOf(
            arrayOf(
                "col:'subject1' | col:'subject3'",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.RESTRICTED)),
                ),
                setOf(itemRightRestricted, itemRightRestrictedOpen),
                2,
                "Filter for Access State Restricted for Item that has only one right",
            ),
            arrayOf(
                "col:subject3",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN)),
                ),
                setOf(itemRightRestrictedOpen),
                1,
                "Filter for Access State Open and expect one result with a similar collection name",
            ),
            arrayOf(
                "col:subject3",
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN)),
                ),
                setOf(itemRightRestrictedOpen),
                1,
                "Filter for Access State Restricted for item that has multiple items",
            ),
            arrayOf(
                "col:startAndEnd",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    StartDateFilter(
                        LocalDate.of(2000, 10, 1),
                    ),
                ),
                setOf(startEndDateFilter),
                1,
                "Filter for Start Date",
            ),
            arrayOf(
                "col:startAndEnd & zgb:2000-10-01",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                setOf(startEndDateFilter),
                1,
                "Filter for Start Date search upper",
            ),
            arrayOf(
                "col:startAndEnd",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    EndDateFilter(
                        LocalDate.of(2000, 12, 1),
                    ),
                ),
                setOf(startEndDateFilter),
                1,
                "Filter for End Date",
            ),
            arrayOf(
                "col:startAndEnd & zge:2000-12-01",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                setOf(startEndDateFilter),
                1,
                "Filter for End Date search upper",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_WITH_RIGHT_FILTER)
    fun testSearchWithRightFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        expectedNumberOfResults: Int,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    givenSearchTerm,
                    10,
                    0,
                    metadataSearchFilter,
                    rightsSearchFilter,
                )
            }

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            searchResult.numberOfResults,
            `is`(
                expectedNumberOfResults,
            ),
        )
    }

    @DataProvider(name = DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER)
    fun createDataForGetItemWithRightFilter() =
        arrayOf(
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED, AccessState.RESTRICTED)),
                ),
                setOf(itemRightRestricted, itemRightRestrictedOpen),
                "Filter for all access states",
            ),
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.RESTRICTED)),
                ),
                setOf(itemRightRestricted, itemRightRestrictedOpen),
                "Filter for Access State Restricted",
            ),
            arrayOf(
                listOf(
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.PROCEEDING,
                        ),
                    ),
                ),
                listOf(
                    AccessStateFilter(listOf(AccessState.OPEN)),
                ),
                setOf(itemRightRestrictedOpen),
                "Filter for Access State Open for Item that has only one right",
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    StartDateFilter(
                        LocalDate.of(2000, 10, 1),
                    ),
                ),
                setOf(startEndDateFilter),
                "Filter for Start Date",
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(
                    EndDateFilter(
                        LocalDate.of(2000, 12, 1),
                    ),
                ),
                setOf(startEndDateFilter),
                "Filter for End Date",
            ),
        )

    @Test(dataProvider = DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER)
    fun testRightFilterWithoutSearchTerm(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    null,
                    10,
                    0,
                    metadataSearchFilter,
                    rightsSearchFilter,
                )
            }

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size),
        )
    }

    @DataProvider(name = DATA_FOR_SEARCH_TEMP_VAL_FILTER)
    fun createDataForSearchTemValFilter() =
        arrayOf(
            arrayOf(
                "col:validity",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    RightValidOnFilter(
                        date = LocalDate.of(2021, 10, 1),
                    ),
                ),
                setOf(tempValFilterFuture, tempValFilterPastNoEnd),
                "Filter for items having an active right information at a certain point in time",
            ),
            arrayOf(
                "col:validity",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    RightValidOnFilter(
                        date = LocalDate.of(2018, 10, 1),
                    ),
                ),
                setOf(tempValFilterPastNoEnd),
                "Filter for items having an active right information at a certain point in time and no end date",
            ),
            arrayOf(
                "col:validity & zgp:2021-10-01",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                setOf(tempValFilterFuture, tempValFilterPastNoEnd),
                "use filter in upper search bar",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_TEMP_VAL_FILTER)
    fun testSearchTemporalValidityFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    givenSearchTerm,
                    10,
                    0,
                    metadataSearchFilter,
                    rightsSearchFilter,
                )
            }

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size),
        )
    }

    @DataProvider(name = DATA_FOR_SEARCH_FORMAL_RULE_FILTER)
    fun createDataForFormalRuleFilterTest() =
        arrayOf(
            arrayOf(
                "col:formalRuleLicence",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(FormalRule.LICENCE_CONTRACT),
                    ),
                ),
                1,
                setOf(formalRuleLicenceContract),
                "formal rule licence contract",
            ),
            arrayOf(
                "col:formalRuleLicence & reg:lizenzvertrag",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                1,
                setOf(formalRuleLicenceContract),
                "formal rule licence contract with upper search bar",
            ),
            arrayOf(
                "col:formalRuleUserAgreement",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(FormalRule.ZBW_USER_AGREEMENT),
                    ),
                ),
                1,
                setOf(formalRuleUserAgreement),
                "formal rule zbw agreement",
            ),
            arrayOf(
                "col:formalRuleUserAgreement & reg:Open-Content-License",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                1,
                setOf(formalRuleUserAgreement),
                "formal rule zbw agreement with restricted open content licence",
            ),
            arrayOf(
                "col:ocl",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules = listOf(FormalRule.OPEN_CONTENT_LICENCE),
                    ),
                ),
                1,
                setOf(formalRuleOCL),
                "formal rule ocl",
            ),
            arrayOf(
                "col:ocl & reg:Open-Content-License",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                1,
                setOf(formalRuleOCL),
                "formal rule ocl with upper search bar",
            ),
            arrayOf(
                "col:formal",
                emptyList<MetadataSearchFilter>(),
                listOf(
                    FormalRuleFilter(
                        formalRules =
                            listOf(
                                FormalRule.OPEN_CONTENT_LICENCE,
                                FormalRule.LICENCE_CONTRACT,
                                FormalRule.ZBW_USER_AGREEMENT,
                            ),
                    ),
                ),
                3,
                setOf(formalRuleOCL, formalRuleUserAgreement, formalRuleLicenceContract),
                "formal rule all",
            ),
            arrayOf(
                "col:formal & (reg:Open-Content-License | reg:ZBW-Nutzungsvereinbarung | reg:Lizenzvertrag)",
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                3,
                setOf(formalRuleOCL, formalRuleUserAgreement, formalRuleLicenceContract),
                "formal rule all with upper search bar",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_FORMAL_RULE_FILTER)
    fun testSearchFormalRuleFilter(
        givenSearchTerm: String,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        numberOfResults: Int,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    givenSearchTerm,
                    10,
                    0,
                    metadataSearchFilter,
                    rightsSearchFilter,
                )
            }

        // then
        assertThat(
            description,
            searchResult.results.size,
            `is`(numberOfResults),
        )

        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        assertThat(
            "Expected number of results does not match",
            searchResult.numberOfResults,
            `is`(expectedResult.size),
        )
    }

    @DataProvider(name = DATA_FOR_ACCESS_STATE)
    fun createDataForAccessState() =
        arrayOf(
            arrayOf(
                "acc:'Open'",
                emptyList<RightSearchFilter>(),
                listOf(itemRightRestrictedOpen).toSet(),
                "Simple search bar",
            ),
            arrayOf(
                "acc:'Open' | acc:'Restricted'",
                emptyList<RightSearchFilter>(),
                listOf(itemRightRestrictedOpen, itemRightRestricted).toSet(),
                "OR search bar",
            ),
            arrayOf(
                "!acc:'closed'",
                emptyList<RightSearchFilter>(),
                listOf(itemRightRestrictedOpen, itemRightRestricted).toSet(),
                "Negation search bar",
            ),
        )

    @Test(dataProvider = DATA_FOR_ACCESS_STATE)
    fun testFilterAccess(
        searchTerm: String,
        searchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: SearchQueryResult =
            runBlocking {
                backend.searchQuery(
                    searchTerm = searchTerm,
                    limit = 10,
                    offset = 0,
                    rightSearchFilter = searchFilter,
                )
            }

        // then
        assertThat(
            description,
            searchResult.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    companion object {
        const val DATA_FOR_ACCESS_STATE = "DATA_FOR_ACCESS_STATE"
        const val DATA_FOR_SEARCH_WITH_RIGHT_FILTER = "DATA_FOR_SEARCH_WITH_RIGHT_FILTER"
        const val DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER = "DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER"
        const val DATA_FOR_SEARCH_TEMP_VAL_FILTER = "DATA_FOR_SEARCH_TEMP_VAL_FILTER"
        const val DATA_FOR_SEARCH_FORMAL_RULE_FILTER = "DATA_FOR_SEARCH_FORMAL_RULE_FILTER"

        val TEST_RIGHT =
            ItemRight(
                rightId = "123",
                accessState = AccessState.CLOSED,
                authorRightException = true,
                basisAccessState = BasisAccessState.LICENCE_CONTRACT,
                basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
                createdBy = "user1",
                createdOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        1,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                hasLegalRisk = true,
                endDate = RestConverterTest.TODAY,
                exceptionOfId = null,
                hasExceptionId = null,
                isTemplate = false,
                lastAppliedOn =
                    OffsetDateTime.of(
                        2022,
                        5,
                        4,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                lastUpdatedBy = "user2",
                lastUpdatedOn =
                    OffsetDateTime.of(
                        2022,
                        3,
                        2,
                        1,
                        1,
                        0,
                        0,
                        ZoneOffset.UTC,
                    ),
                startDate = RestConverterTest.TODAY.minusDays(1),
                licenceContract = "some contract",
                notesGeneral = "Some general notes",
                notesFormalRules = "Some formal rule notes",
                notesProcessDocumentation = "Some process documentation",
                notesManagementRelated = "Some management related notes",
                restrictedOpenContentLicence = false,
                zbwUserAgreement = true,
                templateDescription = "some description",
                templateName = "exampleTemplate",
                groups = null,
                groupIds = emptyList(),
            )
    }
}
