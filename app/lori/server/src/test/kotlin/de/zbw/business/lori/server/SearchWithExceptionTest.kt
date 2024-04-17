package de.zbw.business.lori.server

import de.zbw.business.lori.server.RightFilterTest.Companion.TEST_RIGHT
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.SearchQueryResult
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.ItemDBTest.Companion.NOW
import de.zbw.persistence.lori.server.ItemDBTest.Companion.TEST_Metadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.LocalDate

class SearchWithExceptionTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
        ),
        mockk(),
    )

    private fun getInitialData(): Map<ItemMetadata, List<ItemRight>> = listOf(
        METADATA_NO_RIGHT_INFORMATION to emptyList(),
        METADATA_PUB_1999_OPEN to listOf(RIGHT_OPEN_1999),
        METADATA_PUB_1990_2000_OPEN to listOf(RIGHT_OPEN_1990_2000),
        METADATA_SIG_FOO_1 to emptyList(),
        METADATA_SIG_FOO_2 to emptyList(),
    ).toMap()

    @BeforeClass
    fun fillDB() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2021, 7, 1)
        getInitialData().forEach { entry ->
            backend.insertMetadataElement(entry.key)
            entry.value.forEach { right ->
                val r = backend.insertTemplate(right)
                backend.insertItemEntry(entry.key.metadataId, r)
            }
        }
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @DataProvider(name = DATA_FOR_SEARCH_WITH_EXCEPTIONS)
    fun createDataForSearchWithExceptions() =
        arrayOf(
            arrayOf(
                null,
                null,
                listOf(PublicationDateFilter(1990, 2000)),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                null,
                setOf(METADATA_PUB_1990_2000_OPEN, METADATA_PUB_1999_OPEN),
                "Publication dates between 1990-2000"
            ),
            arrayOf(
                null,
                null,
                listOf(PublicationDateFilter(1990, 2000)),
                listOf(PublicationDateFilter(1999, 1999)),
                emptyList<RightSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                null,
                setOf(METADATA_PUB_1990_2000_OPEN),
                "Metadata exception: Publication dates between 1990-2000 except 1999"
            ),
            arrayOf(
                "sig:$TEST_SIGEL",
                null,
                emptyList<MetadataSearchFilter>(),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                null,
                setOf(METADATA_SIG_FOO_1, METADATA_SIG_FOO_2),
                "All matches for paket sigel $TEST_SIGEL"
            ),
            arrayOf(
                "sig:$TEST_SIGEL",
                "metadataid:foo2",
                emptyList<MetadataSearchFilter>(),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                emptyList<RightSearchFilter>(),
                null,
                null,
                setOf(METADATA_SIG_FOO_1),
                "Search query exception: All matches for paket sigel $TEST_SIGEL except for foo2"
            ),
            arrayOf(
                null,
                null,
                emptyList<MetadataSearchFilter>(),
                emptyList<MetadataSearchFilter>(),
                listOf(
                    RightValidOnFilter(
                        LocalDate.of(1999, 1, 1)
                    )
                ),
                emptyList<RightSearchFilter>(),
                null,
                null,
                setOf(METADATA_PUB_1999_OPEN, METADATA_PUB_1990_2000_OPEN),
                "Right filter: Filter all with valid right on 1.1.1999",
            ),
            arrayOf(
                null,
                null,
                emptyList<MetadataSearchFilter>(),
                emptyList<MetadataSearchFilter>(),
                listOf(
                    RightValidOnFilter(
                        LocalDate.of(1999, 1, 1)
                    )
                ),
                listOf(
                    RightValidOnFilter(
                        LocalDate.of(1991, 1, 1)
                    )
                ),
                null,
                null,
                setOf(METADATA_PUB_1999_OPEN),
                "Right filter: Filter all with valid right on 1.1.1999 except those with right information on 1.1.1991",
            ),
            arrayOf(
                null,
                null,
                emptyList<MetadataSearchFilter>(),
                emptyList<MetadataSearchFilter>(),
                listOf(
                    RightValidOnFilter(
                        LocalDate.of(1999, 1, 1)
                    )
                ),
                listOf(
                    RightValidOnFilter(
                        LocalDate.of(1991, 1, 1)
                    )
                ),
                null,
                NoRightInformationFilter(),
                setOf(METADATA_PUB_1999_OPEN),
                "No right information filter test",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_WITH_EXCEPTIONS)
    fun testSearchWithExceptions(
        givenSearchTerm: String?,
        exceptionSearchTerm: String?,
        metadataSearchFilter: List<MetadataSearchFilter>,
        exceptionMetadataSearchFilter: List<MetadataSearchFilter>,
        rightsSearchFilter: List<RightSearchFilter>,
        exceptionRightsSearchFilter: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
        exceptionNoRightInformationFilter: NoRightInformationFilter?,
        expectedResult: Set<ItemMetadata>,
        description: String,
    ) {
        val received: SearchQueryResult = backend.searchQuery(
            searchTerm = givenSearchTerm,
            exceptionSearchTerm = exceptionSearchTerm,
            rightSearchFilter = rightsSearchFilter,
            exceptionRightSearchFilter = exceptionRightsSearchFilter,
            metadataSearchFilter = metadataSearchFilter,
            exceptionMetadataFilter = exceptionMetadataSearchFilter,
            noRightInformationFilter = noRightInformationFilter,
            exceptionNoRightInformationFilter = exceptionNoRightInformationFilter,
            limit = 10,
            offset = 0,
        )
        assertThat(
            description,
            received.results.map { it.metadata }.toSet(),
            `is`(expectedResult),
        )
    }

    companion object {
        const val TEST_SIGEL = "sig-1234"
        val METADATA_NO_RIGHT_INFORMATION = TEST_Metadata.copy(
            metadataId = "no_rights",
        )
        val METADATA_PUB_1999_OPEN = TEST_Metadata.copy(
            metadataId = "pub_1999_open",
            zdbId = null,
            licenceUrl = "foobar.baz",
            paketSigel = "someothersigel2",
            publicationDate = LocalDate.of(1999, 1, 1)
        )
        val METADATA_PUB_1990_2000_OPEN = TEST_Metadata.copy(
            metadataId = "pub_1990_1998_open",
            zdbId = null,
            licenceUrl = "foobar.baz",
            paketSigel = "someothersigel2",
            publicationDate = LocalDate.of(1990, 1, 1)
        )

        val METADATA_SIG_FOO_1 = TEST_Metadata.copy(
            metadataId = "foo1",
            paketSigel = TEST_SIGEL,
        )

        val METADATA_SIG_FOO_2 = TEST_Metadata.copy(
            metadataId = "foo2",
            paketSigel = TEST_SIGEL,
        )

        val RIGHT_OPEN_1999 = TEST_RIGHT.copy(
            rightId = "open",
            accessState = AccessState.OPEN,
            startDate = LocalDate.of(1999, 1, 1),
            endDate = LocalDate.of(1999, 12, 31),
            templateName = null,
            isTemplate = false,
        )
        val RIGHT_OPEN_1990_2000 = TEST_RIGHT.copy(
            rightId = "open",
            accessState = AccessState.OPEN,
            startDate = LocalDate.of(1990, 1, 1),
            endDate = LocalDate.of(2000, 12, 31),
            templateName = null,
            isTemplate = false,
        )

        const val DATA_FOR_SEARCH_WITH_EXCEPTIONS = "DATA_FOR_EXCEPTION_SEARCH"
    }
}
