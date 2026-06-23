package de.zbw.api.lori.server.connector

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.DACollection
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.DACredentials
import de.zbw.api.lori.server.type.DAItem
import de.zbw.api.lori.server.type.MetadataValidationError
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.api.lori.server.utils.Constants
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.utils.TimezoneUtil.utcOffsetDateTimeToBerlinDate
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.gson.gson
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds

/**
 * Connector for the Digital Archive (DA).
 *
 * Created on 02-10-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DAConnector(
    val config: LoriConfiguration,
    val backend: LoriServerBackend,
    engine: HttpClientEngine = CIO.create(),
    private val client: HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                gson {}
            }
            install(Logging) {
                logger = HttpLogger()
                level = LogLevel.ALL
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
            }
        },
) {
    private val restURL = "${config.digitalArchiveAddress}/rest"
    private val mutexForLogging = Mutex()

    suspend fun login(): String {
        val statement: HttpResponse =
            client.request("$restURL/login") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                    append(HttpHeaders.Accept, "text/plain")
                }
                setBody(
                    DACredentials(
                        email = config.digitalArchiveUsername,
                        password = config.digitalArchivePassword,
                    ),
                )
            }
        return statement.bodyAsText()
    }

    suspend fun getAllCommunityIds(loginToken: String): List<Int> {
        val response =
            client
                .request("$restURL/communities") {
                    method = HttpMethod.Get
                    headers {
                        append(HttpHeaders.Accept, "text/json")
                        append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                    }
                    headers {
                        append(DSPACE_TOKEN, loginToken)
                    }
                    parameter("limit", "1000")
                }.body<List<DACommunity>>()
        return response.map { it.id }
    }

    suspend fun getCommunityById(
        loginToken: String,
        communityId: Int,
    ): DACommunity? {
        val response: ApiResponse<DACommunity, String> =
            client.safeRequest(2, 2000L) {
                method = HttpMethod.Get
                url("$restURL/communities/$communityId")
                headers {
                    append(HttpHeaders.Accept, "text/json")
                    append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                }
                headers {
                    append(DSPACE_TOKEN, loginToken)
                }
                parameter("expand", "collections,subCommunities")
            }
        val generalErrorMsg = "Following error occurred on importing community $communityId"
        when (response) {
            is ApiResponse.Error.HttpError<*> -> {
                LOG.warn(
                    "$generalErrorMsg: HttpError: Status Code" + response.code + "; Error Body" +
                        response.errorBody,
                )
                return null
            }

            is ApiResponse.Error.NetworkError -> {
                LOG.warn("$generalErrorMsg: Network Error: ${response.message}")
                return null
            }

            is ApiResponse.Error.SerializationError -> {
                LOG.warn("$generalErrorMsg: Serialization Error: ${response.message}")
                return null
            }

            is ApiResponse.Success<DACommunity> -> {
                return response.body
            }
        }
    }

    suspend fun importAllCollectionsOfCommunity(
        loginToken: String,
        community: DACommunity,
        validationErrorMap: MutableMap<MetadataValidationError, List<String>>,
    ): List<CollectionImport> =
        coroutineScope {
            val collectionIds = community.collections?.map { it.id } ?: emptyList()
            collectionIds.mapNotNull { cId ->
                importCollection(
                    loginToken,
                    cId,
                    community,
                    validationErrorMap = validationErrorMap,
                )
            }
        }

    suspend fun importCollectionPart(
        loginToken: String,
        collectionId: Int,
        offset: Int,
        limit: Int,
        collection: DACollection,
        community: DACommunity,
        validationErrorMap: MutableMap<MetadataValidationError, List<String>>,
    ): Int {
        LOG.debug("Collection Handle ${collection.handle}: Offset $offset")

        val response: ApiResponse<List<DAItem>, String> =
            client.safeRequest(2, 2000L) {
                method = HttpMethod.Get
                url("$restURL/collections/$collectionId/items")
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                }
                headers {
                    append(DSPACE_TOKEN, loginToken)
                }
                parameter("expand", "metadata")
                parameter("offset", offset.toString())
                parameter("limit", limit.toString())
            }
        return when (response) {
            is ApiResponse.Error.HttpError<*> -> {
                LOG.warn("HttpError: Status Code" + response.code + "; Error Body" + response.errorBody)
                0
            }

            is ApiResponse.Error.NetworkError -> {
                LOG.warn("Network Error: ${response.message}")
                0
            }

            is ApiResponse.Error.SerializationError -> {
                LOG.warn("Serialization Error: ${response.message}")
                0
            }

            is ApiResponse.Success<List<DAItem>> -> {
                val daItemList = response.body
                val metadataList: List<ItemMetadata> =
                    daItemList
                        .mapNotNull {
                            it.toBusiness(
                                daCollection = collection,
                                daCommunity = community,
                                validationErrorMap = validationErrorMap,
                                mutexForLogging = mutexForLogging,
                            )
                        }.map { shortenHandle(it) }
                val newHandlesGettingDefaultEntries = checkForDefaultEntries(metadataList)
                val writtenToDB = backend.upsertMetadata(metadataList).filter { it == 1 }.size
                createDefaultRightEntries(newHandlesGettingDefaultEntries)
                writtenToDB
            }
        }
    }

    suspend fun getCollectionById(
        loginToken: String,
        collectionId: Int,
    ): DACollection? {
        val response: ApiResponse<DACollection, String> =
            client.safeRequest(2, 2000L) {
                method = HttpMethod.Get
                url("$restURL/collections/$collectionId")
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                    append(DSPACE_TOKEN, loginToken)
                }
            }
        val generalErrorMsg = "Following error occurred on importing collection $collectionId"
        when (response) {
            is ApiResponse.Error.HttpError<*> -> {
                LOG.warn(
                    "$generalErrorMsg: HttpError: Status Code" + response.code + "; Error Body" +
                        response.errorBody,
                )
                return null
            }

            is ApiResponse.Error.NetworkError -> {
                LOG.warn("$generalErrorMsg: Network Error: ${response.message}")
                return null
            }

            is ApiResponse.Error.SerializationError -> {
                LOG.warn("$generalErrorMsg: Serialization Error: ${response.message}")
                return null
            }

            is ApiResponse.Success<DACollection> -> {
                return response.body
            }
        }
    }

    suspend fun importCollection(
        loginToken: String,
        collectionId: Int,
        community: DACommunity,
        validationErrorMap: MutableMap<MetadataValidationError, List<String>>,
    ): CollectionImport? =
        coroutineScope {
            val collection: DACollection? =
                getCollectionById(
                    loginToken = loginToken,
                    collectionId = collectionId,
                )

            if (collection == null) {
                return@coroutineScope null
            }

            val numberItems: Int = collection.numberItems ?: 0
            LOG.info("Collection Handle ${collection.handle}: Start importing $numberItems items")
            val deferredResults = mutableListOf<Deferred<Int>>()
            for (offsetCounter in 0..<ceil(numberItems.toDouble() / DEFAULT_IMPORT_CHUNK_SIZE).toInt()) {
                deferredResults +=
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            importCollectionPart(
                                loginToken = loginToken,
                                collectionId = collectionId,
                                offset = offsetCounter * DEFAULT_IMPORT_CHUNK_SIZE,
                                limit = DEFAULT_IMPORT_CHUNK_SIZE,
                                collection = collection,
                                community = community,
                                validationErrorMap = validationErrorMap,
                            )
                        }
                    }
            }
            // Sum results in the end to prevent race conditions
            return@coroutineScope deferredResults
                .awaitAll()
                .sum()
                .let { successfullyImported ->
                    CollectionImport(
                        importsExpected = collection.numberItems ?: 0,
                        importsReceived = successfullyImported,
                        collectionId = collectionId,
                    )
                }.also {
                    LOG.info("Collection Handle ${collection.handle}: Successfully imported ${it.importsReceived} entries")
                    if (it.importsReceived < it.importsExpected) {
                        LOG.warn(
                            "Collection Handle ${collection.handle}:" +
                                " Not all items were imported. Only ${it.importsReceived} out of ${it.importsExpected} were imported.",
                        )
                    }
                }
        }

    suspend fun checkForDefaultEntries(metadata: List<ItemMetadata>): List<String> {
        val candidateHandles =
            metadata
                .filter { it.collectionHandle == COLLECTION_WITH_DEFAULT_ENTRIES }
                .takeIf { it.isNotEmpty() }
                ?.map { it.handle }
                ?: return emptyList()
        val existingHandles = backend.getExistingMetadataHandles(candidateHandles)
        return candidateHandles.filter { it !in existingHandles }
    }

    /**
     * Special case for all new imports of collection 11159/17. Those get a
     * default entry when imported for the first time.
     */
    suspend fun createDefaultRightEntries(newHandles: List<String>) {
        val localDate: LocalDate = utcOffsetDateTimeToBerlinDate(OffsetDateTime.now())
        try {
            newHandles.forEach { handle ->
                LOG.info("Create default right entry for new handle $handle")
                val generatedRightId =
                    backend.insertRight(
                        ItemRight(
                            groupIds = listOf(config.groupIdZBWTerminal),
                            accessState = AccessState.RESTRICTED,
                            startDate = localDate,
                            createdBy = Constants.AUTHOR_AUTOMATIC,
                            lastUpdatedBy = Constants.AUTHOR_AUTOMATIC,
                            basisStorage = BasisStorage.AUTHOR_RIGHT_EXCEPTION,
                            basisAccessState = BasisAccessState.AUTHOR_RIGHT_EXCEPTION,
                            isTemplate = false,
                        ),
                    )
                backend.insertItemEntry(
                    createdBy = Constants.AUTHOR_AUTOMATIC,
                    handle = handle,
                    rightId = generatedRightId,
                )
            }
            return
        } catch (e: Exception) {
            LOG.error("Error while creating default right entries for handles $newHandles", e)
        }
    }

    suspend inline fun <reified T, reified E> HttpClient.safeRequest(
        retries: Int,
        delayMillis: Long,
        block: HttpRequestBuilder.() -> Unit,
    ): ApiResponse<T, E> {
        var currentAttempt = 0
        while (currentAttempt < retries) {
            try {
                val response = request { block() }
                return ApiResponse.Success(response.body())
            } catch (e: ClientRequestException) {
                return ApiResponse.Error.HttpError(e.response.status.value, e.errorBody())
            } catch (e: ServerResponseException) {
                return ApiResponse.Error.HttpError(e.response.status.value, e.errorBody())
            } catch (e: IOException) {
                if (currentAttempt < retries - 1) {
                    delay(delayMillis.milliseconds)
                    currentAttempt++
                    LOG.warn("IOException: Retrying request ${currentAttempt + 1} time")
                } else {
                    return ApiResponse.Error.NetworkError(e.message ?: "No message")
                }
            } catch (e: NoTransformationFoundException) {
                if (currentAttempt < retries - 1) {
                    delay(delayMillis.milliseconds)
                    currentAttempt++
                    LOG.warn("NoTransformationFoundException: Retrying request ${currentAttempt + 1} time")
                } else {
                    return ApiResponse.Error.NetworkError(e.message ?: "No message")
                }
            } catch (e: SerializationException) {
                return ApiResponse.Error.SerializationError(e.message ?: "No message")
            } catch (e: Exception) {
                LOG.error("Unexpected request error", e)
                throw e
            }
        }
        throw IllegalStateException("Unexpected error") // should never happen
    }

    suspend inline fun <reified E> ResponseException.errorBody(): E? =
        try {
            response.body()
        } catch (_: SerializationException) {
            null
        }

    companion object {
        const val DSPACE_TOKEN = "rest-dspace-token"
        const val DEFAULT_IMPORT_CHUNK_SIZE = 100

        // New entries in this collection will receive default right entries
        const val COLLECTION_WITH_DEFAULT_ENTRIES = "11159/17"
        private const val HANDLE_URL = "http://hdl.handle.net/"
        val LOG: Logger = LogManager.getLogger(DAConnector::class.java)

        private const val MAX_PARALLEL_CONNECTIONS = 5
        private val semaphore = Semaphore(MAX_PARALLEL_CONNECTIONS)

        internal fun shortenHandle(item: ItemMetadata) =
            item.copy(
                handle = item.handle.substringAfter(HANDLE_URL),
            )
    }
}
