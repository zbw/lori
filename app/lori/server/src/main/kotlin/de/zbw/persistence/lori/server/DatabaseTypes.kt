package de.zbw.persistence.lori.server

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class PaketSigelAndZDBId(
    val paketSigel: String?,
    val zdbId: String?,
)

data class PaketSigelAndZDBIdSet(
    val paketSigels: Set<String>,
    val zdbIds: Set<String>,
)
