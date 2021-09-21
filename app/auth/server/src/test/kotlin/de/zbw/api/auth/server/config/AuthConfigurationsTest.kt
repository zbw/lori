package de.zbw.api.auth.server.config

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [AuthConfigurations].
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AuthConfigurationsTest {
    @Test
    fun testConfiguration() {
        val config = AuthConfigurations.serverConfig
        assertThat(config.grpcPort, `is`(9092))
        assertThat(config.httpPort, `is`(8082))
    }
}
