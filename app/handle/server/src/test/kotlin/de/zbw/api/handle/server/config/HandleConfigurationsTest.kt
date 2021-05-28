package de.zbw.api.handle.server.config

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [HandleConfigurations].
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleConfigurationsTest {
    @Test
    fun testConfiguration() {
        val config = HandleConfigurations.serverConfig
        assertThat(config.grpcPort, `is`(9092))
        assertThat(config.httpPort, `is`(8082))
        assertThat(config.password, `is`("password"))
        assertThat(config.handlePrefix, `is`("5678"))
    }
}
