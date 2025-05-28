package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.AccessStateOnDateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.FormalRule
import de.zbw.business.lori.server.type.PublicationType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.time.LocalDate
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Testing [QueryParameterParser].
 *
 * Created on 09-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class QueryParameterParserTest {
    @DataProvider(name = DATA_FOR_PARSE_PUBLICATION_YEAR)
    fun createDataForParsePublicationYear() =
        arrayOf(
            arrayOf(
                "2000-2022",
                PublicationYearFilter(2000, 2022),
                "both years",
            ),
            arrayOf(
                "2000-",
                PublicationYearFilter(2000, null),
                "only fromYear",
            ),
            arrayOf(
                "-2000",
                PublicationYearFilter(null, 2000),
                "only toYear",
            ),
            arrayOf(
                "foobar",
                null,
                "invalid term",
            ),
            arrayOf(
                null,
                null,
                "null",
            ),
            arrayOf(
                "",
                null,
                "empty term",
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_PUBLICATION_YEAR)
    fun testParsePublicationYearFilter(
        input: String?,
        expectedFilter: PublicationYearFilter?,
        reason: String,
    ) {
        // when
        val received = QueryParameterParser.parsePublicationYearFilter(input)

        // then
        if (expectedFilter == null) {
            assertNull(received)
        } else {
            assertThat(
                reason,
                received!!.fromYear,
                `is`(
                    expectedFilter.fromYear,
                ),
            )
            assertThat(
                reason,
                received.toYear,
                `is`(
                    expectedFilter.toYear,
                ),
            )
        }
    }

    @DataProvider(name = DATA_FOR_PARSE_PUBLICATION_TYPE)
    fun createDataForParsePublicationType() =
        arrayOf(
            arrayOf(
                "WORKING_PAPER,ARTICLE",
                PublicationTypeFilter(
                    listOf(
                        PublicationType.WORKING_PAPER,
                        PublicationType.ARTICLE,
                    ),
                ),
            ),
            arrayOf(
                "WORKING_PAPER,FOOBAR",
                PublicationTypeFilter(
                    listOf(
                        PublicationType.WORKING_PAPER,
                    ),
                ),
            ),
            arrayOf(
                "FOOBAR",
                null,
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_PUBLICATION_TYPE)
    fun testParsePublicationTypeFilter(
        input: String,
        expectedFilter: PublicationTypeFilter?,
    ) {
        // when
        val received: PublicationTypeFilter? = QueryParameterParser.parsePublicationTypeFilter(input)

        // then
        if (expectedFilter == null) {
            assertNull(received)
        } else {
            assertThat(
                received!!.publicationTypes.toSet(),
                `is`(
                    expectedFilter.publicationTypes.toSet(),
                ),
            )
        }
    }

    @DataProvider(name = DATA_FOR_PARSE_ACCESS_STATE)
    fun createDataForParseAccessState() =
        arrayOf(
            arrayOf(
                "CLOSED,OPEN,RESTRICTED",
                AccessStateFilter(
                    listOf(
                        AccessState.CLOSED,
                        AccessState.OPEN,
                        AccessState.RESTRICTED,
                    ),
                ),
            ),
            arrayOf(
                "OPEN,FOOBAR",
                AccessStateFilter(
                    listOf(
                        AccessState.OPEN,
                    ),
                ),
            ),
            arrayOf(
                "FOOBAR",
                null,
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_ACCESS_STATE)
    fun testParseAccessStateFilter(
        input: String,
        expectedFilter: AccessStateFilter?,
    ) {
        // when
        val received: AccessStateFilter? = QueryParameterParser.parseAccessStateFilter(input)

        // then
        if (expectedFilter == null) {
            assertNull(received)
        } else {
            assertThat(
                received!!.accessStates.toSet(),
                `is`(
                    expectedFilter.accessStates.toSet(),
                ),
            )
        }
    }

    @Test
    fun testParseDateFilter() {
        // given
        val expectedStartFilter =
            StartDateFilter(
                LocalDate.of(2000, 10, 1),
            )
        val expectedEndFilter =
            EndDateFilter(
                LocalDate.of(2000, 12, 1),
            )

        // when + then
        assertThat(
            QueryParameterParser.parseStartDateFilter("2000-10-01")!!.date,
            `is`(expectedStartFilter.date),
        )

        // when + then
        assertThat(
            QueryParameterParser.parseEndDateFilter("2000-12-01")!!.date,
            `is`(expectedEndFilter.date),
        )

        // when + then
        assertNull(
            QueryParameterParser.parseStartDateFilter("2000101Fobbar!"),
        )

        assertNull(
            QueryParameterParser.parseEndDateFilter("2000101Fobbar!"),
        )
    }

    @Test
    fun testFormalRuleFilter() {
        // given
        val expectedRuleLicence = listOf(FormalRule.LICENCE_CONTRACT)
        // when + then
        assertThat(
            QueryParameterParser.parseFormalRuleFilter("licence_contract")!!.formalRules,
            `is`(expectedRuleLicence),
        )

        // given
        val expectedFormalRules = listOf(FormalRule.LICENCE_CONTRACT, FormalRule.CC_LICENCE_NO_RESTRICTION)
        // when + then
        assertThat(
            QueryParameterParser.parseFormalRuleFilter("licence_contract,cc_licence_no_restriction,open_content_licence")!!.formalRules,
            `is`(expectedFormalRules),
        )

        // when + then
        assertNull(
            QueryParameterParser.parseFormalRuleFilter("2000101Fobbar!"),
        )
    }

    @Test
    fun testParseNoRightInformationFilter() {
        assertNull(QueryParameterParser.parseNoRightInformationFilter("fooo"))
        assertNull(QueryParameterParser.parseNoRightInformationFilter("false"))
        assertNull(QueryParameterParser.parseNoRightInformationFilter(""))
        assertNotNull(QueryParameterParser.parseNoRightInformationFilter("tRue"))
        assertNotNull(QueryParameterParser.parseNoRightInformationFilter("true"))
        assertNotNull(QueryParameterParser.parseNoRightInformationFilter("TRUE"))
    }

    @Test
    fun testParseManualRightFilter() {
        assertNull(QueryParameterParser.parseManualRightFilter("fooo"))
        assertNull(QueryParameterParser.parseManualRightFilter("false"))
        assertNull(QueryParameterParser.parseManualRightFilter(""))
        assertNotNull(QueryParameterParser.parseManualRightFilter("tRue"))
        assertNotNull(QueryParameterParser.parseManualRightFilter("true"))
        assertNotNull(QueryParameterParser.parseManualRightFilter("TRUE"))
    }

    @Test
    fun testParseAccessStateOnDate() {
        // given
        val expectedFilter =
            AccessStateOnDateFilter(
                date = LocalDate.of(2000, 10, 1),
                accessState = AccessState.OPEN,
            )

        // when + then
        val receivedFilter: AccessStateOnDateFilter? = QueryParameterParser.parseAccessStateOnDate("open+2000-10-01")
        assertThat(
            receivedFilter!!.date,
            `is`(expectedFilter.date),
        )

        assertThat(
            receivedFilter.accessState,
            `is`(expectedFilter.accessState),
        )

        // when + then
        assertNull(
            QueryParameterParser.parseAccessStateOnDate("OPEN2000-10-01"),
            "Invalid string",
        )

        val expectedFilterNoAccessState =
            AccessStateOnDateFilter(
                date = LocalDate.of(2000, 10, 1),
                accessState = null,
            )

        // when + then
        val receivedFilterNoAccessState: AccessStateOnDateFilter? = QueryParameterParser.parseAccessStateOnDate("2000-10-01")
        assertThat(
            receivedFilterNoAccessState!!.date,
            `is`(expectedFilterNoAccessState.date),
        )

        assertNull(
            receivedFilterNoAccessState.accessState,
        )
    }

    companion object {
        const val DATA_FOR_PARSE_ACCESS_STATE = "DATA_FOR_PARSE_ACCESS_STATE"
        const val DATA_FOR_PARSE_PUBLICATION_YEAR = "DATA_FOR_PARSE_PUBLICATION_YEAR"
        const val DATA_FOR_PARSE_PUBLICATION_TYPE = "DATA_FOR_PARSE_PUBLICATION_TYPE"
    }
}
