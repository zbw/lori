package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertNull

/**
 * Testing [GroupDB].
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class GroupDBTest : DatabaseTest() {
    private val dbConnector =
        DatabaseConnector(
            connectionPool = ConnectionPool(testDataSource),
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).groupDB

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun testGroupRoundTrip() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            // Create

            // when + then
            val receivedGroupId = dbConnector.insertGroup(TEST_GROUP)
            val expectedGroup =
                TEST_GROUP.copy(
                    groupId = receivedGroupId,
                    lastUpdatedOn = NOW,
                    createdOn = NOW,
                    lastUpdatedBy = "user1",
                )

            // Get
            // when + then
            assertThat(
                dbConnector.getGroupById(receivedGroupId),
                `is`(
                    expectedGroup,
                ),
            )

            // Update
            // given

            every { Instant.now() } returns NOW.plusDays(1L).toInstant()
            val updated =
                expectedGroup.copy(
                    description = "baz",
                    lastUpdatedBy = "user2",
                )
            assertThat(
                dbConnector.updateGroup(updated),
                `is`(1),
            )

            // when + then
            assertThat(
                dbConnector.getGroupById(expectedGroup.groupId),
                `is`(
                    updated.copy(lastUpdatedOn = NOW.plusDays(1L)),
                ),
            )

            // Delete
            assertThat(
                dbConnector.deleteGroupById(expectedGroup.groupId),
                `is`(
                    1,
                ),
            )

            // Get no result
            assertNull(
                dbConnector.getGroupById(expectedGroup.groupId),
            )
            unmockkAll()
        }

    @Test
    fun testGetGroupList() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            val receivedGroupId1 = dbConnector.insertGroup(TEST_GROUP)
            val expectedGroup1 =
                TEST_GROUP.copy(
                    groupId = receivedGroupId1,
                    lastUpdatedBy = TEST_GROUP.createdBy,
                    createdOn = NOW,
                    lastUpdatedOn = NOW,
                )
            assertThat(
                dbConnector.getGroupList(50, 0),
                `is`(
                    listOf(
                        expectedGroup1,
                    ),
                ),
            )

            val secondGroup =
                TEST_GROUP.copy(
                    title = "another titler",
                    description = "big description",
                    entries =
                        listOf(
                            GroupEntry(
                                organisationName = "some other organisation",
                                ipAddresses = "192.168.0.1",
                            ),
                        ),
                )
            val receivedGroupId2 =
                dbConnector.insertGroup(
                    secondGroup,
                )
            val expectedGroup2 =
                secondGroup.copy(
                    groupId = receivedGroupId2,
                    lastUpdatedBy = TEST_GROUP.createdBy,
                    createdOn = NOW,
                    lastUpdatedOn = NOW,
                )

            assertThat(
                dbConnector.getGroupList(50, 0),
                `is`(
                    listOf(
                        expectedGroup1,
                        expectedGroup2,
                    ),
                ),
            )
            unmockkAll()
        }

    companion object {
        val NOW =
            OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            )
        val TEST_GROUP =
            Group(
                groupId = 1,
                description = "some description",
                entries =
                    listOf(
                        GroupEntry(
                            organisationName = "some organisation",
                            ipAddresses = "192.168.0.0",
                        ),
                    ),
                title = "test group",
                createdOn = NOW.minusMonths(1L),
                lastUpdatedOn = NOW,
                createdBy = "user1",
                lastUpdatedBy = "user2",
            )
    }
}
