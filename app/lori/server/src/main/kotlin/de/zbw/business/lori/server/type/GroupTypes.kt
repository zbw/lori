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
    val entries: List<GroupEntry>,
)

@kotlinx.serialization.Serializable
data class GroupEntry(
    val organisationName: String,
    val ipAddresses: String,
)
