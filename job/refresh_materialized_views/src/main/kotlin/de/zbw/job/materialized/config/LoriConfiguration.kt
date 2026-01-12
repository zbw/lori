package de.zbw.job.templateapply.config

import de.gfelbing.konfig.core.definition.KonfigDeclaration.int
import de.gfelbing.konfig.core.definition.KonfigDeclaration.long
import de.gfelbing.konfig.core.definition.KonfigDeclaration.required
import de.gfelbing.konfig.core.definition.KonfigDeclaration.string
import de.gfelbing.konfig.core.source.KonfigurationSource

/**
 * Configurations for the Microservice.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class LoriConfiguration(
    val loriAddress: String,
    val loriGrpcPort: Int,
    val loriClientDeadline: Long,
) {
    companion object {
        fun load(
            prefix: String,
            source: KonfigurationSource,
        ): LoriConfiguration {
            val loriAddress = string(prefix, "lori", "address").required()
            val loriGrpcPort = int(prefix, "lori", "grpc", "port").required()
            val loriClientDeadline = long(prefix, "lori", "client", "deadline").required()

            return LoriConfiguration(
                loriAddress = source[loriAddress],
                loriGrpcPort = source[loriGrpcPort],
                loriClientDeadline = source[loriClientDeadline],
            )
        }
    }
}
