package de.zbw.api.access.server.config

import de.gfelbing.konfig.core.source.ChainedKonfiguration
import de.gfelbing.konfig.core.source.EnvironmentKonfiguration
import de.gfelbing.konfig.core.source.PropertiesFileKonfiguration

/**
 * Configurations for the HelloWorld-Service.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object AccessConfigurations {
    private const val SERVICE_NAME = "access"

    private val configSources = ChainedKonfiguration(
        listOf(
            EnvironmentKonfiguration(),
            PropertiesFileKonfiguration("$SERVICE_NAME.properties")
        )
    )
    val serverConfig by lazy { AccessConfiguration.load(SERVICE_NAME, configSources) }
}
