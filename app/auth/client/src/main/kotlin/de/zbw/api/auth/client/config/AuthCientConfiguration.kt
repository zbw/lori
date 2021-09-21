package de.zbw.api.auth.client.config

/**
 * Config for the Auth client.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class AuthClientConfiguration(
    val port: Int,
    val address: String,
    val deadlineInMilli: Long,
)
