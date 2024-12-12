package de.zbw.business.lori.server

import de.zbw.api.lori.server.exception.ResourceStillInUseException
import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import de.zbw.persistence.lori.server.GroupDBTest.Companion.NOW
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.opentelemetry.api.OpenTelemetry
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.fail
import org.testng.annotations.AfterClass
import org.testng.annotations.Test
import java.time.Instant

/**
 * Test function related to Group-Right entries.
 *
 * Created on 01-23-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightGroupTest : DatabaseTest() {
    private val backend =
        LoriServerBackend(
            DatabaseConnector(
                connectionPool = ConnectionPool(testDataSource),
                tracer = OpenTelemetry.noop().getTracer("foo"),
            ),
            mockk(),
        )

    @AfterClass
    fun afterTests() {
        unmockkAll()
    }

    @Test
    fun rightGroupRoundtrip() =
        runBlocking {
            mockkStatic(Instant::class)
            every { Instant.now() } returns NOW.toInstant()
            // Given group
            val group1 =
                Group(
                    groupId = 55,
                    description = null,
                    entries =
                        listOf(
                            GroupEntry(
                                organisationName = "orga1",
                                ipAddresses = "192.168.1.*",
                            ),
                        ),
                    title = "some title",
                    createdOn = NOW.minusMonths(1L),
                    lastUpdatedOn = NOW,
                    createdBy = "user1",
                    lastUpdatedBy = "user2",
                )

            // Insert group
            val receivedGroupId1 = backend.insertGroup(group1)
            val expectedGroup1 =
                group1.copy(
                    groupId = receivedGroupId1,
                    lastUpdatedOn = NOW,
                    createdOn = NOW,
                    lastUpdatedBy = "user1",
                )

            // Insert Right using the group
            val initialRight: ItemRight =
                TEST_RIGHT.copy(
                    groups =
                        listOf(
                            expectedGroup1,
                        ),
                )

            val rightId1 = backend.insertRight(initialRight)

            // Verify insertions
            assertThat(
                backend.getGroupById(receivedGroupId1),
                `is`(expectedGroup1),
            )

            assertThat(
                backend.dbConnector.groupDB.getGroupsByRightId(rightId1),
                `is`(
                    listOf(expectedGroup1),
                ),
            )

            // Update Right with another group
            val groupName2 = 2
            val group2 =
                Group(
                    groupId = groupName2,
                    description = null,
                    entries =
                        listOf(
                            GroupEntry(
                                organisationName = "orga2",
                                ipAddresses = "192.168.1.*",
                            ),
                        ),
                    title = "some title2",
                    createdOn = NOW.minusMonths(1L),
                    lastUpdatedOn = NOW,
                    createdBy = "user1",
                    lastUpdatedBy = "user2",
                )

            val receivedGroupId2 = backend.insertGroup(group2)
            val expectedGroup2 =
                group2.copy(
                    groupId = receivedGroupId2,
                    createdOn = NOW,
                    lastUpdatedOn = NOW,
                    lastUpdatedBy = "user1",
                )
            val rightUpdated =
                TEST_RIGHT.copy(
                    rightId = rightId1,
                    groupIds =
                        listOf(
                            receivedGroupId2,
                            receivedGroupId1,
                        ),
                )
            backend.upsertRight(rightUpdated)

            // Verify group update was successful
            assertThat(
                backend.dbConnector.groupDB
                    .getGroupsByRightId(rightId1)
                    .toSet(),
                `is`(
                    setOf(expectedGroup1, expectedGroup2),
                ),
            )

            // Another update, this time a group is removed
            val rightUpdated2 =
                TEST_RIGHT.copy(
                    rightId = rightId1,
                    groupIds = listOf(receivedGroupId2),
                )
            backend.upsertRight(rightUpdated2)

            // Verify update again
            assertThat(
                backend.dbConnector.groupDB
                    .getGroupsByRightId(rightId1)
                    .toSet(),
                `is`(
                    setOf(expectedGroup2),
                ),
            )

            // Try to update remaining via Right update path -> not allowed
            val rightUpdated3 =
                TEST_RIGHT.copy(
                    rightId = rightId1,
                    groups =
                        listOf(
                            expectedGroup2.copy(
                                description = "some nonesense",
                            ),
                        ),
                    groupIds = listOf(receivedGroupId2),
                )
            backend.upsertRight(rightUpdated3)

            // Verify update again
            assertThat(
                backend.dbConnector.groupDB
                    .getGroupsByRightId(rightId1)
                    .toSet(),
                `is`(
                    setOf(expectedGroup2),
                ),
            )

            // Try to delete Group that is still bond to Right
            try {
                backend.deleteGroup(receivedGroupId2)
                // An exception should be thrown
                fail()
            } catch (_: ResourceStillInUseException) {
            }

            // Delete Right
            backend.deleteRight(rightId1)
            assertThat(
                backend.dbConnector.groupDB
                    .getGroupsByRightId(rightId1)
                    .toSet(),
                `is`(emptySet()),
            )

            // Delete Group finally
            assertThat(
                backend.deleteGroup(receivedGroupId2),
                `is`(1),
            )
        }

    companion object {
        val TEST_RIGHT = RestConverterTest.TEST_RIGHT
    }
}
