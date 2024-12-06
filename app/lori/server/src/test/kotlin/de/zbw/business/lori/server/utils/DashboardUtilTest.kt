package de.zbw.business.lori.server.utils

import de.zbw.api.lori.server.type.RestConverterTest
import de.zbw.business.lori.server.type.ConflictType
import de.zbw.business.lori.server.type.Item
import de.zbw.business.lori.server.type.RightError
import io.mockk.every
import io.mockk.mockkStatic
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DashboardUtilTest {
    @BeforeMethod
    fun beforeTest() {
        mockkStatic(OffsetDateTime::class)
        every { OffsetDateTime.now(ZoneOffset.UTC) } returns NOW
    }

    @DataProvider(name = DATA_FOR_GAP_ERRORS)
    fun createDataForGapErrors() =
        arrayOf(
            arrayOf(
                Item(
                    metadata = TEST_METADATA,
                    rights = emptyList(),
                ),
                emptyList<RightError>(),
                "No errors because no right information exist",
            ),
            arrayOf(
                Item(
                    metadata = TEST_METADATA,
                    rights = listOf(RIGHT_DEC),
                ),
                emptyList<RightError>(),
                "No errors, one Right information with open end",
            ),
            arrayOf(
                Item(
                    metadata = TEST_METADATA,
                    rights = listOf(RIGHT_SEP, RIGHT_DEC),
                ),
                listOf(
                    RightError(
                        conflictByRightId = null,
                        conflictByContext = "sigel",
                        conflictType = ConflictType.GAP,
                        createdOn = NOW,
                        message = "Dem Handle hdl:example.handle.net fehlt eine Rechteinformation zwischen 2021-09-30 und 2021-12-01.",
                        handle = "hdl:example.handle.net",
                        errorId = null,
                        conflictingWithRightId = null,
                        testId = null,
                        createdBy = "user1",
                    ),
                ),
                "No errors, one Right information with open end",
            ),
            arrayOf(
                Item(
                    metadata = TEST_METADATA,
                    rights = listOf(RIGHT_SEP, RIGHT_NOV),
                ),
                listOf(
                    RightError(
                        conflictByRightId = null,
                        conflictByContext = "sigel",
                        conflictType = ConflictType.GAP,
                        createdOn = NOW,
                        message = "Dem Handle hdl:example.handle.net fehlt eine Rechteinformation zwischen 2021-09-30 und 2021-11-01.",
                        handle = "hdl:example.handle.net",
                        errorId = null,
                        conflictingWithRightId = null,
                        testId = null,
                        createdBy = "user1",
                    ),
                    RightError(
                        conflictByRightId = null,
                        conflictByContext = "sigel",
                        conflictType = ConflictType.GAP,
                        createdOn = NOW,
                        message = "Handle hdl:example.handle.net hat nur Rechteinformationen bis zum 2021-11-30.",
                        handle = "hdl:example.handle.net",
                        errorId = null,
                        conflictingWithRightId = null,
                        testId = null,
                        createdBy = "user1",
                    ),
                ),
                "Both, gap and no open end",
            ),
            arrayOf(
                Item(
                    metadata = TEST_METADATA,
                    rights = listOf(RIGHT_NOV, RIGHT_SEP),
                ),
                listOf(
                    RightError(
                        conflictByRightId = null,
                        conflictByContext = "sigel",
                        conflictType = ConflictType.GAP,
                        createdOn = NOW,
                        message = "Dem Handle hdl:example.handle.net fehlt eine Rechteinformation zwischen 2021-09-30 und 2021-11-01.",
                        handle = "hdl:example.handle.net",
                        errorId = null,
                        conflictingWithRightId = null,
                        testId = null,
                        createdBy = "user1",
                    ),
                    RightError(
                        conflictByRightId = null,
                        conflictByContext = "sigel",
                        conflictType = ConflictType.GAP,
                        createdOn = NOW,
                        message = "Handle hdl:example.handle.net hat nur Rechteinformationen bis zum 2021-11-30.",
                        handle = "hdl:example.handle.net",
                        errorId = null,
                        conflictingWithRightId = null,
                        testId = null,
                        createdBy = "user1",
                    ),
                ),
                "Both, gap and no open end but with reverse order (to check if sort works internally as expected)",
            ),
        )

    @Test(dataProvider = DATA_FOR_GAP_ERRORS)
    fun testCheckForGapErrors(
        item: Item,
        expected: List<RightError>,
        reason: String,
    ) {
        assertThat(
            reason,
            DashboardUtil.checkForGapErrors(item, "user1"),
            `is`(expected),
        )
    }

    companion object {
        const val DATA_FOR_GAP_ERRORS = "DATA_FOR_GAP_ERRORS "
        val TEST_METADATA = RestConverterTest.TEST_METADATA
        val NOW: OffsetDateTime =
            OffsetDateTime.of(
                2022,
                3,
                1,
                1,
                1,
                0,
                0,
                ZoneOffset.UTC,
            )!!

        val RIGHT_SEP =
            RestConverterTest.TEST_RIGHT.copy(
                rightId = "SEP",
                startDate = LocalDate.of(2021, 9, 1),
                endDate = LocalDate.of(2021, 9, 30),
            )
        val RIGHT_OCT =
            RestConverterTest.TEST_RIGHT.copy(
                rightId = "OCT",
                startDate = LocalDate.of(2021, 10, 1),
                endDate = LocalDate.of(2021, 10, 31),
            )

        val RIGHT_NOV =
            RestConverterTest.TEST_RIGHT.copy(
                rightId = "NOV",
                startDate = LocalDate.of(2021, 11, 1),
                endDate = LocalDate.of(2021, 11, 30),
            )

        val RIGHT_DEC =
            RestConverterTest.TEST_RIGHT.copy(
                rightId = "DEC",
                startDate = LocalDate.of(2021, 12, 1),
                endDate = null,
            )
    }
}
