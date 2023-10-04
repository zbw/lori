package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.Either
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
import org.junit.Assert
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
            TEST_METADATA.copy(metadataId = "roundtrip"),
            TEST_METADATA.copy(metadataId = "no_rights"),
        )
        val rightAssignments = TEST_RIGHT to listOf(givenMetadataEntries[0].metadataId)

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
            TEST_METADATA.copy(metadataId = "zzz", publicationDate = LocalDate.of(1978, 1, 1)),
            TEST_METADATA.copy(metadataId = "zzz2", publicationDate = LocalDate.of(1978, 1, 1)),
            TEST_METADATA.copy(metadataId = "aaa"),
            TEST_METADATA.copy(metadataId = "abb"),
            TEST_METADATA.copy(metadataId = "acc"),
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
        val expectedMetadata = TEST_METADATA.copy(band = "anotherband")

        // when
        backend.upsertMetadataElements(listOf(expectedMetadata))
        val received = backend.getMetadataElementsByIds(listOf(expectedMetadata.metadataId))

        // then
        assertThat(received, `is`(listOf(expectedMetadata)))

        val expectedMetadata2 = TEST_METADATA.copy(band = "anotherband2")
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
                emptyList<SearchPair>(),
                "no search key pair"
            ),
            arrayOf(
                "bllaaaa col:bar",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "bar",
                    )
                ),
                "single case with random string"
            ),
            arrayOf(
                "col:bar",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "bar",
                    )
                ),
                "single case no additional string"
            ),
            arrayOf(
                "                col:bar                             ",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "bar",
                    )
                ),
                "single case with whitespace"
            ),
            arrayOf(
                "col:bar zdb:foo",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "bar",
                    ),
                    SearchPair(
                        SearchKey.ZDB_ID,
                        "foo",
                    ),
                ),
                "two search keys"
            ),
            arrayOf(
                "col:'foo | bar'",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "foo | bar",
                    )
                ),
                "multiple words quoted"
            ),
            arrayOf(
                "col:'foobar'",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "foobar",
                    ),
                ),
                "single word quoted"
            ),
            arrayOf(
                "col:\"foobar\"",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "foobar",
                    ),
                ),
                "single word doublequoted"
            ),
            arrayOf(
                "            col:'foobar'           com:'foo & bar'",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "foobar",
                    ),
                    SearchPair(
                        SearchKey.COMMUNITY,
                        "foo & bar",
                    ),
                ),
                "mutltiple and single words quoted with whitespaces"
            ),
            arrayOf(
                "col:col-foo-bar",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "col-foo-bar",
                    )
                ),
                "multiple words minus"
            ),
            arrayOf(
                "col:'col-foo-bar'",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "col-foo-bar",
                    ),
                ),
                "multiple words quoted minus"
            ),
            arrayOf(
                "col:'col-;:'",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "col-;:",
                    ),
                ),
                "handle special characters"
            ),
            arrayOf(
                "col:'(subject1 | subject2) & subject3'",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "(subject1 | subject2) & subject3",
                    ),
                ),
                "with parentheses"
            ),
            arrayOf(
                "col:\"(subject1 | subject2) & subject3\"",
                listOf(
                    SearchPair(
                        SearchKey.COLLECTION,
                        "(subject1 | subject2) & subject3",
                    ),
                ),
                "with parentheses and double quotes"
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_KEY_PARSING)
    fun testParseSearchKeys(
        searchTerm: String,
        expectedKeys: List<SearchPair>,
        description: String,
    ) {
        val receivedPairs: List<SearchPair> = LoriServerBackend.parseValidSearchPairs(searchTerm)
        receivedPairs.forEachIndexed { index, searchPair ->
            assertThat(
                searchPair.values,
                `is`(expectedKeys[index].values),
            )
            assertThat(
                searchPair.key,
                `is`(expectedKeys[index].key),
            )
        }
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
            TEST_METADATA.copy(metadataId = "search_test_1", zdbId = "zbdTest"),
            TEST_METADATA.copy(metadataId = "search_test_2", zdbId = "zbdTest"),
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

    @Test
    fun testSearchKeyConversion() {
        val given = listOf(
            SearchPair(SearchKey.TITLE, "foobar & baz"),
            SearchPair(SearchKey.COLLECTION, "col1"),
            SearchPair(SearchKey.COMMUNITY, "com1"),
            SearchPair(SearchKey.ZDB_ID, "zdb1"),
        )
        val received = LoriServerBackend.parseValidSearchPairs(
            LoriServerBackend.searchPairsToString(given)
        )
        received.forEachIndexed { index, searchPair ->
            assertThat(
                searchPair.values,
                `is`(given[index].values),
            )
            assertThat(
                searchPair.key,
                `is`(given[index].key),
            )
        }
    }

    @DataProvider(name = DATA_FOR_CHECK_RIGHT_CONFLICTS)
    fun createDataForCheckDateRightConflicts() =
        arrayOf(
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2025, 6, 1),
                    endDate = LocalDate.of(2025, 9, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                false,
                "Valid. Start and end date completely disjunct",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2025, 6, 1),
                    endDate = LocalDate.of(2025, 9, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2025, 9, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                true,
                "Invalid overlap. Start or end date match on one day",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2026, 6, 1),
                ),
                true,
                "Invalid overlap. Start or end date match on one day",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 4, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                true,
                "Invalid overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = null,
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 4, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                true,
                "Invalid overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 2),
                    endDate = null
                ),
                true,
                "Invalid overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2024, 6, 1),
                    endDate = LocalDate.of(2024, 7, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 2),
                    endDate = null
                ),
                false,
                "No overlap.",
            ),
        )

    @Test(dataProvider = DATA_FOR_CHECK_RIGHT_CONFLICTS)
    fun testCheckDateRightConflicts(
        r1: ItemRight,
        r2: ItemRight,
        expected: Boolean,
        description: String,
    ) {
        assertThat(
            description,
            LoriServerBackend.checkForDateConflict(r1, r2),
            `is`(expected),
        )
    }

    @Test
    fun testInsertItemEntry() {
        val givenMetadata = TEST_METADATA
        val givenRight1 = TEST_RIGHT.copy(rightId = "1")
        val givenRight2 = TEST_RIGHT.copy(rightId = "2")
        val givenRight3 = TEST_RIGHT.copy(rightId = "3")

        backend.insertMetadataElement(givenMetadata)
        backend.insertRight(givenRight1)
        backend.insertRight(givenRight2)
        backend.insertRight(givenRight3)

        backend.insertItemEntry(givenMetadata.metadataId, givenRight1.rightId!!)

        // Insert first conflict, no deletion
        when (backend.insertItemEntry(givenMetadata.metadataId, givenRight2.rightId!!)) {
            is Either.Left -> {
                // Error is expected due to conflict
            }

            is Either.Right -> {
                Assert.fail("An error should be raised due to a given conflict.")
            }
        }

        when (backend.insertItemEntry(givenMetadata.metadataId, givenRight3.rightId!!, true)) {
            is Either.Left -> {
                // Error is expected due to conflict
            }

            is Either.Right -> {
                Assert.fail("An error should be raised due to a given conflict.")
            }
        }

        val existingRights =
            backend.getRightsByIds(listOf(givenRight1.rightId!!, givenRight2.rightId!!, givenRight3.rightId!!))
                .map { it.rightId }
                .toSet()
        assertThat(
            existingRights,
            `is`(setOf(givenRight1.rightId!!, givenRight2.rightId!!))
        )
    }

    companion object {
        const val DATA_FOR_CHECK_RIGHT_CONFLICTS = "DATA_FOR_CHECK_RIGHT_CONFLICTS"
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
        val TEST_METADATA = ItemMetadata(
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
            lastAppliedOn = null,
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
            templateDescription = "descritpion",
            templateId = null,
            templateName = "name",
            zbwUserAgreement = true,
        )
    }
}
