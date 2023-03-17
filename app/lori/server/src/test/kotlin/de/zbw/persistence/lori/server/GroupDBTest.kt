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
    private val dbConnector = DatabaseConnector(
        connection = dataSource.connection,
        tracer = OpenTelemetry.noop().getTracer("foo"),
    ).groupDB

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
    fun testGetGroupList() {
        val group1 = TEST_GROUP.copy(name = "testGetGroupList")
        dbConnector.insertGroup(group1)
        assertThat(
            dbConnector.getGroupList(50, 0),
            `is`(
                listOf(
                    group1
                )
            )
        )

        val group2 = TEST_GROUP.copy(name = "testGetGroupList2")
        dbConnector.insertGroup(group2)

        assertThat(
            dbConnector.getGroupList(50, 0),
            `is`(
                listOf(
                    group1,
                    group2,
                )
            )
        )
    }

    companion object {
        val TEST_GROUP = Group(
            name = "test group",
            description = "some description",
            entries = listOf(
                GroupEntry(
                    organisationName = "some organisation",
                    ipAddresses = "192.168.0.0",
                ),
            ),
        )
    }
}
