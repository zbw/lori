package de.zbw.business.lori.server.type

/**
 * Helper type that encapsulates the results of a search query.
 *
 * Created on 10-28-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class SearchQueryResult(
    val numberOfResults: Int, // Number of all search results
    val results: List<Item>, // Results determined by offset and limit
    val accessState: Map<AccessState, Int>,
    val invalidSearchKey: List<String>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasSearchTokenWithNoKey: Boolean,
    val hasZbwUserAgreement: Boolean,
    val paketSigels: Map<String, Int>,
    val publicationType: Map<PublicationType, Int>,
    val templateIds: Map<Int, Pair<String, Int>>,
    val zdbIds: Map<String, Int>,
)
