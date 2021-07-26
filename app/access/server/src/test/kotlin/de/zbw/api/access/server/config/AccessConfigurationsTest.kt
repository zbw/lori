package de.zbw.api.access.server.config

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [AccessConfigurations].
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessConfigurationsTest {
    @Test
    fun testConfiguration() {
        val config = AccessConfigurations.serverConfig
        assertThat(config.grpcPort, `is`(9092))
        assertThat(config.httpPort, `is`(8082))
        assertThat(config.sqlUser, `is`("access"))
        assertThat(config.sqlPassword, `is`("1qay2wsx"))
        assertThat(config.sqlUrl, `is`("jdbc:postgresql://127.0.0.1/accessinformation"))
    }
}
