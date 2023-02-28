package de.zbw.business.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
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
import java.time.Instant.now
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertTrue

/**
 * Test [LoriServerBackend].
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackendTest : DatabaseTest() {

    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            gson = mockk(),
        ),
        mockk(),
    )

    @BeforeClass
    fun fillDatabase() {
        mockkStatic(Instant::class)
        every { now() } returns NOW.toInstant()
    }

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testRoundtrip() {
        // given
        val givenMetadataEntries = arrayOf(
            TEST_Metadata,
            TEST_Metadata.copy(metadataId = "no_rights"),
        )
        val rightAssignments = TEST_RIGHT to listOf(TEST_Metadata.metadataId)

        // when
        backend.insertMetadataElements(givenMetadataEntries.toList())
        val generatedRightId = backend.insertRightForMetadataIds(rightAssignments.first, rightAssignments.second)
        val received = backend.getItemByMetadataId(givenMetadataEntries[0].metadataId)!!

        // then
        assertThat(received, `is`(Item(givenMetadataEntries[0], listOf(TEST_RIGHT.copy(rightId = generatedRightId)))))

        // when
        val receivedNoRights = backend.getItemByMetadataId(givenMetadataEntries[1].metadataId)!!
        // then
        assertThat(receivedNoRights, `is`(Item(givenMetadataEntries[1], emptyList())))
    }

    @Test
    fun testGetList() {
        // given
        val givenMetadata = arrayOf(
            TEST_Metadata.copy(metadataId = "zzz", publicationDate = LocalDate.of(1978, 1, 1)),
            TEST_Metadata.copy(metadataId = "zzz2", publicationDate = LocalDate.of(1978, 1, 1)),
            TEST_Metadata.copy(metadataId = "aaa"),
            TEST_Metadata.copy(metadataId = "abb"),
            TEST_Metadata.copy(metadataId = "acc"),
        )

        backend.insertMetadataElements(givenMetadata.toList())
        // when
        val receivedItems: List<Item> = backend.getItemList(limit = 3, offset = 0)
        // then
        assertThat(
            "Not equal",
            receivedItems,
            `is`(
                listOf(
                    Item(
                        givenMetadata[2],
                        emptyList(),
                    ),
                    Item(
                        givenMetadata[3],
                        emptyList(),
                    ),
                    Item(
                        givenMetadata[4],
                        emptyList(),
                    )
                )
            )
        )

        // no limit
        val noLimit = backend.getItemList(
            limit = 0, offset = 0
        )
        assertThat(noLimit, `is`(emptyList()))

        assertTrue(backend.metadataContainsId(givenMetadata[0].metadataId))

        // when
        val receivedMetadataElements: List<ItemMetadata> = backend.getMetadataList(3, 0)

        // then
        assertThat(
            receivedMetadataElements,
            `is`(
                listOf(
                    givenMetadata[2],
                    givenMetadata[3],
                    givenMetadata[4],
                )
            )
        )
        assertThat(backend.getMetadataList(1, 100), `is`(emptyList()))
        assertThat(backend.getMetadataList(1, 100), `is`(emptyList()))
        assertThat(backend.countMetadataEntries(), `is`(5))
    }

    @Test
    fun testUpsert() {
        // given
        val expectedMetadata = TEST_Metadata.copy(band = "anotherband")

        // when
        backend.upsertMetadataElements(listOf(expectedMetadata))
        val received = backend.getMetadataElementsByIds(listOf(expectedMetadata.metadataId))

        // then
        assertThat(received, `is`(listOf(expectedMetadata)))

        val expectedMetadata2 = TEST_Metadata.copy(band = "anotherband2")
        // when
        backend.upsertMetadataElements(listOf(expectedMetadata2))
        val received2 = backend.getMetadataElementsByIds(listOf(expectedMetadata2.metadataId))

        // then
        assertThat(received2, `is`(listOf(expectedMetadata2)))
    }

    @Test
    fun testHashString() {
        val hashedPassword = LoriServerBackend.hashString("SHA-256", "foobar")
        val expectedHash = "c3ab8ff13720e8ad9047dd39466b3c8974e592c2fa383d4a3960714caef0c4f2"
        assertThat(hashedPassword, `is`(expectedHash))
    }

    @DataProvider(name = DATA_FOR_SEARCH_KEY_PARSING)
    fun createDataForSearchKeyParsing() =
        arrayOf(
            arrayOf(
                "bllaaaa",
                emptyMap<SearchKey, List<String>>(),
                "no search key pair"
            ),
            arrayOf(
                "bllaaaa col:bar",
                mapOf(
                    SearchKey.COLLECTION to listOf("bar"),
                ),
                "single case with random string"
            ),
            arrayOf(
                "col:bar",
                mapOf(
                    SearchKey.COLLECTION to listOf("bar"),
                ),
                "single case no additional string"
            ),
            arrayOf(
                "                col:bar                             ",
                mapOf(
                    SearchKey.COLLECTION to listOf("bar"),
                ),
                "single case with whitespace"
            ),
            arrayOf(
                "col:bar zdb:foo",
                mapOf(
                    SearchKey.COLLECTION to listOf("bar"),
                    SearchKey.ZDB_ID to listOf("foo"),
                ),
                "two search keys"
            ),
            arrayOf(
                "col:'foo bar'",
                mapOf(
                    SearchKey.COLLECTION to listOf("foo", "bar"),
                ),
                "multiple words quoted"
            ),
            arrayOf(
                "col:'foobar'",
                mapOf(
                    SearchKey.COLLECTION to listOf("foobar"),
                ),
                "single word quoted"
            ),
            arrayOf(
                "            col:'foobar'           com:'foo bar'",
                mapOf(
                    SearchKey.COLLECTION to listOf("foobar"),
                    SearchKey.COMMUNITY to listOf("foo", "bar"),
                ),
                "mutltiple and single words quoted with whitespaces"
            ),
            arrayOf(
                "col:col-foo-bar",
                mapOf(
                    SearchKey.COLLECTION to listOf("col-foo-bar"),
                ),
                "multiple words minus"
            ),
            arrayOf(
                "col:'col-foo-bar'",
                mapOf(
                    SearchKey.COLLECTION to listOf("col-foo-bar"),
                ),
                "multiple words quoted minus"
            ),
            arrayOf(
                "col:'col-;:'",
                mapOf(
                    SearchKey.COLLECTION to listOf("col-;:"),
                ),
                "handle special characters"
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_KEY_PARSING)
    fun testParseSearchKeys(
        searchTerm: String,
        expectedKeys: Map<SearchKey, List<String>>,
        description: String,
    ) {
        assertThat(description, LoriServerBackend.parseValidSearchKeys(searchTerm), `is`(expectedKeys))
    }

    @DataProvider(name = DATA_FOR_INVALID_SEARCH_KEY_PARSING)
    fun createDataForInvalidSearchKeyParsing() =
        arrayOf(
            arrayOf(
                "bllaaaa",
                emptyList<String>(),
                "no search key pair"
            ),
            arrayOf(
                "moo:koo col:foobar cro:moobar",
                listOf("moo", "cro"),
                "two invalid keys"
            ),
        )

    @Test(dataProvider = DATA_FOR_INVALID_SEARCH_KEY_PARSING)
    fun testParseInvalidSearchKeys(
        searchTerm: String,
        expectedKeys: List<String>,
        description: String,
    ) {
        assertThat(description, LoriServerBackend.parseInvalidSearchKeys(searchTerm), `is`(expectedKeys))
    }

    @Test
    fun testSearchQuery() {
        // given
        val givenMetadataEntries = arrayOf(
            TEST_Metadata.copy(metadataId = "search_test_1", zdbId = "zbdTest"),
            TEST_Metadata.copy(metadataId = "search_test_2", zdbId = "zbdTest"),
        )
        val rightAssignments = TEST_RIGHT to listOf(givenMetadataEntries[0].metadataId)

        backend.insertMetadataElements(givenMetadataEntries.toList())
        val generatedRightId = backend.insertRightForMetadataIds(rightAssignments.first, rightAssignments.second)

        // when
        val (number, items) = backend.searchQuery(
            "zdb:${givenMetadataEntries[0].zdbId!!}",
            5,
            0
        )

        // then
        assertThat(number, `is`(2))
        assertThat(
            items.toSet(),
            `is`(
                setOf(
                    Item(
                        metadata = givenMetadataEntries[0],
                        rights = listOf(TEST_RIGHT.copy(rightId = generatedRightId)),
                    ),
                    Item(
                        metadata = givenMetadataEntries[1],
                        rights = emptyList(),
                    )
                )
            )
        )

        // when
        val (numberNoItem, itemsNoItem) = backend.searchQuery(
            "zdb:NOT_IN_DATABASE_ID",
            5,
            0
        )
        assertThat(numberNoItem, `is`(0))
        assertThat(itemsNoItem, `is`(emptyList()))
    }

    @DataProvider(name = DATA_FOR_REMOVE_VALID_SEARCH_TOKEN)
    fun createDataForRemoveValidSearchToken() =
        arrayOf(
            arrayOf(
                "",
                false,
            ),
            arrayOf(
                "  foo:'bar baz' abc  bcd\t",
                true,
            ),
            arrayOf(
                "  col:'bar'  \t  ",
                false,
            ),
            arrayOf(
                "  baaaa col:'bar'  \t  ",
                true,
            ),
            arrayOf(
                "  col:bar \t  ",
                false,
            ),
            arrayOf(
                "  col:bar foooo\t  ",
                true,
            ),
            arrayOf(
                "  col:'bar baz'  \t  ",
                false,
            ),
        )

    @Test(dataProvider = DATA_FOR_REMOVE_VALID_SEARCH_TOKEN)
    fun testRemoveValidSearchToken(input: String, expected: Boolean) {
        assertThat(
            "$input was not correctly identified",
            LoriServerBackend.hasSearchTokensWithNoKey(input),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_INVALID_SEARCH_KEY_PARSING = "DATA_FOR_INVALID_SEARCH_KEY_PARSING"
        const val DATA_FOR_SEARCH_KEY_PARSING = "DATA_FOR_SEARCH_KEY_PARSING"
        const val DATA_FOR_REMOVE_VALID_SEARCH_TOKEN = "DATA_FOR_REMOVE_VALID_SEARCH_TOKEN"

        val NOW: OffsetDateTime = OffsetDateTime.of(
            2022,
            3,
            1,
            1,
            1,
            0,
            0,
            ZoneOffset.UTC,
        )!!

        private val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
        val TEST_Metadata = ItemMetadata(
            metadataId = "that-test",
            author = "Colbjørnsen, Terje",
            band = "band",
            collectionName = "collectionName",
            communityName = "communityName",
            createdBy = "user1",
            createdOn = NOW,
            doi = "doi:example.org",
            handle = "hdl:example.handle.net",
            isbn = "1234567890123",
            issn = "123456",
            lastUpdatedBy = "user2",
            lastUpdatedOn = NOW,
            paketSigel = "sigel",
            ppn = "ppn",
            publicationType = PublicationType.ARTICLE,
            publicationDate = LocalDate.of(2022, 9, 26),
            rightsK10plus = "some rights",
            storageDate = NOW.minusDays(3),
            title = "Important title",
            titleJournal = null,
            titleSeries = null,
            zdbId = null,
        )

        private val TEST_RIGHT = ItemRight(
            rightId = "12",
            accessState = AccessState.OPEN,
            authorRightException = true,
            basisAccessState = BasisAccessState.LICENCE_CONTRACT,
            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
            createdBy = "user1",
            createdOn = NOW,
            endDate = TODAY,
            groupIds = emptyList(),
            lastUpdatedBy = "user2",
            lastUpdatedOn = NOW,
            licenceContract = "some contract",
            nonStandardOpenContentLicence = true,
            nonStandardOpenContentLicenceURL = "https://nonstandardoclurl.de",
            notesGeneral = "Some general notes",
            notesFormalRules = "Some formal rule notes",
            notesProcessDocumentation = "Some process documentation",
            notesManagementRelated = "Some management related notes",
            openContentLicence = "some licence",
            restrictedOpenContentLicence = false,
            startDate = TODAY.minusDays(1),
            zbwUserAgreement = true,
        )
    }
}
