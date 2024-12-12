package de.zbw.business.lori.server.type

import java.time.OffsetDateTime

/**
 * Types related to Groups.
 * Groups represent IP lists that are checked whenever a request is received. Only if
 * the IP list contains the IP address from the request, the request will be successful.
 *
 * Created on 11-10-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class Group(
    val groupId: Int,
    val description: String?,
    val entries: List<GroupEntry>,
    val title: String,
    val createdBy: String?,
    val createdOn: OffsetDateTime?,
    val lastUpdatedBy: String?,
    val lastUpdatedOn: OffsetDateTime?,
)

data class GroupEntry(
    val organisationName: String,
    val ipAddresses: String,
)
