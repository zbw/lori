package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.RightError
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.LoriServiceGrpcKt
import de.zbw.lori.api.TemplateApplication
import de.zbw.lori.api.TemplateError
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager

/**
 * Lori GRPC-server.
 *
 * Created on 07-12-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriGrpcServer(
    private val config: LoriConfiguration,
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
                val communityIds = daConnector.getAllCommunityIds(token)
                val imports = runImports(communityIds, token)
                FullImportResponse
                    .newBuilder()
                    .setItemsImported(imports)
                    .build()
            } catch (e: Throwable) {
                span.setStatus(StatusCode.ERROR, e.message ?: e.cause.toString())
                throw StatusRuntimeException(
                    Status.INTERNAL.withCause(e.cause)
                        .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}")
                )
            } finally {
                span.end()
            }
        }
    }

    override suspend fun applyTemplates(request: ApplyTemplatesRequest): ApplyTemplatesResponse {
        val span = tracer
            .spanBuilder("lori.LoriService/ApplyTemplates")
            .setSpanKind(SpanKind.SERVER)
            .startSpan()
        return withContext(span.asContextElement()) {
            try {
                val backendResponse: Map<Int, Pair<List<String>, List<RightError>>> = if (request.all) {
                    daConnector.backend.applyAllTemplates()
                } else {
                    daConnector.backend.applyTemplates(request.templateIdsList)
                }
                val templateApplications: List<TemplateApplication> = backendResponse.entries.map { e ->
                    TemplateApplication
                        .newBuilder()
                        .setTemplateId(e.key)
                        .setNumberAppliedEntries(e.value.first.size)
                        .addAllMetadataIds(e.value.first)
                        .addAllErrors(
                            e.value.second.map {
                                TemplateError.newBuilder()
                                    .setErrorId(it.errorId ?: -1)
                                    .setMessage(it.message)
                                    .setTemplateIdSource(it.templateIdSource ?: -1)
                                    .setRightIdSource(it.rightIdSource ?: "")
                                    .setMetadataId(it.metadataId)
                                    .setHandleId(it.handleId)
                                    .setConflictingRightId(it.conflictingRightId)
                                    .setCreatedOn(it.createdOn?.toInstant()?.toEpochMilli() ?: -1)
                                    .build()
                            }
                        )
                        .build()
                }
                ApplyTemplatesResponse
                    .newBuilder()
                    .addAllTemplateApplications(templateApplications)
                    .build()
            } catch (e: Throwable) {
                span.setStatus(StatusCode.ERROR, e.message ?: e.cause.toString())
                throw StatusRuntimeException(
                    Status.INTERNAL.withCause(e.cause)
                        .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}")
                )
            }
        }
    }

    private suspend fun runImports(communityIds: List<Int>, token: String): Int {
        val semaphore = Semaphore(3)
        val mutex = Mutex()
        var importCount = 0
        coroutineScope {
            launch(Dispatchers.IO) {
                repeat(communityIds.size) {
                    semaphore.acquire()
                    val imports = importCommunity(token, communityIds[it])
                    mutex.withLock {
                        importCount += imports
                    }
                    semaphore.release()
                }
            }
        }
        return importCount
    }

    private suspend fun importCommunity(token: String, communityId: Int): Int {
        LOG.info("Start importing community $communityId")
        val daCommunity: DACommunity = daConnector.getCommunity(token, communityId)
        val import = daConnector.startFullImport(token, daCommunity)
        return import.sum()
    }

    companion object {
        private val LOG = LogManager.getLogger(LoriGrpcServer::class.java)
    }
}
