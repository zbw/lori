package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.TemplateApplicationResult
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
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
        val span =
            tracer
                .spanBuilder("lori.LoriService/FullImport")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
        return withContext(span.asContextElement()) {
            try {
                val token = daConnector.login()
                val communityIds = daConnector.getAllCommunityIds(token)
                LOG.info("Community Ids to import: ${communityIds.sortedDescending().reversed()}")
                val imports = runImports(communityIds, token)
                FullImportResponse
                    .newBuilder()
                    .setItemsImported(imports)
                    .build()
            } catch (e: Throwable) {
                span.setStatus(StatusCode.ERROR, e.message ?: e.cause.toString())
                throw StatusRuntimeException(
                    Status.INTERNAL
                        .withCause(e.cause)
                        .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}"),
                )
            } finally {
                span.end()
            }
        }
    }

    override suspend fun applyTemplates(request: ApplyTemplatesRequest): ApplyTemplatesResponse {
        val span =
            tracer
                .spanBuilder("lori.LoriService/ApplyTemplates")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
        return withContext(span.asContextElement()) {
            try {
                val backendResponse: List<TemplateApplicationResult> =
                    if (request.all) {
                        daConnector.backend.applyAllTemplates()
                    } else {
                        daConnector.backend.applyTemplates(request.rightIdsList)
                    }
                val templateApplications: List<TemplateApplication> =
                    backendResponse.map { e: TemplateApplicationResult ->
                        TemplateApplication
                            .newBuilder()
                            .setRightId(e.rightId)
                            .setTemplateName(e.templateName)
                            .setNumberAppliedEntries(e.appliedMetadataIds.size)
                            .addAllMetadataIds(e.appliedMetadataIds)
                            .addAllErrors(
                                e.errors.map {
                                    TemplateError
                                        .newBuilder()
                                        .setErrorId(it.errorId ?: -1)
                                        .setMessage(it.message)
                                        .setRightIdSource(it.conflictingWithRightId ?: "")
                                        .setMetadataId(it.metadataId)
                                        .setHandleId(it.handleId)
                                        .setConflictingRightId(it.conflictByRightId)
                                        .setCreatedOn(it.createdOn?.toInstant()?.toEpochMilli() ?: -1)
                                        .build()
                                },
                            ).addAllExceptions(
                                e.exceptionTemplateApplicationResult.map { exc ->
                                    TemplateApplication
                                        .newBuilder()
                                        .setRightId(exc.rightId)
                                        .setTemplateName(exc.templateName)
                                        .setNumberAppliedEntries(exc.appliedMetadataIds.size)
                                        .addAllMetadataIds(exc.appliedMetadataIds)
                                        .addAllErrors(
                                            exc.errors.map {
                                                TemplateError
                                                    .newBuilder()
                                                    .setErrorId(it.errorId ?: -1)
                                                    .setMessage(it.message)
                                                    .setRightIdSource(it.conflictingWithRightId ?: "")
                                                    .setMetadataId(it.metadataId)
                                                    .setHandleId(it.handleId)
                                                    .setConflictingRightId(it.conflictByRightId)
                                                    .setCreatedOn(it.createdOn?.toInstant()?.toEpochMilli() ?: -1)
                                                    .build()
                                            },
                                        ).build()
                                },
                            ).build()
                    }
                ApplyTemplatesResponse
                    .newBuilder()
                    .addAllTemplateApplications(templateApplications)
                    .build()
            } catch (e: Throwable) {
                span.setStatus(StatusCode.ERROR, e.message ?: e.cause.toString())
                throw StatusRuntimeException(
                    Status.INTERNAL
                        .withCause(e.cause)
                        .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}"),
                )
            }
        }
    }

    private suspend fun runImports(
        communityIds: List<Int>,
        token: String,
    ): Int {
        val semaphore = Semaphore(3)
        val numberImportsDeferred: List<Deferred<Int>> =
            coroutineScope {
                communityIds.map {
                    val import = async { importCommunity(token, it, semaphore) }
                    import
                }
            }
        return numberImportsDeferred.awaitAll().sum()
    }

    private suspend fun importCommunity(
        token: String,
        communityId: Int,
        semaphore: Semaphore,
    ): Int {
        semaphore.acquire()
        LOG.info("Start importing community $communityId")
        val daCommunity: DACommunity = daConnector.getCommunity(token, communityId)
        val import = daConnector.startFullImport(token, daCommunity)
        semaphore.release()
        LOG.info("Finished importing community $communityId")
        return import.sum()
    }

    companion object {
        private val LOG = LogManager.getLogger(LoriGrpcServer::class.java)
    }
}
