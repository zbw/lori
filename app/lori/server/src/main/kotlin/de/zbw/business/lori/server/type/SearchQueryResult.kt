package de.zbw.business.lori.server.type

/**
 * Helper type that encapsulates the results of a search query.
 *
 * Created on 10-28-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class SearchQueryResult(
    val numberOfResults: Int,
    val results: List<Item>,
    val paketSigels: Set<String>,
    val zdbIds: Set<String>,
)
