package de.zbw.business.lori.server

import de.zbw.api.lori.server.exception.ResourceStillInUseException
import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.persistence.lori.server.DatabaseConnector
import de.zbw.persistence.lori.server.DatabaseTest
import io.mockk.mockk
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.Assert.fail
import org.testng.annotations.Test

/**
 * Test function related to Group-Right entries.
 *
 * Created on 01-23-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightGroupTest : DatabaseTest() {
    private val backend = LoriServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ),
        mockk(),
    )

    @Test
    fun rightGroupRoundtrip() {
        // Given group
        val groupName1 = "group1"
        val group1 =
            Group(
                name = groupName1,
                description = null,
                entries = listOf(
                    GroupEntry(
                        organisationName = "orga1",
                        ipAddresses = "192.168.1.*",
                    )
                )
            )

        // Insert group
        val groupId1 = backend.insertGroup(group1)
        assertThat(
            groupId1,
            `is`(groupName1),
        )

        // Insert Right using the group
        val initialRight: ItemRight = TEST_RIGHT.copy(
            groupIds = listOf(groupId1),
        )

        val rightId1 = backend.insertRight(initialRight)

        // Verify insertions
        assertThat(
            backend.getGroupById(groupId1),
            `is`(group1),
        )

        assertThat(
            backend.dbConnector.groupDB.getGroupsByRightId(rightId1),
            `is`(
                listOf(groupName1),
            )
        )

        // Update Right with another group
        val groupName2 = "group2"
        val group2 =
            Group(
                name = groupName2,
                description = null,
                entries = listOf(
                    GroupEntry(
                        organisationName = "orga2",
                        ipAddresses = "192.168.1.*",
                    )
                )
            )

        val groupId2 = backend.insertGroup(group2)
        val rightUpdated = TEST_RIGHT.copy(
            rightId = rightId1,
            groupIds = listOf(groupId1, groupId2)
        )
        backend.upsertRight(rightUpdated)

        // Verify group update was successful
        assertThat(
            backend.dbConnector.groupDB.getGroupsByRightId(rightId1).toSet(),
            `is`(
                setOf(groupId1, groupId2),
            )
        )

        // Another update, this time a group is removed
        val rightUpdated2 = TEST_RIGHT.copy(
            rightId = rightId1,
            groupIds = listOf(groupId2)
        )
        backend.upsertRight(rightUpdated2)

        // Verify update again
        assertThat(
            backend.dbConnector.groupDB.getGroupsByRightId(rightId1).toSet(),
            `is`(
                setOf(groupId2),
            )
        )

        // Try to delete Group that is still bond to Right
        try {
            backend.deleteGroup(groupId2)
            // An exception should be thrown
            fail()
        } catch (_: ResourceStillInUseException) {
        }

        // Delete Right
        backend.deleteRight(rightId1)
        assertThat(
            backend.dbConnector.groupDB.getGroupsByRightId(rightId1).toSet(),
            `is`(emptySet()),
        )

        // Delete Group finally
        assertThat(
            backend.deleteGroup(groupId2),
            `is`(1),
        )
    }

    companion object {
        val TEST_RIGHT = RestConverterTest.TEST_RIGHT
    }
}
