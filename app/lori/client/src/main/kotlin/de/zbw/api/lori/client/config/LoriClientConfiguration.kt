package de.zbw.api.lori.client.config

/**
 * Config for the AccessService client.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class LoriClientConfiguration(
    val port: Int,
    val address: String,
    val deadlineInMilli: Long,
)
