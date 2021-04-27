package de.zbw.api.helloworld.server.config

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [HelloWorldConfigurations].
 *
 * Created on 04-27-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HelloWorldConfigurationsTest {
    @Test
    fun testConfiguration() {
        val config = HelloWorldConfigurations.serverConfig
        assertThat(config.grpcPort, `is`(9092))
        assertThat(config.httpPort, `is`(8082))
    }
}
