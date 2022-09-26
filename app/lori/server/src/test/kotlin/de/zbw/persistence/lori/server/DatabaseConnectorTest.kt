package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.BasisAccessState
import de.zbw.business.lori.server.BasisStorage
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.PublicationType
import de.zbw.business.lori.server.SearchKey
import de.zbw.business.lori.server.User
import de.zbw.business.lori.server.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Testing [DatabaseConnector].
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnectorTest : DatabaseTest() {
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo")
    )

    @BeforeMethod
    fun beforeTest() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.toInstant()
    }

    @AfterMethod
    fun afterTest() {
        unmockkAll()
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testInsertHeaderException() {

        // given
        val testHeaderId = "double_entry"
        val testMetadata = TEST_Metadata.copy(metadataId = testHeaderId)

        // when
        dbConnector.insertMetadata(testMetadata)

        // exception
        dbConnector.insertMetadata(testMetadata)
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertMetadataNoInsertError() {
        // given
        val prepStmt = spyk(dbConnector.connection.prepareStatement(DatabaseConnector.STATEMENT_INSERT_METADATA)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection>(relaxed = true) {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            },
            tracer,
        )
        // when
        dbConnectorMockked.insertMetadata(TEST_Metadata)
        // then exception
    }

    @Test
    fun testMetadataRoundtrip() {
        // given
        val testId = "id_test"
        val testMetadata = TEST_Metadata.copy(metadataId = testId, title = "foo")

        // when
        val responseInsert = dbConnector.insertMetadata(testMetadata)

        // then
        assertThat(responseInsert, `is`(testId))

        // when
        val receivedMetadata: List<ItemMetadata> = dbConnector.getMetadata(listOf(testId))

        // then
        assertThat(
            receivedMetadata.first(), `is`(testMetadata)
        )

        // when
        assertThat(
            dbConnector.getMetadata(listOf("not_in_db")), `is`(listOf())
        )

        // when
        val deletedMetadata = dbConnector.deleteMetadata(listOf(testId))

        // then
        assertThat(deletedMetadata, `is`(1))
        assertThat(dbConnector.getMetadata(listOf(testId)), `is`(listOf()))
    }

    @Test
    fun testBatchUpsert() {
        // given
        val id1 = "upsert1"
        val id2 = "upsert2"
        val m1 = TEST_Metadata.copy(metadataId = id1, title = "foo")
        val m2 = TEST_Metadata.copy(metadataId = id2, title = "bar")

        // when
        val responseUpsert = dbConnector.upsertMetadataBatch(listOf(m1, m2))

        // then
        assertThat(responseUpsert, `is`(IntArray(2) { 1 }))

        // when
        val receivedM1: List<ItemMetadata> = dbConnector.getMetadata(listOf(id1))

        // then
        assertThat(
            receivedM1.first(), `is`(m1)
        )

        val receivedM2: List<ItemMetadata> = dbConnector.getMetadata(listOf(id2))

        // then
        assertThat(
            receivedM2.first(), `is`(m2)
        )

        // when
        unmockkAll()

        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.plusDays(1).toInstant()
        val m1Changed = m1.copy(title = "foo2", lastUpdatedBy = "user2", lastUpdatedOn = NOW.plusDays(1))
        val m2Changed = m2.copy(title = "bar2", lastUpdatedBy = "user2", lastUpdatedOn = NOW.plusDays(1))

        val responseUpsert2 = dbConnector.upsertMetadataBatch(listOf(m1Changed, m2Changed))

        // then
        assertThat(responseUpsert2, `is`(IntArray(2) { 1 }))

        // when
        val receivedM1Changed: List<ItemMetadata> = dbConnector.getMetadata(listOf(id1))

        // then
        assertThat(
            receivedM1Changed.first(), `is`(m1Changed)
        )

        val receivedM2Changed: List<ItemMetadata> = dbConnector.getMetadata(listOf(id2))

        // then
        assertThat(
            receivedM2Changed.first(), `is`(m2Changed)
        )
    }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun testInsertRightNoRowInsertedError() {
        // given
        val prepStmt = spyk(dbConnector.connection.prepareStatement(DatabaseConnector.STATEMENT_INSERT_RIGHT)) {
            every { executeUpdate() } returns 0
        }
        val dbConnectorMockked = DatabaseConnector(
            mockk<Connection>(relaxed = true) {
                every { prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns prepStmt
            },
            tracer,
        )
        // when
        dbConnectorMockked.insertRight(TEST_RIGHT)
        // then exception
    }

    @Test
    fun testRightRoundtrip() {
        // given
        val initialRight = TEST_RIGHT

        // Insert
        // when
        val generatedRightId = dbConnector.insertRight(initialRight)
        val receivedRights: List<ItemRight> = dbConnector.getRights(listOf(generatedRightId))

        // then
        assertThat(receivedRights.first(), `is`(initialRight.copy(rightId = generatedRightId)))
        assertTrue(dbConnector.rightContainsId(generatedRightId))

        // upsert

        // given
        val updatedRight =
            initialRight.copy(rightId = generatedRightId, lastUpdatedBy = "user2", accessState = AccessState.RESTRICTED)
        mockkStatic(Instant::class)
        every { Instant.now() } returns NOW.plusDays(1).toInstant()

        // when
        val updatedRights = dbConnector.upsertRight(updatedRight)

        // then
        assertThat(updatedRights, `is`(1))
        val receivedUpdatedRights: List<ItemRight> = dbConnector.getRights(listOf(generatedRightId))
        assertThat(receivedUpdatedRights.first(), `is`(updatedRight.copy(lastUpdatedOn = NOW.plusDays(1))))

        // delete
        // when
        val deletedItems = dbConnector.deleteRights(listOf(generatedRightId))

        // then
        assertThat(deletedItems, `is`(1))

        // when + then
        assertThat(dbConnector.getRights(listOf(generatedRightId)), `is`(emptyList()))
        assertFalse(dbConnector.rightContainsId(generatedRightId))
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetMetadataException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>(relaxed = true) {
                every { prepareStatement(any()) } throws SQLException()
            },
            tracer,
        )
        dbConnector.getMetadata(listOf("foo"))
    }

    @Test(expectedExceptions = [SQLException::class])
    fun testGetRightException() {
        val dbConnector = DatabaseConnector(
            mockk<Connection>(relaxed = true) {
                every { prepareStatement(any()) } throws SQLException()
            },
            tracer,
        )
        dbConnector.getRights(listOf("1"))
    }

    @Test
    fun testContainsMetadata() {

        // given
        val metadataId = "metadataIdContainCheck"
        val expectedMetadata = TEST_Metadata.copy(metadataId = metadataId)

        // when
        val containedBefore = dbConnector.metadataContainsId(metadataId)
        assertFalse(containedBefore, "Metadata should not exist yet")

        // when
        dbConnector.insertMetadata(expectedMetadata)
        val containedAfter = dbConnector.metadataContainsId(metadataId)
        assertTrue(containedAfter, "Metadata should exist now")
    }

    @Test
    fun testMetadataRange() {
        // given
        val givenMetadata =
            listOf(
                TEST_Metadata.copy(metadataId = "aaaa"),
                TEST_Metadata.copy(metadataId = "aaab"),
                TEST_Metadata.copy(metadataId = "aaac"),
            )
        // when
        givenMetadata.map {
            dbConnector.insertMetadata(it)
        }

        // then
        assertThat(
            dbConnector.getMetadataRange(limit = 3, offset = 0).toSet(),
            `is`(givenMetadata.toSet())
        )
        assertThat(
            dbConnector.getMetadataRange(limit = 2, offset = 1).toSet(),
            `is`(givenMetadata.subList(1, 3).toSet())
        )
    }

    @Test
    fun testDeleteItem() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "item_roundtrip_meta")
        val expectedRight = TEST_RIGHT

        // when
        dbConnector.insertMetadata(expectedMetadata)
        val generatedRightId = dbConnector.insertRight(expectedRight)
        dbConnector.insertItem(expectedMetadata.metadataId, generatedRightId)

        // then
        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(generatedRightId))
        )

        val deletedItems = dbConnector.deleteItem(expectedMetadata.metadataId, generatedRightId)
        assertThat(
            deletedItems,
            `is`(1),
        )

        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )
    }

    @Test
    fun testDeleteItemBy() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "delete_item_meta")
        val expectedRight = TEST_RIGHT

        // when
        dbConnector.insertMetadata(expectedMetadata)
        val generatedRightId = dbConnector.insertRight(expectedRight)
        dbConnector.insertItem(expectedMetadata.metadataId, generatedRightId)

        // then
        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(generatedRightId))
        )

        val deletedItemsByMetadata = dbConnector.deleteItemByMetadata(expectedMetadata.metadataId)
        assertThat(
            deletedItemsByMetadata,
            `is`(1),
        )

        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )

        // when
        dbConnector.insertItem(expectedMetadata.metadataId, generatedRightId)
        // then
        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(listOf(generatedRightId))
        )

        val deletedItemsByRight = dbConnector.deleteItemByRight(generatedRightId)
        assertThat(
            deletedItemsByRight,
            `is`(1),
        )

        assertThat(
            dbConnector.getRightIdsByMetadata(expectedMetadata.metadataId),
            `is`(emptyList())
        )
    }

    @Test
    fun testItemExists() {
        // given
        val expectedMetadata = TEST_Metadata.copy(metadataId = "item_exists_metadata")
        val expectedRight = TEST_RIGHT

        assertFalse(dbConnector.itemContainsRight(expectedRight.rightId!!))
        assertFalse(dbConnector.itemContainsEntry(expectedMetadata.metadataId, expectedRight.rightId!!))
        assertFalse(dbConnector.itemContainsMetadata(expectedMetadata.metadataId))
        // when
        dbConnector.insertMetadata(expectedMetadata)
        val generatedRightId = dbConnector.insertRight(expectedRight)
        dbConnector.insertItem(expectedMetadata.metadataId, generatedRightId)

        // then
        assertTrue(dbConnector.itemContainsRight(generatedRightId))
        assertTrue(dbConnector.itemContainsMetadata(expectedMetadata.metadataId))
        assertTrue(dbConnector.itemContainsEntry(expectedMetadata.metadataId, generatedRightId))
        assertThat(dbConnector.countItemByRightId(generatedRightId), `is`(1))
    }

    @Test
    fun testUsernameExistsRoundtrip() {
        // given
        val expectedUser = TEST_USER

        assertFalse(dbConnector.userTableContainsName(expectedUser.name))
        // when
        val userName = dbConnector.insertUser(expectedUser)

        // then
        assertThat(userName, `is`(TEST_USER.name))
        assertTrue(dbConnector.userTableContainsName(expectedUser.name))

        // when
        val deletedUsers = dbConnector.deleteUser(expectedUser.name)
        assertThat(deletedUsers, `is`(1))
        assertFalse(dbConnector.userTableContainsName(expectedUser.name))
    }

    @Test
    fun testUserExistsByNameAndPassword() {
        // given
        val expectedUser = TEST_USER.copy(
            name = "testUserExists",
        )

        assertFalse(dbConnector.userTableContainsName(expectedUser.name))
        // when
        val userName = dbConnector.insertUser(expectedUser)

        // then
        assertThat(userName, `is`(expectedUser.name))
        assertTrue(dbConnector.userTableContainsName(expectedUser.name))

        // when
        assertTrue(
            dbConnector.userExistsByNameAndPassword(
                expectedUser.name,
                expectedUser.passwordHash
            )
        )
    }

    @Test
    fun testUserDoesNotExistsByNameAndPassword() {
        // given
        val expectedUser = TEST_USER.copy(
            name = notExistingUsername,
        )

        assertFalse(dbConnector.userTableContainsName(expectedUser.name))
        // when
        val userName = dbConnector.insertUser(expectedUser)

        // then
        assertThat(userName, `is`(expectedUser.name))
        assertTrue(dbConnector.userTableContainsName(expectedUser.name))

        // when
        assertFalse(
            dbConnector.userExistsByNameAndPassword(
                expectedUser.name,
                expectedUser.passwordHash + "$",
            )
        )
    }

    @Test
    fun testGetRoleByUsername() {
        // given
        val expectedUser = TEST_USER.copy(
            name = "testGetRoleByExistingUsername",
            role = UserRole.READWRITE,
        )

        // when
        val userName = dbConnector.insertUser(expectedUser)
        // then
        assertThat(userName, `is`(expectedUser.name))

        // when
        val receivedRole = dbConnector.getRoleByUsername(expectedUser.name)
        // then
        assertThat(receivedRole, `is`(expectedUser.role))

        // when
        val receivedRoleNonExistingUser = dbConnector.getRoleByUsername(notExistingUsername)
        // then
        assertNull(receivedRoleNonExistingUser)
    }

    @Test
    fun testUpdateUserNonRoleProperties() {
        // given
        val beforeUpdateUser = TEST_USER.copy(
            name = "testUpdateUserNonRoleProp",
            role = UserRole.READONLY,
        )
        dbConnector.insertUser(beforeUpdateUser)

        val afterUpdateUser = beforeUpdateUser.copy(
            passwordHash = "foobar23456"
        )

        dbConnector.updateUserNonRoleProperties(afterUpdateUser)
        assertThat(
            dbConnector.getUserByName(afterUpdateUser.name),
            `is`(afterUpdateUser),
        )
    }

    @Test
    fun testUpdateUserRoleProperty() {
        // given
        val beforeUpdateUser = TEST_USER.copy(
            name = "testUpdateUserRoleProp",
            role = UserRole.READONLY,
        )
        dbConnector.insertUser(beforeUpdateUser)

        val afterUpdateUser = beforeUpdateUser.copy(
            role = UserRole.READWRITE
        )

        // when
        dbConnector.updateUserRoleProperty(afterUpdateUser.name, afterUpdateUser.role!!)

        // then
        assertThat(
            dbConnector.getUserByName(afterUpdateUser.name),
            `is`(afterUpdateUser),
        )
    }

    @Test
    fun searchMetadata() {
        // given
        val testZBD = TEST_Metadata.copy(metadataId = "searchZBD", zbdId = "zbdId")
        dbConnector.insertMetadata(testZBD)

        // when
        val searchTermsZBD = mapOf(Pair(SearchKey.ZBD_ID, listOf(testZBD.zbdId!!)))
        val resultZBD =
            dbConnector.searchMetadata(
                searchTerms = searchTermsZBD,
                limit = 5,
                offset = 0,
            )
        val numberResultZBD = dbConnector.countSearchMetadata(
            searchTerms = searchTermsZBD,
        )
        // then
        assertThat(resultZBD[0], `is`(testZBD))
        assertThat(numberResultZBD, `is`(1))
        // when
        val searchTermsAll = mapOf(
            Pair(SearchKey.COLLECTION, listOf(testZBD.collectionName!!)),
            Pair(SearchKey.COMMUNITY, listOf(testZBD.communityName!!)),
            Pair(SearchKey.PAKET_SIGEL, listOf(testZBD.paketSigel!!)),
            Pair(SearchKey.ZBD_ID, listOf(testZBD.zbdId!!)),
        )
        val resultAll =
            dbConnector.searchMetadata(
                searchTerms = searchTermsAll,
                limit = 5,
                offset = 0,
            )
        val numberResultAll = dbConnector.countSearchMetadata(
            searchTerms = searchTermsAll
        )
        // then
        assertThat(resultAll.toSet(), `is`(setOf(testZBD)))
        assertThat(numberResultAll, `is`(1))

        // Add second metadata with same zbdID
        val testZBD2 = TEST_Metadata.copy(metadataId = "searchZBD2", zbdId = "zbdId")
        dbConnector.insertMetadata(testZBD2)
        // when
        val resultZBD2 =
            dbConnector.searchMetadata(
                searchTerms = searchTermsZBD,
                limit = 5,
                offset = 0,
            )
        val numberResultZBD2 = dbConnector.countSearchMetadata(
            searchTerms = searchTermsAll
        )
        // then
        assertThat(resultZBD2.toSet(), `is`(setOf(testZBD, testZBD2)))
        assertThat(numberResultZBD2, `is`(2))

        // when
        val resultZBD2Offset =
            dbConnector.searchMetadata(
                searchTerms = searchTermsZBD,
                limit = 5,
                offset = 1,
            )
        assertThat(
            resultZBD2Offset.size, `is`(1)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SEARCH_QUERY)
    private fun createBuildSearchQueryData() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                DatabaseConnector.STATEMENT_SELECT_ALL_METADATA + " WHERE ts_collection @@ to_tsquery('english', ?) LIMIT ? OFFSET ?;",
            ),
            arrayOf(
                mapOf(SearchKey.ZBD_ID to listOf("foo"), SearchKey.PAKET_SIGEL to listOf("bar")),
                DatabaseConnector.STATEMENT_SELECT_ALL_METADATA + " WHERE ts_zbd_id @@ to_tsquery('english', ?) AND ts_sigel @@ to_tsquery('english', ?) LIMIT ? OFFSET ?;",
            ),
            arrayOf(
                mapOf(SearchKey.ZBD_ID to listOf("foo", "bar")),
                DatabaseConnector.STATEMENT_SELECT_ALL_METADATA + " WHERE ts_zbd_id @@ to_tsquery('english', ?) LIMIT ? OFFSET ?;",
            ),
            arrayOf(
                mapOf(SearchKey.ZBD_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                DatabaseConnector.STATEMENT_SELECT_ALL_METADATA + " WHERE ts_zbd_id @@ to_tsquery('english', ?) AND ts_sigel @@ to_tsquery('english', ?) LIMIT ? OFFSET ?;",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SEARCH_QUERY)
    fun testBuildSearchQuery(searchKeys: Map<SearchKey, List<String>>, expectedWhereClause: String) {
        assertThat(dbConnector.buildSearchQuery(searchKeys), `is`(expectedWhereClause))
    }

    @DataProvider(name = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    private fun createBuildSearchCountQueryData() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                DatabaseConnector.STATEMENT_COUNT_METADATA + " WHERE ts_collection @@ to_tsquery('english', ?);",
            ),
            arrayOf(
                mapOf(SearchKey.ZBD_ID to listOf("foo"), SearchKey.PAKET_SIGEL to listOf("foo")),
                DatabaseConnector.STATEMENT_COUNT_METADATA + " WHERE ts_zbd_id @@ to_tsquery('english', ?) AND ts_sigel @@ to_tsquery('english', ?);",
            ),
            arrayOf(
                mapOf(SearchKey.ZBD_ID to listOf("foo", "bar")),
                DatabaseConnector.STATEMENT_COUNT_METADATA + " WHERE ts_zbd_id @@ to_tsquery('english', ?);",
            ),
            arrayOf(
                mapOf(SearchKey.ZBD_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
                DatabaseConnector.STATEMENT_COUNT_METADATA + " WHERE ts_zbd_id @@ to_tsquery('english', ?) AND ts_sigel @@ to_tsquery('english', ?);",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    fun testBuildSearchCountQuery(searchKeys: Map<SearchKey, List<String>>, expectedWhereClause: String) {
        assertThat(dbConnector.buildCountSearchQuery(searchKeys), `is`(expectedWhereClause))
    }

    companion object {
        const val DATA_FOR_BUILD_SEARCH_QUERY = "DATA_FOR_BUILD_SEARCH_QUERY"
        const val DATA_FOR_BUILD_SEARCH_COUNT_QUERY = "DATA_FOR_BUILD_SEARCH_COUNT_QUERY"
        const val notExistingUsername = "notExistentUser"
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
            titleJournal = "anything",
            titleSeries = null,
            zbdId = "some id",
        )

        private val TEST_RIGHT = ItemRight(
            rightId = "testright",
            accessState = AccessState.OPEN,
            authorRightException = true,
            basisAccessState = BasisAccessState.LICENCE_CONTRACT,
            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
            createdBy = "user1",
            createdOn = NOW,
            endDate = TODAY,
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

        private val TEST_USER = User(
            name = "Bob",
            passwordHash = "122345",
            role = UserRole.ADMIN,
        )
    }

    private val tracer: Tracer = OpenTelemetry.noop().getTracer("de.zbw.api.lori.server.DatabaseConnectorTest")
}
