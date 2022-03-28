package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.LoriServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * Access GRPC-server.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriGrpcServer(
    config: LoriConfiguration,
    private val backend: LoriServerBackend,
    private val daConnector: DAConnector = DAConnector(config, backend),
    private val tracer: Tracer,
) : LoriServiceGrpcKt.LoriServiceCoroutineImplBase() {

    override suspend fun fullImport(request: FullImportRequest): FullImportResponse {
        val span = tracer
            .spanBuilder("lori.LoriService/FullImport")
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
        return withContext(span.asContextElement()) {
            try {
                val token = daConnector.login()
                val community: DACommunity = daConnector.getCommunity(token)
                val imports = daConnector.startFullImport(token, community.collections.map { it.id })
                FullImportResponse
                    .newBuilder()
                    .setItemsImported(imports.sum())
                    .build()
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR, e.message ?: e.cause.toString())
                throw StatusRuntimeException(
                    Status.INTERNAL.withCause(e.cause)
                        .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}")
                )
            }
        }
    }
}
