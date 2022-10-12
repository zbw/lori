package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseConnectorTest
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Testing filters concerning rights.
 *
 * Created on 10-12-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightFilterTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private val itemRightRestricted = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "restricted right",
        collectionName = "subject1 subject2",
        publicationType = PublicationType.PROCEEDINGS,
    )
    private val itemRightRestrictedOpen = DatabaseConnectorTest.TEST_Metadata.copy(
        metadataId = "restricted and open right",
        collectionName = "subject3",
        publicationType = PublicationType.PROCEEDINGS,
    )

    private fun getInitialMetadata(): Map<ItemMetadata, List<ItemRight>> = mapOf(
        itemRightRestricted to listOf(TEST_RIGHT.copy(accessState = AccessState.RESTRICTED)),
        itemRightRestrictedOpen to listOf(
            TEST_RIGHT.copy(accessState = AccessState.RESTRICTED),
            TEST_RIGHT.copy(accessState = AccessState.OPEN),
        ),
    )

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns DatabaseConnectorTest.NOW.toInstant()
        getInitialMetadata().forEach { entry ->
            backend.insertMetadataElement(entry.key)
            entry.value.forEach { right ->
                val r = backend.insertRight(right)
                backend.insertItemEntry(entry.key.metadataId, r)
            }
        }
    }

    @DataProvider(name = DATA_FOR_SEARCH_WITH_RIGHT_FILTER)
    fun createDataForSearchWithRightFilter() = arrayOf(
        arrayOf(
            "col:subject1",
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.RESTRICTED)),
            ),
            setOf(itemRightRestricted),
            1,
            "Filter for Access State Restricted for Item that has only one right"
        ),
        arrayOf(
            "col:subject1",
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.OPEN)),
            ),
            emptySet<ItemMetadata>(),
            0,
            "Filter for Access State Open and expect no result",
        ),
        arrayOf(
            "col:subject3",
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.OPEN)),
            ),
            setOf(itemRightRestrictedOpen),
            1,
            "Filter for Access State Restricted for item that has multiple items",
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
        val searchResult: Pair<Int, List<Item>> = backend.searchQuery(
            givenSearchTerm,
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.second.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
        assertThat(
            searchResult.first,
            `is`(
                expectedNumberOfResults
            ),
        )
    }

    @DataProvider(name = DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER)
    fun createDataForGetItemWithRightFilter() = arrayOf(
        arrayOf(
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED, AccessState.RESTRICTED)),
            ),
            setOf(itemRightRestricted, itemRightRestrictedOpen),
            "Filter for all access states"
        ),
        arrayOf(
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.RESTRICTED)),
            ),
            setOf(itemRightRestricted, itemRightRestrictedOpen),
            "Filter for Access State Restricted"
        ),
        arrayOf(
            listOf(
                PublicationTypeFilter(
                    listOf(
                        PublicationType.PROCEEDINGS,
                    )
                ),
            ),
            listOf(
                AccessStateFilter(listOf(AccessState.OPEN)),
            ),
            setOf(itemRightRestrictedOpen),
            "Filter for Access State Open for Item that has only one right"
        ),
    )

    @Test(dataProvider = DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER)
    fun testGetItemWithRightFilter(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        // when
        val searchResult: List<Item> = backend.getItemList(
            10,
            0,
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            description,
            searchResult.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )

        // when
        val numberOfResults = backend.countMetadataEntries(
            metadataSearchFilter,
            rightsSearchFilter,
        )

        // then
        assertThat(
            "Expected number of results does not match",
            numberOfResults,
            `is`(expectedResult.size)
        )
    }

    companion object {
        const val DATA_FOR_SEARCH_WITH_RIGHT_FILTER = "DATA_FOR_SEARCH_WITH_RIGHT_FILTER"
        const val DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER = "DATA_FOR_GET_ITEM_WITH_RIGHT_FILTER"

        val TEST_RIGHT = ItemRight(
            rightId = "123",
            accessState = AccessState.CLOSED,
            authorRightException = true,
            basisAccessState = BasisAccessState.LICENCE_CONTRACT,
            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
            createdBy = "user1",
            createdOn = OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            ),
            endDate = RestConverterTest.TODAY,
            lastUpdatedBy = "user2",
            lastUpdatedOn = OffsetDateTime.of(
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
            nonStandardOpenContentLicence = true,
            nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
            notesGeneral = "Some general notes",
            notesFormalRules = "Some formal rule notes",
            notesProcessDocumentation = "Some process documentation",
            notesManagementRelated = "Some management related notes",
            openContentLicence = "some licence",
            restrictedOpenContentLicence = false,
            zbwUserAgreement = true,
        )
    }
}
