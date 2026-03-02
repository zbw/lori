package de.zbw.job.templateapply.config

import de.gfelbing.konfig.core.source.ChainedKonfiguration
import de.gfelbing.konfig.core.source.EnvironmentKonfiguration
import de.gfelbing.konfig.core.source.PropertiesFileKonfiguration

/**
 * Configurations for the Lori-Service.
 *
 * Created on 10-06-2025.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object LoriConfigurations {
    private const val SERVICE_NAME = "materialized"

    private val configSources =
        ChainedKonfiguration(
            listOf(
                EnvironmentKonfiguration(),
                PropertiesFileKonfiguration("$SERVICE_NAME.properties"),
            ),
        )
    val serverConfig by lazy { LoriConfiguration.load(SERVICE_NAME, configSources) }
}
