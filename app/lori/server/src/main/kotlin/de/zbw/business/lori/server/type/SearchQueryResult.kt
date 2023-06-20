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
    val accessState: Map<AccessState, Int>,
    val invalidSearchKey: List<String>,
    val hasLicenceContract: Boolean,
    val hasOpenContentLicence: Boolean,
    val hasSearchTokenWithNoKey: Boolean,
    val hasZbwUserAgreement: Boolean,
    val paketSigels: Map<String, Int>,
    val publicationType: Map<PublicationType, Int>,
    val zdbIds: Map<String, Int>,
) {
    companion object {
        fun reduceResults(results: List<SearchQueryResult>): SearchQueryResult {
            val uniqueResults: Set<Item> = results.flatMap { it.results }.toSet()
            val paketSigel: Map<String, Int> =
                uniqueResults
                    .filter { it.metadata.paketSigel != null }
                    .groupBy { it.metadata.paketSigel!! }
                    .mapValues { it.value.size }

            val publicationTypes: Map<PublicationType, Int> =
                uniqueResults.groupBy { it.metadata.publicationType }
                    .mapValues { it.value.size }

            val zdbIds: Map<String, Int> =
                uniqueResults
                    .filter { it.metadata.paketSigel != null }
                    .groupBy { it.metadata.zdbId!! }
                    .mapValues { it.value.size }

            val accessState = results.foldRight(false){elem, acc ->
                acc
            }

            return SearchQueryResult(
                numberOfResults = uniqueResults.size,
                results = uniqueResults.toList(),
                accessState = emptyMap(), // TODO
                invalidSearchKey = emptyList(), // TODO
                hasLicenceContract = false, // TODO
                hasOpenContentLicence = false, // TODO
                hasSearchTokenWithNoKey = false, // TODO
                hasZbwUserAgreement = false, // TODO
                paketSigels = paketSigel,
                publicationType = publicationTypes,
                zdbIds = zdbIds,
            )

        }
    }
}
