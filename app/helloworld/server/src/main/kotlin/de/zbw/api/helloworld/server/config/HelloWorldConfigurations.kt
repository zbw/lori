package de.zbw.api.helloworld.server.config

import de.gfelbing.konfig.core.source.ChainedKonfiguration
import de.gfelbing.konfig.core.source.EnvironmentKonfiguration
import de.gfelbing.konfig.core.source.PropertiesFileKonfiguration

/**
 * Configurations for the HelloWorld-Service.
 *
 * Created on 04-27-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object HelloWorldConfigurations {
    private const val SERVICE_NAME = "helloworld"

    private val configSources = ChainedKonfiguration(
        listOf(
            EnvironmentKonfiguration(),
            PropertiesFileKonfiguration("$SERVICE_NAME.properties")
        )
    )
    val serverConfig by lazy { HelloWorldConfiguration.load(SERVICE_NAME, configSources) }
}
