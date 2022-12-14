package de.zbw.persistence.lori.server

import com.google.gson.Gson
import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.SearchKey
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupIpAddress
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.User
import de.zbw.business.lori.server.type.UserRole
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
        tracer = OpenTelemetry.noop().getTracer("foo"),
        gson = Gson().newBuilder().create(),
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
            mockk(),
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
    fun testGroupRoundtrip() {
        // Create

        // when + then
        val receivedGroupId = dbConnector.insertGroup(TEST_GROUP)
        assertThat(
            receivedGroupId,
            `is`(
                TEST_GROUP.name
            ),
        )

        // Get
        // when + then
        assertThat(
            dbConnector.getGroupById(TEST_GROUP.name),
            `is`(
                TEST_GROUP
            ),
        )

        // Update
        // given
        val updated = TEST_GROUP.copy(description = "baz")
        assertThat(
            dbConnector.updateGroup(updated),
            `is`(1),
        )

        // when + then
        assertThat(
            dbConnector.getGroupById(TEST_GROUP.name),
            `is`(
                updated
            ),
        )

        // Delete
        assertThat(
            dbConnector.deleteGroupById(TEST_GROUP.name),
            `is`(
                1
            ),
        )

        // Get no result
        assertNull(
            dbConnector.getGroupById(TEST_GROUP.name),
        )
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
            mockk(),
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
            mockk(),
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
            mockk(),
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
        val testZDB = TEST_Metadata.copy(metadataId = "searchZBD", zdbId = "zbdId")
        dbConnector.insertMetadata(testZDB)

        // when
        val searchTermsZDB = mapOf(Pair(SearchKey.ZDB_ID, listOf(testZDB.zdbId!!)))
        val resultZDB =
            dbConnector.searchMetadata(
                searchTerms = searchTermsZDB,
                limit = 5,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
            )
        val numberResultZDB = dbConnector.countSearchMetadata(
            searchTerms = searchTermsZDB,
            metadataSearchFilter = emptyList(),
        )
        // then
        assertThat(resultZDB[0], `is`(testZDB))
        assertThat(numberResultZDB, `is`(1))
        // when
        val searchTermsAll = mapOf(
            Pair(SearchKey.COLLECTION, listOf(testZDB.collectionName!!)),
            Pair(SearchKey.COMMUNITY, listOf(testZDB.communityName!!)),
            Pair(SearchKey.PAKET_SIGEL, listOf(testZDB.paketSigel!!)),
            Pair(SearchKey.ZDB_ID, listOf(testZDB.zdbId!!)),
        )
        val resultAll =
            dbConnector.searchMetadata(
                searchTerms = searchTermsAll,
                limit = 5,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
            )
        val numberResultAll = dbConnector.countSearchMetadata(
            searchTerms = searchTermsAll,
            metadataSearchFilter = emptyList(),
        )
        // then
        assertThat(resultAll.toSet(), `is`(setOf(testZDB)))
        assertThat(numberResultAll, `is`(1))

        // Add second metadata with same zbdID
        val testZDB2 = TEST_Metadata.copy(metadataId = "searchZBD2", zdbId = "zbdId")
        dbConnector.insertMetadata(testZDB2)
        // when
        val resultZBD2 =
            dbConnector.searchMetadata(
                searchTerms = searchTermsZDB,
                limit = 5,
                offset = 0,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
            )
        val numberResultZDB2 = dbConnector.countSearchMetadata(
            searchTerms = searchTermsAll,
            metadataSearchFilter = emptyList(),
        )
        // then
        assertThat(resultZBD2.toSet(), `is`(setOf(testZDB, testZDB2)))
        assertThat(numberResultZDB2, `is`(2))

        // when
        val resultZDB2Offset =
            dbConnector.searchMetadata(
                searchTerms = searchTermsZDB,
                limit = 5,
                offset = 1,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = emptyList(),
            )
        assertThat(
            resultZDB2Offset.size, `is`(1)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    private fun createBuildSearchQueryData() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_col,1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,collection_name <-> ? as dist_col FROM item_metadata) as sub WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for one string in collection",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo"), SearchKey.PAKET_SIGEL to listOf("bar")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for multiple searchkeys",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for multiple words in one searchkey",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                emptyList<MetadataSearchFilter>(),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for multiple words in multiple searchkeys"
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for publication date filter"
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("bar")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "query for publication date and publication type filter"
            )
        )

    @Test(dataProvider = DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithOnlyMetadataFilter(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            DatabaseConnector.buildSearchQuery(
                searchKeys,
                metadataSearchFilter,
                emptyList(),
            ),
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun createDataForBuildSearchQueryBoth() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_col,1))/1 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state,collection_name <-> ? as dist_col FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "right filter only"
            ),
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE, PublicationType.PROCEEDINGS
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.CLOSED))),
                "SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_col,1))/1 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state,collection_name <-> ? as dist_col FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE sub.dist_col < 0.9 ORDER BY score LIMIT ? OFFSET ?",
                "right filter combined with metadatafilter"
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY)
    fun testBuildSearchQueryWithBothFilterTypes(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            DatabaseConnector.buildSearchQuery(
                searchKeys,
                metadataSearchFilter,
                rightSearchFilter,
            ),
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    private fun createBuildSearchCountQueryData() =
        arrayOf(
            arrayOf(
                mapOf(SearchKey.COLLECTION to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM" +
                    " (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_col,1))/1 as score" +
                    " FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,collection_name <-> ? as dist_col FROM item_metadata) as sub WHERE sub.dist_col < 0.9 ORDER BY score) as foo",
                "count query filter with one searchkey",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo"), SearchKey.PAKET_SIGEL to listOf("foo")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as foo",
                "count query filter with two searchkeys",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1))/1 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 ORDER BY score) as foo",
                "count query filter with multiple words for one key",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
                emptyList<MetadataSearchFilter>(),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as foo",
                "count query with multiple words for multiple keys",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata WHERE publication_date >= ? AND publication_date <= ?) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as foo",
                "count query with one filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf<MetadataSearchFilter>(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                ),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? ORDER BY item_metadata.metadata_id ASC) as foo",
                "count query without keys but with filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                emptyList<RightSearchFilter>(),
                "SELECT COUNT(*) FROM (SELECT item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date FROM item_metadata WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) ORDER BY item_metadata.metadata_id ASC) as foo",
                "count query without keys but with filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.RESTRICTED, AccessState.CLOSED))),
                "SELECT COUNT(*) FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) ORDER BY item_metadata.metadata_id ASC) as foo",
                "count query only with right search filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.RESTRICTED, AccessState.CLOSED))),
                "SELECT COUNT(*) FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?) ORDER BY item_metadata.metadata_id ASC) as foo",
                "count query without keys but with both filter",
            ),
            arrayOf(
                mapOf(SearchKey.ZDB_ID to listOf("foo", "bar"), SearchKey.PAKET_SIGEL to listOf("baz")),
                listOf(
                    PublicationDateFilter(fromYear = 2016, toYear = 2022),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    )
                ),
                listOf(AccessStateFilter(listOf(AccessState.RESTRICTED, AccessState.CLOSED))),
                "SELECT COUNT(*) FROM (SELECT metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,created_on,last_updated_on,created_by,last_updated_by,author,collection_name,community_name,storage_date,(coalesce(sub.dist_zdb,1) + coalesce(sub.dist_sig,1))/2 as score FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state,zdb_id <-> ? as dist_zdb,paket_sigel <-> ? as dist_sig FROM item_metadata LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?) WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ? OR publication_type = ?)) as sub WHERE sub.dist_zdb < 0.9 AND sub.dist_sig < 0.9 ORDER BY score) as foo",
                "count query with keys and with both filter",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SEARCH_COUNT_QUERY)
    fun testBuildSearchCountQuery(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedWhereClause: String,
        description: String,
    ) {
        assertThat(
            description,
            DatabaseConnector.buildCountSearchQuery(
                searchKeys,
                metadataSearchFilter,
                rightSearchFilter
            ),
            `is`(expectedWhereClause)
        )
    }

    @DataProvider(name = DATA_FOR_METASEARCH_QUERY)
    private fun createMetasearchQueryWithFilterNoSearch() =
        arrayOf(
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                DatabaseConnector.STATEMENT_GET_METADATA_RANGE + " ORDER BY metadata_id ASC LIMIT ? OFFSET ?;",
                "metasearch query without filter",
            ),
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019)),
                DatabaseConnector.STATEMENT_GET_METADATA_RANGE + " WHERE publication_date >= ? AND publication_date <= ? ORDER BY metadata_id ASC LIMIT ? OFFSET ?;",
                "metasearch query with one filter",
            ),
            arrayOf(
                listOf(
                    PublicationDateFilter(2000, 2019),
                    PublicationTypeFilter(
                        listOf(
                            PublicationType.ARTICLE,
                            PublicationType.PROCEEDINGS,
                        )
                    ),
                ),
                DatabaseConnector.STATEMENT_GET_METADATA_RANGE + " WHERE publication_date >= ? AND publication_date <= ? AND" +
                    " (publication_type = ? OR publication_type = ?) ORDER BY metadata_id ASC LIMIT ? OFFSET ?;",
                "metasearch query with multiple filter",
            ),
        )

    @DataProvider(name = DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH)
    private fun createBuildCountQueryRightFilterNoSearch() =
        arrayOf(
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM" +
                    " (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC) as foo",
                "both filter"
            ),
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT COUNT(*) FROM" +
                    " (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC) as foo",
                "only right filter"
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH)
    fun testBuildCountQueryBothFilterNoSearch(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            DatabaseConnector.buildCountSearchQuery(
                emptyMap(),
                metadataSearchFilter,
                rightSearchFilter,
            ),
            `is`(expectedSQLQuery)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY)
    private fun createMetadataQueryFilterNoSearchWithRightFilter() =
        arrayOf(
            arrayOf(
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal," +
                    "title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn," +
                    "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by," +
                    "item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC LIMIT ? OFFSET ?",
                "query only right filter",
            ),
            arrayOf(
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title,title_journal," +
                    "title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus,paket_sigel,zdb_id,issn," +
                    "item_metadata.created_on,item_metadata.last_updated_on,item_metadata.created_by," +
                    "item_metadata.last_updated_by,author,collection_name,community_name,storage_date,item_right.access_state" +
                    " FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)" +
                    " ORDER BY item_metadata.metadata_id ASC LIMIT ? OFFSET ?",
                "query with both filters",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY)
    fun testBuildMetadataQueryFilterNoSearchWithRightFilter(
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            DatabaseConnector.buildSearchQuery(
                emptyMap(),
                metadataSearchFilter,
                rightSearchFilter,
            ),
            `is`(expectedSQLQuery)
        )
    }

    @DataProvider(name = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    private fun createQueryFilterSearchForSigelAndZDB() =
        arrayOf(
            arrayOf(
                listOf(SearchKey.COLLECTION to "foo").toMap(),
                emptyList<MetadataSearchFilter>(),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT sub.access_state, sub.paket_sigel, sub.publication_type, sub.zdb_id" +
                    " FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state,collection_name <-> ? as dist_col FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)) as sub" +
                    " WHERE sub.dist_col < 0.9" +
                    " GROUP BY sub.access_state, sub.paket_sigel, sub.publication_type, sub.zdb_id;",
                "query with search and right filter",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                listOf(AccessStateFilter(listOf(AccessState.OPEN, AccessState.RESTRICTED))),
                "SELECT sub.access_state, sub.paket_sigel, sub.publication_type, sub.zdb_id" +
                    " FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state FROM item_metadata" +
                    " LEFT JOIN item ON item.metadata_id = item_metadata.metadata_id" +
                    " JOIN item_right ON item.right_id = item_right.right_id AND (access_state = ? OR access_state = ?)" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) as sub" +
                    " GROUP BY sub.access_state, sub.paket_sigel, sub.publication_type, sub.zdb_id;",
                "query with both filters and no searchkey",
            ),
            arrayOf(
                emptyMap<SearchKey, List<String>>(),
                listOf(PublicationDateFilter(2000, 2019), PublicationTypeFilter(listOf(PublicationType.PROCEEDINGS))),
                emptyList<RightSearchFilter>(),
                "SELECT sub.access_state, sub.paket_sigel, sub.publication_type, sub.zdb_id" +
                    " FROM (SELECT DISTINCT ON (item_metadata.metadata_id) item_metadata.metadata_id,handle,ppn,title," +
                    "title_journal,title_series,publication_date,band,publication_type,doi,isbn,rights_k10plus," +
                    "paket_sigel,zdb_id,issn,item_metadata.created_on,item_metadata.last_updated_on," +
                    "item_metadata.created_by,item_metadata.last_updated_by,author,collection_name,community_name," +
                    "storage_date,item_right.access_state" +
                    " FROM item_metadata LEFT JOIN item" +
                    " ON item.metadata_id = item_metadata.metadata_id" +
                    " LEFT JOIN item_right ON item.right_id = item_right.right_id" +
                    " WHERE publication_date >= ? AND publication_date <= ? AND (publication_type = ?)) as sub" +
                    " GROUP BY sub.access_state, sub.paket_sigel, sub.publication_type, sub.zdb_id;",
                "query with metadata filter only",
            ),
        )

    @Test(dataProvider = DATA_FOR_BUILD_SIGEL_AND_ZDB)
    fun testBuildMetadataQueryForSigelAndZDB(
        searchKeys: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
        expectedSQLQuery: String,
        description: String,
    ) {
        assertThat(
            description,
            DatabaseConnector.buildSearchQueryForFacets(
                searchKeys,
                metadataSearchFilter,
                rightSearchFilter,
                true,
            ),
            `is`(expectedSQLQuery)
        )
    }

    companion object {
        const val DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_METADATA_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_SEARCH_QUERY"
        const val DATA_FOR_BUILD_SEARCH_COUNT_QUERY = "DATA_FOR_BUILD_SEARCH_COUNT_QUERY"
        const val DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH =
            "DATA_FOR_BUILD_COUNT_QUERY_RIGHT_FILTER_NO_SEARCH "
        const val DATA_FOR_METASEARCH_QUERY = "DATA_FOR_METASEARCH_QUERY"
        const val DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY = "DATA_FOR_BUILD_BOTH_FILTER_NO_SEARCH_QUERY "

        const val DATA_FOR_BUILD_SIGEL_AND_ZDB = "DATA_FOR_BUILD_SIGEL_AND_ZDB"
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

        val TEST_GROUP = Group(
            name = "test group",
            description = "some description",
            ipAddresses = listOf(
                GroupIpAddress(
                    organisationName = "some organisation",
                    ipAddress = "192.168.0.0",
                ),
            ),
        )

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
            zdbId = "some id",
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
