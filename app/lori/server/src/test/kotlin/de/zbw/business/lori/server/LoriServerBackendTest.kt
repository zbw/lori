package de.zbw.business.lori.server

import de.zbw.api.lori.server.type.Either
import de.zbw.business.lori.server.ApplyTemplateTest.Companion.ZDB_1
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.RightError
import de.zbw.business.lori.server.type.SearchGrammar
import de.zbw.business.lori.server.type.SortInformation
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNull
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.Instant
import java.time.Instant.now
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertTrue

/**
 * Test [LoriServerBackend].
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackendTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("de.zbw.business.lori.server.LoriServerBackendTest"),
            ),
            mockk {
                every { url } returns "foo.bar"
            },
        )
    private val templateApplication =
        TemplateApplication(
            dbConnector = backend.dbConnector,
            backend = backend,
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
    fun testRoundtrip() =
        runBlocking {
            // given
            val givenMetadataEntries =
                arrayOf(
                    TEST_METADATA.copy(handle = "11159/222"),
                    TEST_METADATA.copy(handle = "11159/223"),
                )
            val rightAssignments = TEST_RIGHT to listOf(givenMetadataEntries[0].handle)

            // when
            backend.insertMetadataElements(givenMetadataEntries.toList())
            val generatedRightId =
                backend.insertRightForHandles(
                    right = rightAssignments.first,
                    handles = rightAssignments.second,
                    createdBy = "testUser",
                )
            val received = backend.getItemByHandle(givenMetadataEntries[0].handle)!!

            // then
            assertThat(received, `is`(Item(givenMetadataEntries[0], listOf(TEST_RIGHT.copy(rightId = generatedRightId)))))

            // when
            val receivedNoRights = backend.getItemByHandle(givenMetadataEntries[1].handle)!!
            // then
            assertThat(receivedNoRights, `is`(Item(givenMetadataEntries[1], emptyList())))
        }

    @Test
    fun testGetList() =
        runBlocking {
            // given

            // We need high handle numbers for sorting purposes...
            val givenMetadata =
                arrayOf(
                    TEST_METADATA.copy(handle = "11159/10818", publicationYear = 1978),
                    TEST_METADATA.copy(handle = "11159/10819", publicationYear = 1978),
                    TEST_METADATA.copy(handle = "11159/10820"),
                    TEST_METADATA.copy(handle = "11159/10821"),
                    TEST_METADATA.copy(handle = "11159/10822"),
                )

            backend.insertMetadataElements(givenMetadata.toList())
            // when
            val receivedItems: List<Item> = backend.getItemList(limit = 3, offset = 0)
            // then
            assertThat(
                "Not equal",
                receivedItems.toSet(),
                `is`(
                    setOf(
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
                        ),
                    ),
                ),
            )

            // no limit
            val noLimit =
                backend.getItemList(
                    limit = 0,
                    offset = 0,
                )
            assertThat(noLimit, `is`(emptyList()))

            assertTrue(backend.metadataContainsHandle(givenMetadata[0].handle))

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
                    ),
                ),
            )
            assertThat(backend.getMetadataList(1, 100), `is`(emptyList()))
            assertThat(backend.getMetadataList(1, 100), `is`(emptyList()))
        }

    @Test
    fun testUpsert() =
        runBlocking {
            // given
            val expectedMetadata = TEST_METADATA.copy(isPartOfBook = "anotherbook")

            // when
            backend.upsertMetadataElements(listOf(expectedMetadata))
            val received = backend.getMetadataElementsByIds(listOf(expectedMetadata.handle))

            // then
            assertThat(received, `is`(listOf(expectedMetadata)))

            val expectedMetadata2 = TEST_METADATA.copy(isPartOfBook = "anotherbook")
            // when
            backend.upsertMetadataElements(listOf(expectedMetadata2))
            val received2 = backend.getMetadataElementsByIds(listOf(expectedMetadata2.handle))

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
                "",
                "no search key pair",
            ),
            arrayOf(
                "bllaaaa col:bar",
                "col:\"bar\"",
                "single case with random string",
            ),
            arrayOf(
                "col:bar",
                "col:\"bar\"",
                "single case no additional string",
            ),
            arrayOf(
                "                col:bar                             ",
                "col:\"bar\"",
                "single case with whitespace",
            ),
            arrayOf(
                "col:bar zdb:foo",
                "col:\"bar\",zdb:\"foo\"",
                "two search keys",
            ),
            arrayOf(
                "col:\"foobar\"",
                "col:\"foobar\"",
                "single word quoted",
            ),
            arrayOf(
                "col:\"foobar\"",
                "col:\"foobar\"",
                "single word doublequoted",
            ),
            arrayOf(
                "            col:\"foobar\"           com:\"foo & bar\"",
                "col:\"foobar\",com:\"foo & bar\"",
                "mutltiple and single words quoted with whitespaces",
            ),
            arrayOf(
                "col:col-foo-bar",
                "col:\"col-foo-bar\"",
                "multiple words minus",
            ),
            arrayOf(
                "col:\"col-foo-bar\"",
                "col:\"col-foo-bar\"",
                "multiple words quoted minus",
            ),
            arrayOf(
                "col:\"col-;:\"",
                "col:\"col-;:\"",
                "handle special characters",
            ),
            arrayOf(
                "ser:\"subject1 subject2 subject3 subject4 subject5\"",
                "ser:\"subject1 subject2 subject3 subject4 subject5\"",
                "direct access multiple words",
            ),
            arrayOf(
                "tit:\"subject1 subject2 subject3 subject4 subject5\"",
                "tit:\"subject1 subject2 subject3 subject4 subject5\"",
                "TSVector access and multiple words",
            ),
            arrayOf(
                "jah:\"2011-2013\"",
                "jah:2011-2013",
                "Publication Date from and to",
            ),
            arrayOf(
                "jah:\"2011-\"",
                "jah:2011-",
                "Publication Date from",
            ),
            arrayOf(
                "jah:\"-2013\"",
                "jah:-2013",
                "Publication Date to",
            ),
        )

    @Test(dataProvider = DATA_FOR_SEARCH_KEY_PARSING)
    fun testParseSearchKeys(
        searchTerm: String,
        expectedKeys: String,
        description: String,
    ) {
        val receivedPairs = SearchGrammar.parseSearchTermToFilters(searchTerm)
        assertThat(
            description,
            receivedPairs.joinToString(separator = ","),
            `is`(
                expectedKeys,
            ),
        )
    }

    @DataProvider(name = DATA_FOR_INVALID_SEARCH_KEY_PARSING)
    fun createDataForInvalidSearchKeyParsing() =
        arrayOf(
            arrayOf(
                "bllaaaa",
                emptyList<String>(),
                "no search key pair",
            ),
            arrayOf(
                "moo:koo col:foobar cro:moobar",
                listOf("moo", "cro"),
                "two invalid keys",
            ),
        )

    @Test
    fun testSearchQuery() =
        runBlocking {
            // given
            val givenMetadataEntries =
                arrayOf(
                    TEST_METADATA.copy(handle = "11159/801", zdbIds = listOf("zbdTest")),
                    TEST_METADATA.copy(handle = "11159/802", zdbIds = listOf("zbdTest")),
                )
            val expectedRight =
                TEST_RIGHT.copy(
                    isTemplate = true,
                    templateName = "some name",
                )
            val rightAssignments =
                expectedRight to listOf(givenMetadataEntries[0].handle)

            backend.insertMetadataElements(givenMetadataEntries.toList())
            val generatedRightId =
                backend.insertRightForHandles(
                    right = rightAssignments.first,
                    handles = rightAssignments.second,
                    createdBy = "testUser",
                )

            // when
            val (number, items) =
                runBlocking {
                    backend.searchQuery(
                        searchTerm = "zdb:${givenMetadataEntries[0].zdbIds?.get(0)}",
                        limit = 5,
                        offset = 0,
                        sortInformation = SortInformation.DEFAULT,
                    )
                }

            // then
            assertThat(number, `is`(2))
            assertThat(
                items.toSet(),
                `is`(
                    setOf(
                        Item(
                            metadata = givenMetadataEntries[0],
                            rights =
                                listOf(
                                    expectedRight.copy(
                                        startDate = TEST_RIGHT.createdOn!!.toLocalDate(),
                                        rightId = generatedRightId,
                                    ),
                                ),
                        ),
                        Item(
                            metadata = givenMetadataEntries[1],
                            rights = emptyList(),
                        ),
                    ),
                ),
            )

            // when
            val (numberNoItem, itemsNoItem) =
                runBlocking {
                    backend.searchQuery(
                        searchTerm = "zdb:NOT_IN_DATABASE_ID",
                        limit = 5,
                        offset = 0,
                        sortInformation = SortInformation.DEFAULT,
                    )
                }
            assertThat(numberNoItem, `is`(0))
            assertThat(itemsNoItem, `is`(emptyList()))
        }

    @DataProvider(name = DATA_FOR_REMOVE_VALID_SEARCH_TOKEN)
    fun createDataForRemoveValidSearchToken(): Array<Array<Any?>> =
        arrayOf(
            arrayOf(
                "",
                false,
            ),
            arrayOf(
                "  foo:\"bar baz\" abc  bcd\t",
                true,
            ),
            arrayOf(
                "  col:\"bar\"  \t  ",
                false,
            ),
            arrayOf(
                "  baaaa col:\"bar\"  \t  ",
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
                "  col:\"bar baz\"  \t  ",
                false,
            ),
            arrayOf(
                "com:\"internation centre of\"",
                false,
            ),
        )

    @Test(dataProvider = DATA_FOR_REMOVE_VALID_SEARCH_TOKEN)
    fun testRemoveValidSearchToken(
        input: String,
        expected: Boolean,
    ) {
        assertThat(
            "$input was not correctly identified",
            LoriServerBackend.hasSearchTokensWithNoKey(input),
            `is`(expected),
        )
    }

    @DataProvider(name = DATA_FOR_CHECK_RIGHT_CONFLICTS)
    fun createDataForCheckDateRightConflicts() =
        arrayOf(
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 7, 1),
                    endDate = null,
                ),
                true,
                "Invalid overlap. End date equals start date",
            ),
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
                    startDate = LocalDate.of(2025, 9, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2025, 6, 1),
                    endDate = LocalDate.of(2025, 9, 1),
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
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2026, 6, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
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
                    startDate = LocalDate.of(2026, 4, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                true,
                "Invalid overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 4, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                true,
                "Invalid overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 7, 1),
                    endDate = LocalDate.of(2026, 8, 1),
                ),
                true,
                "Invalid inner overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 7, 1),
                    endDate = LocalDate.of(2026, 8, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 9, 1),
                ),
                true,
                "Invalid inner overlap.",
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
                    startDate = LocalDate.of(2026, 4, 1),
                    endDate = LocalDate.of(2026, 7, 1),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = null,
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
                    endDate = null,
                ),
                true,
                "Invalid overlap.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 2),
                    endDate = null,
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2026, 6, 1),
                    endDate = LocalDate.of(2026, 7, 1),
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
                    endDate = null,
                ),
                false,
                "No overlap. Gap between rights.",
            ),
            arrayOf(
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2024, 6, 1),
                    endDate = LocalDate.of(2024, 12, 31),
                ),
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = null,
                ),
                false,
                "No overlap. No gap between rights",
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
    fun testInsertItemEntry() =
        runBlocking {
            val givenMetadata = TEST_METADATA
            val givenRight1 = TEST_RIGHT
            val givenRight2 = TEST_RIGHT
            val givenRight3 = TEST_RIGHT

            backend.insertMetadataElement(givenMetadata)
            val rightId1 = backend.insertRight(givenRight1)
            val rightId2 = backend.insertRight(givenRight2)
            val rightId3 = backend.insertRight(givenRight3)

            backend.insertItemEntry(givenMetadata.handle, rightId1)

            // Insert first conflict, no deletion
            when (backend.insertItemEntry(givenMetadata.handle, rightId2)) {
                is Either.Left -> {
                    // Error is expected due to a conflict
                }

                is Either.Right -> {
                    Assert.fail("An error should be raised due to a given conflict.")
                }
            }

            when (backend.insertItemEntry(givenMetadata.handle, rightId3, true)) {
                is Either.Left -> {
                    // Error is expected due to a conflict
                }

                is Either.Right -> {
                    Assert.fail("An error should be raised due to a given conflict.")
                }
            }

            val existingRights =
                backend
                    .getRightsByIds(listOf(rightId1, rightId2, rightId3))
                    .map { it.rightId }
                    .toSet()
            assertThat(
                existingRights,
                `is`(setOf(rightId1, rightId2)),
            )

            // Add another entry successfully
            val givenRight4 =
                TEST_RIGHT.copy(
                    startDate = TODAY.minusDays(10),
                    endDate = TODAY.minusDays(6),
                )
            val rightId4 = backend.insertRight(givenRight4)
            when (
                backend.insertItemEntry(givenMetadata.handle, rightId4)
            ) {
                is Either.Left -> {
                    Assert.fail("No conflicts expected.")
                }

                is Either.Right -> {
                }
            }

            // Test update conflicts
            val ret =
                backend.upsertRight(
                    givenRight4.copy(
                        rightId = rightId4,
                        endDate = null,
                    ),
                )
            when (ret) {
                is Either.Left -> {
                }

                is Either.Right -> {
                    Assert.fail("An error should be raised due to a given conflict.")
                }
            }
        }

    @DataProvider(name = DATA_FOR_FIND_RIGHT_CONFLICTS)
    fun createDataForFindItemsWithConflicts() =
        arrayOf(
            arrayOf(
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights = listOf(TEST_RIGHT),
                    ),
                ),
                TEST_RIGHT,
                emptySet<Item>(),
                0,
                emptySet<ConflictType>(),
                "Skip same right",
            ),
            arrayOf(
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights = listOf(TEST_RIGHT),
                    ),
                ),
                TEST_RIGHT.copy(rightId = "testConflict"),
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights = listOf(TEST_RIGHT),
                    ),
                ),
                1,
                setOf<ConflictType>(
                    ConflictType.DATE_OVERLAP,
                ),
                "Find duplicate",
            ),
            arrayOf(
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights =
                            listOf(
                                TEST_RIGHT.copy(endDate = null),
                            ),
                    ),
                ),
                TEST_RIGHT.copy(
                    rightId = "testConflict",
                    startDate = TODAY.plusDays(2),
                    isTemplate = true,
                ),
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights =
                            listOf(
                                TEST_RIGHT.copy(endDate = null),
                            ),
                    ),
                ),
                1,
                setOf<ConflictType>(
                    ConflictType.DATE_OVERLAP_NO_END_MANUAL,
                ),
                "Conflict which can be resolved",
            ),
            arrayOf(
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights =
                            listOf(
                                TEST_RIGHT.copy(
                                    endDate = null,
                                    isTemplate = true,
                                ),
                            ),
                    ),
                ),
                TEST_RIGHT.copy(
                    rightId = "testConflict",
                    startDate = TODAY.plusDays(2),
                    isTemplate = true,
                ),
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights =
                            listOf(
                                TEST_RIGHT.copy(
                                    endDate = null,
                                    isTemplate = true,
                                ),
                            ),
                    ),
                ),
                1,
                setOf<ConflictType>(
                    ConflictType.DATE_OVERLAP,
                ),
                "Conflict which can not be resolved",
            ),
            arrayOf(
                setOf(
                    Item(
                        metadata = TEST_METADATA,
                        rights = listOf(TEST_RIGHT),
                    ),
                    Item(
                        metadata = TEST_METADATA.copy(handle = "11158/804"),
                        rights = listOf(TEST_RIGHT, TEST_RIGHT.copy(rightId = "right2")),
                    ),
                ),
                TEST_RIGHT.copy(
                    rightId = "testConflict",
                    startDate = LocalDate.MIN,
                    endDate = LocalDate.MIN.plusDays(1),
                ),
                emptySet<Item>(),
                0,
                emptySet<ConflictType>(),
                "No conflicts found",
            ),
        )

    @Test(dataProvider = DATA_FOR_FIND_RIGHT_CONFLICTS)
    fun testFindItemsWithConflicts(
        searchResults: Set<Item>,
        rightConflictToCheck: ItemRight,
        expected: Set<Item>,
        expectedErrorCount: Int,
        expectedConflictTypes: Set<ConflictType>,
        reason: String,
    ) {
        val received: Map<Item, List<RightError>> =
            LoriServerBackend.findItemsWithConflicts(
                searchResults,
                rightConflictToCheck,
                null,
                "user1",
            )
        assertThat(
            reason,
            received.keys,
            `is`(expected),
        )
        assertThat(
            reason,
            received.values.flatten().size,
            `is`(expectedErrorCount),
        )
        assertThat(
            reason,
            received.values
                .flatten()
                .map { it.conflictType }
                .toSet(),
            `is`(expectedConflictTypes),
        )
    }

    @Test
    fun testSumUpMapEntries() {
        // Given
        val given =
            mapOf(
                listOf("foo", "bar", "baz") to 5,
                listOf("foo", "bar") to 3,
                listOf("foo") to 5,
            )

        val expected =
            mapOf(
                "foo" to 13,
                "bar" to 8,
                "baz" to 5,
            )

        // when + then
        assertThat(
            LoriServerBackend.sumUpIndividualMapEntries(given),
            `is`(expected),
        )
    }

    @Test
    fun testTransformTemplateToManualRightForDeletedItem() =
        runBlocking {
            // given
            val deletedMetadata =
                TEST_METADATA.copy(
                    handle = "11159/902",
                    deleted = true,
                )

            val template =
                TEST_RIGHT.copy(
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2020, 12, 31),
                    isTemplate = true,
                    templateName = "testTransofrming",
                )
            val rightAssignments =
                listOf(template to listOf(deletedMetadata.handle))

            mockkStatic(Instant::class)
            every { now() } returns NOW.minusYears(20L).toInstant()
            backend.insertMetadataElement(deletedMetadata)
            rightAssignments.forEach { pair ->
                backend.insertRightForHandles(
                    right = pair.first,
                    handles = pair.second,
                    createdBy = "testUser",
                )
            }

            // when
            val deletionDate = LocalDate.of(2020, 9, 1)

            mockkStatic(Instant::class)
            every { now() } returns NOW.toInstant()
            val updates =
                backend.deleteAndUpdateManualRightsByHandle(
                    deletionDate = deletionDate,
                    handle = deletedMetadata.handle,
                )
            assertThat(
                updates,
                `is`(1),
            )

            // then
            val item = backend.getItemByHandle(deletedMetadata.handle)!!

            assertThat(
                item.rights.size,
                `is`(1),
            )

            assertThat(
                item.rights.first().endDate!!,
                `is`(deletionDate),
            )
            assertThat(
                item.rights.first().startDate,
                `is`(LocalDate.of(2020, 1, 1)),
            )

            assertFalse(
                item.rights.first().isTemplate,
            )
            assertNull(
                item.rights.first().templateName,
            )
            assertNull(
                item.rights.first().templateDescription,
            )
            assertNull(
                item.rights.first().predecessorId,
            )
            assertNull(
                item.rights.first().successorId,
            )
            assertNull(
                item.rights.first().exceptionOfId,
            )
            assertNull(
                item.rights.first().hasExceptionId,
            )
        }

    @Test
    fun testDeleteAndUpdateManualRightsByHandle() =
        runBlocking {
            // given
            val deletedMetadata = TEST_METADATA.copy(handle = "11159/878", deleted = true)

            val rightAssignments =
                listOf(
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2020, 1, 1),
                        endDate = LocalDate.of(2020, 12, 31),
                    ) to listOf(deletedMetadata.handle),
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2021, 1, 1),
                        endDate = LocalDate.of(2021, 12, 31),
                    ) to listOf(deletedMetadata.handle),
                    TEST_RIGHT.copy(
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    ) to listOf(deletedMetadata.handle),
                )

            backend.insertMetadataElement(deletedMetadata)
            rightAssignments.forEach { pair ->
                backend.insertRightForHandles(
                    right = pair.first,
                    handles = pair.second,
                    createdBy = "testUser",
                )
            }

            // when

            val deletionDate = LocalDate.of(2020, 9, 1)

            val updates =
                backend.deleteAndUpdateManualRightsByHandle(
                    deletionDate = deletionDate,
                    handle = deletedMetadata.handle,
                )
            assertThat(
                updates,
                `is`(3),
            )

            // then
            val item = backend.getItemByHandle(deletedMetadata.handle)!!

            assertThat(
                item.rights.size,
                `is`(1),
            )

            assertThat(
                item.rights.first().endDate!!,
                `is`(deletionDate),
            )
            assertThat(
                item.rights.first().startDate,
                `is`(LocalDate.of(2020, 1, 1)),
            )
        }

    @Test
    fun testDeleteAndUpdateFutureTemplates() =
        runBlocking {
            val today = LocalDate.of(NOW.year, NOW.month, NOW.dayOfMonth).plusDays(10)
            mockkStatic(LocalDate::class)
            every { LocalDate.now(any<ZoneId>()) } returns today
            every { LocalDate.now() } returns today
            // given
            val metadataToDelete =
                TEST_METADATA.copy(
                    handle = "11159/478",
                    deleted = false,
                    zdbIds = listOf(ZDB_1),
                )

            val templateId =
                backend.insertTemplate(
                    TEST_RIGHT.copy(
                        templateName = "die zukunft",
                        isTemplate = true,
                        // Start Date is in the future as well
                        startDate = today.plusDays(1),
                        endDate = today.plusYears(1),
                    ),
                )
            backend.insertMetadataElement(metadataToDelete)

            // Connect Bookmark and Template
            val bookmarkId =
                backend.insertBookmark(
                    Bookmark(
                        bookmarkName = "zdb1Bookmark",
                        bookmarkId = 0,
                        zdbIdFilters =
                            listOf(
                                ZDBIdFilter(
                                    zdbId = ZDB_1,
                                ),
                            ),
                    ),
                )

            backend.insertBookmarkTemplatePair(
                bookmarkId = bookmarkId,
                rightId = templateId,
            )

            val received =
                templateApplication.applyTemplate(
                    templateId,
                    skipTemplateDrafts = false,
                    dryRun = false,
                    createdBy = "user1",
                )

            assertThat(
                received.appliedMetadataHandles,
                `is`(listOf(metadataToDelete.handle)),
            )

            // when
            backend.upsertMetadata(
                listOf(
                    metadataToDelete.copy(deleted = true),
                ),
            )
            val deletionDate = today

            val updates =
                backend.deleteAndUpdateManualRightsByHandle(
                    deletionDate = deletionDate,
                    handle = metadataToDelete.handle,
                )
            assertThat(
                updates,
                `is`(1),
            )

            // then
            val item = backend.getItemByHandle(metadataToDelete.handle)!!

            assertThat(
                item.rights.size,
                `is`(0),
            )
        }

    companion object {
        const val DATA_FOR_CHECK_RIGHT_CONFLICTS = "DATA_FOR_CHECK_RIGHT_CONFLICTS"
        const val DATA_FOR_FIND_RIGHT_CONFLICTS = "DATA_FOR_FIND_RIGHT_CONFLICTS"
        const val DATA_FOR_INVALID_SEARCH_KEY_PARSING = "DATA_FOR_INVALID_SEARCH_KEY_PARSING"
        const val DATA_FOR_SEARCH_KEY_PARSING = "DATA_FOR_SEARCH_KEY_PARSING"
        const val DATA_FOR_REMOVE_VALID_SEARCH_TOKEN = "DATA_FOR_REMOVE_VALID_SEARCH_TOKEN"

        val NOW: OffsetDateTime =
            OffsetDateTime.of(
                2022,
                3,
                1,
                0,
                0,
                0,
                0,
                ZoneOffset.UTC,
            )!!

        val TODAY: LocalDate = LocalDate.of(2022, 3, 1)
        val TEST_METADATA =
            ItemMetadata(
                collectionHandle = "colHandle",
                collectionName = "collectionName",
                communityHandle = "comHandle",
                communityName = "communityName",
                createdBy = "user1",
                createdOn = NOW,
                deleted = false,
                pids = listOf("doi:example.org"),
                econbizId = "123",
                handle = "11159/810",
                isbn = listOf("1234567890123"),
                issn = listOf("123456"),
                isPartOfSeries = listOf("series"),
                lastUpdatedBy = "user2",
                lastUpdatedOn = NOW,
                licenceUrl = "https://creativecommons.org/licenses/by-sa/4.0/legalcode.de",
                licenceUrlFilter = "by-sa/4.0/legalcode.de",
                paketSigel = listOf("sigel"),
                ppn = "ppn",
                publicationType = PublicationType.ARTICLE,
                publicationYear = 2022,
                storageDate = NOW.minusDays(3),
                subCommunityHandle = "11159/1114",
                subCommunityName = "Department",
                title = "Important title",
                zdbIds = listOf("zdbId"),
                econstorIssue = "issue",
                econstorVolume = "volume",
                ppnBook = "ppnBook",
                ppnSeries = "ppnSeries",
                ppnJournal = "ppnJournal",
                isPartOfBook = "part of book",
                isPartOfJournal = "part of journal",
                enumeration = "volume,issue",
            )

        private val TEST_RIGHT =
            ItemRight(
                rightId = "12",
                accessState = AccessState.OPEN,
                basisAccessState = BasisAccessState.LICENCE_CONTRACT,
                basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
                createdBy = "user1",
                createdOn = NOW,
                endDate = TODAY,
                exceptionOfId = null,
                firstAppliedOn = null,
                hasExceptionId = null,
                hasLegalRisk = true,
                groups = emptyList(),
                groupIds = emptyList(),
                isTemplate = false,
                lastAppliedOn = null,
                lastUpdatedBy = "user2",
                lastUpdatedOn = NOW,
                licenceContract = "some contract",
                notesGeneral = "Some general notes",
                notesFormalRules = "Some formal rule notes",
                notesProcessDocumentation = "Some process documentation",
                notesManagementRelated = "Some management related notes",
                predecessorId = null,
                restrictedOpenContentLicence = false,
                successorId = null,
                startDate = TODAY.minusDays(1),
                templateDescription = "descritpion",
                templateName = null,
                zbwUserAgreement = true,
            )
    }
}
