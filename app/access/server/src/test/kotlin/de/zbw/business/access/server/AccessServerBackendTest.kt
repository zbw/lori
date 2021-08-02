package de.zbw.business.access.server

import de.zbw.persistence.access.server.DatabaseConnector
import de.zbw.persistence.access.server.DatabaseTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import kotlin.test.assertTrue

/**
 * Test [AccessServerBackend].
 *
 * Created on 07-22-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessServerBackendTest : DatabaseTest() {

    private val backend = AccessServerBackend(
        DatabaseConnector(
            connection = dataSource.connection,
        )
    )

    private val orderedList = arrayOf(
        TEST_ACCESS_RIGHT.copy(header = TEST_HEADER.copy(id = "aaaa")),
        TEST_ACCESS_RIGHT.copy(header = TEST_HEADER.copy(id = "aaaa2")),
    )

    private val entries = arrayOf(
        TEST_ACCESS_RIGHT,
        TEST_ACCESS_RIGHT.copy(header = TEST_HEADER.copy(id = "test_2a")),
        TEST_ACCESS_RIGHT.copy(header = TEST_HEADER.copy(id = "test_2b")),
    ).plus(orderedList)

    @BeforeClass
    fun fillDatabase() {
        backend.insertAccessRightEntries(entries.toList())
    }

    @DataProvider(name = DATA_FOR_ROUNDTRIP)
    fun createDataForRoundtrip() =
        arrayOf(
            arrayOf(
                listOf(entries[0].header.id),
                setOf(entries[0]),
            ),
            arrayOf(
                listOf(entries[1].header.id, entries[2].header.id),
                setOf(entries[1], entries[2]),
            ),
            arrayOf(
                listOf(entries[0].header.id, "invalidId"),
                setOf(entries[0]),
            ),
            arrayOf(
                listOf("invalidId"),
                setOf<AccessRight>(),
            ),
        )

    @Test(dataProvider = DATA_FOR_ROUNDTRIP)
    fun testRoundtrip(
        queryIds: List<String>,
        expectedAccessRights: Set<AccessRight>
    ) {
        // when
        val received = backend.getAccessRightEntries(queryIds)
        // then
        assertThat(received.toSet(), `is`(expectedAccessRights))
    }

    @Test
    fun testGetList() {
        // when
        val received = backend.getAccessRightList(limit = 2, offset = 0)
        // then
        assertThat(received, `is`(orderedList.toList()))

        // no limit
        val noLimit = backend.getAccessRightList(limit = 0, offset = 0)
        assertThat(noLimit, `is`(emptyList()))

        assertTrue(backend.containsAccessRightId(orderedList.first().header.id))
    }

    companion object {
        const val DATA_FOR_ROUNDTRIP = "DATA_FOR_ROUNDTRIP"

        val TEST_HEADER = Header(
            id = "test",
            tenant = "www.zbw.eu",
            usageGuide = "usageGuie",
            template = "CC",
            mention = true,
            shareAlike = true,
            commercialUse = true,
            copyright = false,
        )

        private val TEST_RESTRICTION = Restriction(
            type = RestrictionType.DATE,
            attribute = Attribute(
                type = AttributeType.FROM_DATE,
                values = listOf("2022-01-01"),
            )
        )

        private val TEST_ACTION = Action(
            type = ActionType.READ,
            permission = true,
            restrictions = listOf(TEST_RESTRICTION),
        )

        val TEST_ACCESS_RIGHT = AccessRight(
            header = TEST_HEADER,
            actions = listOf(TEST_ACTION),
        )
    }
}
