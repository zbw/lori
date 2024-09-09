package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupEntry
import io.opentelemetry.api.OpenTelemetry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test
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
            connection = dataSource.connection,
            tracer = OpenTelemetry.noop().getTracer("foo"),
        ).groupDB

    @Test
    fun testGroupRoundtrip() {
        // Create

        // when + then
        val receivedGroupId = dbConnector.insertGroup(TEST_GROUP)
        val expectedGroup = TEST_GROUP.copy(groupId = receivedGroupId)

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
        val updated =
            expectedGroup.copy(
                description = "baz",
            )
        assertThat(
            dbConnector.updateGroup(updated),
            `is`(1),
        )

        // when + then
        assertThat(
            dbConnector.getGroupById(expectedGroup.groupId),
            `is`(
                updated,
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
    }

    @Test
    fun testGetGroupList() {
        val receivedGroupId1 = dbConnector.insertGroup(TEST_GROUP)
        val expectedGroup1 = TEST_GROUP.copy(groupId = receivedGroupId1)
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
        val expectedGroup2 = secondGroup.copy(groupId = receivedGroupId2)

        assertThat(
            dbConnector.getGroupList(50, 0),
            `is`(
                listOf(
                    expectedGroup1,
                    expectedGroup2,
                ),
            ),
        )
    }

    companion object {
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
            )
    }
}
