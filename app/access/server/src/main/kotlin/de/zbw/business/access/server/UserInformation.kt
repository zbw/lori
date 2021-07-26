package de.zbw.business.access.server

import kotlinx.serialization.Serializable

/**
 * Business object for user information.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
@Serializable
data class UserInformation(
    val name: String,
    val uuid: Long,
)
