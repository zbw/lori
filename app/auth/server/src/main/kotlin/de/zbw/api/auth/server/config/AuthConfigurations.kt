package de.zbw.api.auth.server.config

import de.gfelbing.konfig.core.source.ChainedKonfiguration
import de.gfelbing.konfig.core.source.EnvironmentKonfiguration
import de.gfelbing.konfig.core.source.PropertiesFileKonfiguration

/**
 * Configurations for the Auth-Service.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object AuthConfigurations {
    private const val SERVICE_NAME = "auth"

    private val configSources = ChainedKonfiguration(
        listOf(
            EnvironmentKonfiguration(),
            PropertiesFileKonfiguration("$SERVICE_NAME.properties")
        )
    )
    val serverConfig by lazy { AuthConfiguration.load(SERVICE_NAME, configSources) }
}
