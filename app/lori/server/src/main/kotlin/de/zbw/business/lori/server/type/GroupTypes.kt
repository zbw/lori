package de.zbw.business.lori.server.type

/**
 * Types related to Groups.
 *
 * Created on 11-10-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
@kotlinx.serialization.Serializable
data class Group(
    val name: String,
    val description: String?,
    val ipAddresses: List<GroupIpAddress>,
)

@kotlinx.serialization.Serializable
data class GroupIpAddress(
    val organisationName: String,
    val ipAddress: String,
)
