package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.PublicationType

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class PaketSigelZDBIdPubType(
    val paketSigel: String?,
    val zdbId: String?,
    val publicationType: PublicationType,
)

data class PaketSigelZDBIdPubTypeSet(
    val paketSigels: Set<String>,
    val zdbIds: Set<String>,
    val publicationType: Set<PublicationType>,
)
