package de.zbw.business.lori.server.type

import de.zbw.business.lori.server.LoriServerBackendTest.Companion.TEST_METADATA
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.testng.annotations.Test

/**
 * Test [SearchQueryResult].
 *
 * Created on 08-04-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class SearchQueryResultTest {
    @Test
    fun testReduceResults() {
        val item1 = TEST_ITEM.copy(metadata = TEST_METADATA.copy(metadataId = "1"))
        val item2 = TEST_ITEM.copy(metadata = TEST_METADATA.copy(metadataId = "2"))
        val givenSearchResult1 = SearchQueryResult(
            numberOfResults = 20,
            results = listOf(
                item1,
                item2,
            ),
            accessState = mapOf(
                AccessState.OPEN to 5,
                AccessState.CLOSED to 3,
            ),
            invalidSearchKey = emptyList(),
            hasLicenceContract = false,
            hasOpenContentLicence = true,
            hasSearchTokenWithNoKey = false,
            hasZbwUserAgreement = true,
            paketSigels = mapOf(
                "paketSigel1" to 3,
                "paketSigel2" to 10,
            ),
            publicationType = mapOf(
                PublicationType.ARTICLE to 5,
                PublicationType.THESIS to 3,
            ),
            zdbIds = emptyMap(),
        )

        val givenSearchResult2 = SearchQueryResult(
            numberOfResults = 30,
            results = listOf(
                item2,
            ),
            accessState = mapOf(
                AccessState.OPEN to 15,
                AccessState.RESTRICTED to 1,
            ),
            invalidSearchKey = emptyList(),
            hasLicenceContract = false,
            hasOpenContentLicence = false,
            hasSearchTokenWithNoKey = false,
            hasZbwUserAgreement = true,
            paketSigels = mapOf(
                "paketSigel2" to 10,
            ),
            publicationType = mapOf(
                PublicationType.THESIS to 3,
                PublicationType.BOOK to 5,
            ),
            zdbIds = mapOf(
                "zdbId1" to 3,
            ),
        )

        val expectedSearchResult = SearchQueryResult(
            numberOfResults = 50,
            results = listOf(
                item1,
                item2,
                item2,
            ),
            accessState = mapOf(
                AccessState.OPEN to 20,
                AccessState.CLOSED to 3,
                AccessState.RESTRICTED to 1,
            ),
            invalidSearchKey = emptyList(),
            hasLicenceContract = false,
            hasOpenContentLicence = true,
            hasSearchTokenWithNoKey = false,
            hasZbwUserAgreement = true,
            paketSigels = mapOf(
                "paketSigel1" to 3,
                "paketSigel2" to 20,
            ),
            publicationType = mapOf(
                PublicationType.ARTICLE to 5,
                PublicationType.THESIS to 6,
                PublicationType.BOOK to 5,
            ),
            zdbIds = mapOf(
                "zdbId1" to 3,
            ),
        )

        // when
        val receivedSearchResult = SearchQueryResult.reduceResults(listOf(givenSearchResult1, givenSearchResult2))

        // then
        assertThat(
            receivedSearchResult,
            `is`(expectedSearchResult),
        )
    }

    companion object {
        val TEST_ITEM = Item(
            metadata = TEST_METADATA,
            rights = emptyList(),
        )
    }
}
