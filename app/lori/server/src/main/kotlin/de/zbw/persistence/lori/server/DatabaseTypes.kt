package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.PublicationType

/**
 * Types that represent results of db queries.
 *
 * Created on 10-27-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class PaketSigelZDBIdPubType(
    val accessState: AccessState?,
    val paketSigel: String?,
    val publicationType: PublicationType,
    val zdbId: String?,
)

data class PaketSigelZDBIdPubTypeSet(
    val accessState: Set<AccessState>,
    val paketSigels: Set<String>,
    val publicationType: Set<PublicationType>,
    val zdbIds: Set<String>,
)
