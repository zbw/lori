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
    val zdbIds: Map<String, Int>,
) {
    companion object {
        fun reduceResults(results: List<SearchQueryResult>): SearchQueryResult {
            val paketSigel: Map<String, Int> =
                results.foldRight(emptyMap()) { elem, acc ->
                    val mutMap: MutableMap<String, Int> = acc.toMutableMap()
                    elem.paketSigels.forEach { it: Map.Entry<String, Int> ->
                        mutMap.merge(it.key, it.value) { oldV, newV ->
                            oldV + newV
                        }
                    }
                    mutMap
                }

            val publicationTypes: Map<PublicationType, Int> =
                results.foldRight(emptyMap()) { elem, acc ->
                    val mutMap: MutableMap<PublicationType, Int> = acc.toMutableMap()
                    elem.publicationType.forEach { it: Map.Entry<PublicationType, Int> ->
                        mutMap.merge(it.key, it.value) { oldV, newV ->
                            oldV + newV
                        }
                    }
                    mutMap
                }

            val zdbIds: Map<String, Int> =
                results.foldRight(emptyMap()) { elem, acc ->
                    val mutMap: MutableMap<String, Int> = acc.toMutableMap()
                    elem.zdbIds.forEach { it: Map.Entry<String, Int> ->
                        mutMap.merge(it.key, it.value) { oldV, newV ->
                            oldV + newV
                        }
                    }
                    mutMap
                }

            val accessState = results.foldRight(emptyMap<AccessState, Int>()) { elem, acc ->
                val mutMap: MutableMap<AccessState, Int> = acc.toMutableMap()
                elem.accessState.forEach { it: Map.Entry<AccessState, Int> ->
                    mutMap.merge(it.key, it.value) { oldV, newV ->
                        oldV + newV
                    }
                }
                mutMap
            }

            val hasLicenceContract = results.foldRight(false) { elem, acc ->
                elem.hasLicenceContract || acc
            }

            val hasOpenContentLicence = results.foldRight(false) { elem, acc ->
                elem.hasOpenContentLicence || acc
            }

            val hasSearchTokenWithNoKey = results.foldRight(false) { elem, acc ->
                elem.hasSearchTokenWithNoKey || acc
            }

            val hasZbwUserAgreement = results.foldRight(false) { elem, acc ->
                elem.hasZbwUserAgreement || acc
            }

            return SearchQueryResult(
                numberOfResults = results.sumOf { it.numberOfResults },
                results = results.flatMap { it.results },
                accessState = accessState,
                invalidSearchKey = emptyList(),
                hasLicenceContract = hasLicenceContract,
                hasOpenContentLicence = hasOpenContentLicence,
                hasSearchTokenWithNoKey = hasSearchTokenWithNoKey,
                hasZbwUserAgreement = hasZbwUserAgreement,
                paketSigels = paketSigel,
                publicationType = publicationTypes,
                zdbIds = zdbIds,
            )
        }
    }
}
