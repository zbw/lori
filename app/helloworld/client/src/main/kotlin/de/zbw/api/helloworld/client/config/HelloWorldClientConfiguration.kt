package de.zbw.api.helloworld.client.config

/**
 * Config for the HelloWorld client.
 *
 * Created on 04-28-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class HelloWorldClientConfiguration(
    val port: Int,
    val address: String,
    val deadlineInMilli: Long,
)
