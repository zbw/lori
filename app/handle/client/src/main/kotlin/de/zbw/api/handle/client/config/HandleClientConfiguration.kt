package de.zbw.api.handle.client.config

/**
 * Config for the HandleService client.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class HandleClientConfiguration(
    val port: Int,
    val address: String,
    val deadlineInMilli: Long,
)
