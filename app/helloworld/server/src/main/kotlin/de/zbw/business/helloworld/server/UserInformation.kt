package de.zbw.business.helloworld.server

import kotlinx.serialization.Serializable

/**
 * Business object for user information.
 *
 * Created on 05-05-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
@Serializable
data class UserInformation(
    val name: String,
    val uuid: Long,
)
