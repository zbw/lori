package de.zbw.business.lori.server.type

/**
 * Helper type that encapsulates the results of a search query.
 *
 * Created on 10-28-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class SearchQueryResult(
    // Number of all search results
    val numberOfResults: Int,
    // Results determined by offset and limit
    val results: List<Item>,
    val accessState: Map<AccessState, Int>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasZbwUserAgreement: Boolean,
    val isPartOfSeries: Map<String, Int>,
    val paketSigels: Map<String, Int>,
    val publicationType: Map<PublicationType, Int>,
    val templateNamesToOcc: Map<String, Pair<String, Int>>,
    val zdbIds: Map<String, Int>,
)
