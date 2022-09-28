package de.zbw.api.lori.server.route

import de.zbw.business.lori.server.PublicationDateFilter
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import kotlin.test.assertNull

/**
 * Testing [QueryParameterParser].
 *
 * Created on 09-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class QueryParameterParserTest {

    @DataProvider(name = DATA_FOR_PARSE_PUBLICATION_DATE)
    fun createDataForParsePublicationDate() =
        arrayOf(
            arrayOf(
                "2000-2022",
                PublicationDateFilter(2000, 2022),
            ),
            arrayOf(
                "2000-",
                PublicationDateFilter(2000, PublicationDateFilter.MAX_YEAR),
            ),
            arrayOf(
                "-2000",
                PublicationDateFilter(PublicationDateFilter.MIN_YEAR, 2000),
            ),
            arrayOf(
                "foobar",
                null,
            ),
        )

    @Test(dataProvider = DATA_FOR_PARSE_PUBLICATION_DATE)
    fun testParsePublicationDateFilter(
        input: String,
        expectedFilter: PublicationDateFilter?,
    ) {
        // when
        val received = QueryParameterParser.parsePublicationDateFilter(input)

        if (expectedFilter == null) {
            assertNull(received)
        } else {
            assertThat(
                expectedFilter.fromYear,
                `is`(received!!.fromYear)
            )
            assertThat(
                expectedFilter.toYear,
                `is`(received.toYear)
            )
        }
    }

    companion object {
        const val DATA_FOR_PARSE_PUBLICATION_DATE = "DATA_FOR_PARSE_PUBLICATION_DATE"
    }
}
