package de.zbw.api.handle.server.config

import de.gfelbing.konfig.core.source.ChainedKonfiguration
import de.gfelbing.konfig.core.source.EnvironmentKonfiguration
import de.gfelbing.konfig.core.source.PropertiesFileKonfiguration

/**
 * Configurations for the HelloWorld-Service.
 *
 * Created on 05-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object HandleConfigurations {
    private const val SERVICE_NAME = "handle"

    private val configSources = ChainedKonfiguration(
        listOf(
            EnvironmentKonfiguration(),
            PropertiesFileKonfiguration("$SERVICE_NAME.properties")
        )
    )
    val serverConfig by lazy { HandleConfiguration.load(SERVICE_NAME, configSources) }
}
