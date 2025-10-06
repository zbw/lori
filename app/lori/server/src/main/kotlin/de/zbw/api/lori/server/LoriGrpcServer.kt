package de.zbw.api.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.connector.DAConnector
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.mail.MailService
import de.zbw.business.lori.server.type.GenericJob
import de.zbw.business.lori.server.type.JobKind
import de.zbw.business.lori.server.type.JobStatus
import de.zbw.business.lori.server.type.TemplateApplicationResult
import de.zbw.lori.api.ApplyTemplatesRequest
import de.zbw.lori.api.ApplyTemplatesResponse
import de.zbw.lori.api.CheckForRightErrorsRequest
import de.zbw.lori.api.CheckForRightErrorsResponse
import de.zbw.lori.api.CleanDownloadsRequest
import de.zbw.lori.api.CleanDownloadsResponse
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import de.zbw.lori.api.LoriServiceGrpcKt
import de.zbw.lori.api.SendMailRequest
import de.zbw.lori.api.SendMailResponse
import de.zbw.lori.api.TemplateApplication
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.opentelemetry.api.trace.Span
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
import java.time.Instant

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
    private val mailService: MailService,
) : LoriServiceGrpcKt.LoriServiceCoroutineImplBase() {
    override suspend fun checkForRightErrors(request: CheckForRightErrorsRequest): CheckForRightErrorsResponse {
        val span =
            tracer
                .spanBuilder("lori.LoriService/CheckForRightErrors")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
        val job =
            GenericJob(
                createdBy = GPRC_USER,
                kind = JobKind.CHECK_RIGHT_ERRORS,
                status = JobStatus.RUNNING,
            )
        return withContext(span.asContextElement()) {
            try {
                backend.insertGenericJob(job)
                val errorsCount =
                    daConnector.backend.checkForRightErrors(GPRC_USER)

                job.status = JobStatus.SUCCESSFUL
                job.summary = "Number of errors found: $errorsCount"
                backend.updateGenericJobById(genericJob = job)
                CheckForRightErrorsResponse
                    .newBuilder()
                    .setNumberOfErrors(errorsCount)
                    .build()
            } catch (e: Throwable) {
                throw handleError(e, span, job)
            } finally {
                span.end()
            }
        }
    }

    override suspend fun fullImport(request: FullImportRequest): FullImportResponse {
        val span =
            tracer
                .spanBuilder("lori.LoriService/FullImport")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
        val job =
            GenericJob(
                createdBy = GPRC_USER,
                kind = JobKind.FULL_IMPORT,
                status = JobStatus.RUNNING,
            )
        return withContext(span.asContextElement()) {
            try {
                backend.insertGenericJob(job)
                val startTime = Instant.now()
                val token = daConnector.login()
                LOG.info("Login-Token: $token")
                val communityIds = daConnector.getAllCommunityIds(token)
                LOG.info("Community Ids to import: ${communityIds.sortedDescending().reversed()}")
                val imports: Int = runImports(communityIds, token)
                val deleted: Int = backend.updateMetadataAsDeleted(startTime)
                LOG.info("Number of imported Items: $imports")
                LOG.info("Number of deleted Items found: $deleted")

                job.status = JobStatus.SUCCESSFUL
                job.summary = "Number of imported Items: $imports; Number of deleted Items found: $deleted"
                backend.updateGenericJobById(genericJob = job)

                FullImportResponse
                    .newBuilder()
                    .setItemsImported(imports)
                    .setItemsDeleted(deleted)
                    .build()
            } catch (e: Throwable) {
                throw handleError(e, span, job)
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
        val job =
            GenericJob(
                createdBy = GPRC_USER,
                kind = JobKind.TEMPLATE_APPLY,
                status = JobStatus.RUNNING,
            )
        return withContext(span.asContextElement()) {
            try {
                backend.insertGenericJob(job)
                val backendResponse: List<TemplateApplicationResult> =
                    if (request.all) {
                        daConnector.backend.applyAllTemplates(
                            request.skipDraft,
                            request.dryRun,
                            GPRC_USER,
                        )
                    } else {
                        daConnector.backend.applyTemplates(
                            request.rightIdsList,
                            request.skipDraft,
                            request.dryRun,
                            GPRC_USER,
                        )
                    }
                val templateApplications: List<TemplateApplication> =
                    backendResponse.map { e: TemplateApplicationResult ->
                        TemplateApplication
                            .newBuilder()
                            .setRightId(e.rightId)
                            .setTemplateName(e.templateName)
                            .setNumberAppliedEntries(e.appliedMetadataHandles.size)
                            .setNumberOfErrors(
                                e.errors.size,
                            ).setException(
                                e.exceptionTemplateApplicationResult?.let { exc ->
                                    TemplateApplication
                                        .newBuilder()
                                        .setRightId(exc.rightId)
                                        .setTemplateName(exc.templateName)
                                        .setNumberAppliedEntries(exc.appliedMetadataHandles.size)
                                        .setNumberOfErrors(
                                            exc.errors.size,
                                        ).build()
                                } ?: TemplateApplication.newBuilder().build(),
                            ).build()
                    }
                job.status = JobStatus.SUCCESSFUL
                job.summary = "Number of Templates applied: ${templateApplications.size}." +
                    " Number of errors ${templateApplications.foldRight(0){r, acc ->
                        r.numberOfErrors + acc
                    }}"
                backend.updateGenericJobById(genericJob = job)

                ApplyTemplatesResponse
                    .newBuilder()
                    .addAllTemplateApplications(templateApplications)
                    .build()
            } catch (e: Throwable) {
                throw handleError(e, span, job)
            } finally {
                span.end()
            }
        }
    }

    override suspend fun cleanDownloads(request: CleanDownloadsRequest): CleanDownloadsResponse {
        val span: Span =
            tracer
                .spanBuilder("lori.LoriService/CleanDownloads")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()
        val job =
            GenericJob(
                createdBy = GPRC_USER,
                kind = JobKind.CLEAN_DOWNLOADS,
                status = JobStatus.RUNNING,
            )

        return withContext(span.asContextElement()) {
            try {
                backend.insertGenericJob(job)
                val instant: Instant =
                    Instant.ofEpochSecond(
                        request.olderThan.seconds,
                        request.olderThan.nanos.toLong(),
                    )
                val deletions = backend.cleanDownloads(instant)
                job.status = JobStatus.SUCCESSFUL
                job.summary = "Number of deleted files: $deletions"
                backend.updateGenericJobById(genericJob = job)
                CleanDownloadsResponse
                    .newBuilder()
                    .setDeletedCount(deletions)
                    .build()
            } catch (e: Throwable) {
                throw handleError(e, span, job)
            } finally {
                span.end()
            }
        }
    }

    override suspend fun sendMail(request: SendMailRequest): SendMailResponse {
        val span: Span =
            tracer
                .spanBuilder("lori.LoriService/SendMail")
                .setSpanKind(SpanKind.SERVER)
                .startSpan()

        return withContext(span.asContextElement()) {
            try {
                mailService.sendMail(
                    to = request.receiver,
                    subject = request.subject,
                    body = request.text,
                )

                SendMailResponse
                    .newBuilder()
                    .setStatus(SUCCESS_MSG)
                    .build()
            } catch (e: Throwable) {
                span.recordException(e)
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

    private suspend fun handleError(
        e: Throwable,
        span: Span,
        job: GenericJob,
    ): StatusRuntimeException {
        span.recordException(e)
        span.setStatus(StatusCode.ERROR, e.message ?: e.cause.toString())
        job.status = JobStatus.FAILED
        job.errorMessage = e.message ?: e.cause.toString()
        backend.updateGenericJobById(genericJob = job)
        mailService.sendMail(
            to = config.mailTo,
            subject = "Lori-Job Error: Fehlerhafter Lauf (${config.stage})",
            body =
                "Es ist ein Fehler aufgetreten beim Job '${job.kind}'!\n" +
                    "Bitte kontaktieren Sie den Systembesitzer.\n" +
                    "Folgender Fehler ist aufgetreten: ${e.message ?: e.cause.toString()}",
        )

        throw StatusRuntimeException(
            Status.INTERNAL
                .withCause(e.cause)
                .withDescription("Following error occurred: ${e.message}\nStacktrace: ${e.stackTraceToString()}"),
        )
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
        val daCommunity: DACommunity = daConnector.getCommunityById(token, communityId) ?: return 0
        val import = daConnector.importAllCollectionsOfCommunity(token, daCommunity)
        semaphore.release()
        LOG.info("Finished importing community $communityId")
        return import.sum()
    }

    companion object {
        private val LOG = LogManager.getLogger(LoriGrpcServer::class.java)
        private const val GPRC_USER = "GRPC_INTERFACE"
        internal const val SUCCESS_MSG = "Successfully sent mail"
    }
}
