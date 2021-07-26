package de.zbw.api.access.client.config

/**
 * Config for the AccessService client.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class AccessClientConfiguration(
    val port: Int,
    val address: String,
    val deadlineInMilli: Long,
)
