package de.zbw.api.lori.server.connector

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.DACollection
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.DACredentials
import de.zbw.api.lori.server.type.DAItem
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.type.ItemMetadata
import io.ktor.client.HttpClient
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
import io.ktor.client.request.get
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerializationException
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.math.ceil

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

    suspend fun getCommunity(
        loginToken: String,
        community: Int,
    ): DACommunity {
        val response =
            client
                .request("$restURL/communities/$community") {
                    method = HttpMethod.Get
                    headers {
                        append(HttpHeaders.Accept, "text/json")
                        append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                    }
                    headers {
                        append(DSPACE_TOKEN, loginToken)
                    }
                    parameter("expand", "collections")
                }.body<DACommunity>()
        return response
    }

    suspend fun startFullImport(
        loginToken: String,
        community: DACommunity,
    ): List<Int> =
        coroutineScope {
            val collectionIds = community.collections?.map { it.id } ?: emptyList()
            collectionIds.map { cId ->
                importCollection(loginToken, cId, community.id)
            }
        }

    suspend fun importCollection(
        loginToken: String,
        collectionId: Int,
        communityId: Int,
    ): Int {
        val numberItems: Int =
            client
                .get("$restURL/collections/$collectionId") {
                    headers {
                        append(HttpHeaders.Accept, "application/json")
                        append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                        append(DSPACE_TOKEN, loginToken)
                    }
                }.body<DACollection>()
                .numberItems ?: 0
        LOG.info("CollectionId $collectionId: Number of Items: $numberItems")

        return (1..ceil(numberItems.toDouble() / 100).toInt()).sumOf {
            semaphore.withPermit {
                LOG.info("CollectionId $collectionId: Offset ${(it - 1) * 100}")

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
                        parameter("expand", "metadata,parentCommunityList,parentCollection")
                        parameter("offset", "${(it - 1) * 100}")
                        parameter("limit", "100")
                    }
                when (response) {
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
                        val metadataList =
                            daItemList
                                .mapNotNull { it.toBusiness(communityId) }
                                .map { shortenHandle(it) }
                        backend.upsertMetadata(metadataList).filter { it == 1 }.size
                    }
                }
            }
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
                    delay(delayMillis)
                    currentAttempt++
                } else {
                    return ApiResponse.Error.NetworkError(e.message ?: "No message")
                }
            } catch (e: SerializationException) {
                return ApiResponse.Error.SerializationError(e.message ?: "No message")
            }
        }
        throw IllegalStateException("Unexpected error") // should never happen
    }

    suspend inline fun <reified E> ResponseException.errorBody(): E? =
        try {
            response.body()
        } catch (e: SerializationException) {
            null
        }

    companion object {
        const val DSPACE_TOKEN = "rest-dspace-token"
        private const val HANDLE_URL = "http://hdl.handle.net/"
        internal val LOG: Logger = LogManager.getLogger(DAConnector::class.java)

        private val MAX_PARALLEL_CONNECTIONS = 5
        private val semaphore = Semaphore(MAX_PARALLEL_CONNECTIONS)

        internal fun shortenHandle(item: ItemMetadata) =
            item.copy(
                handle = item.handle.substringAfter(HANDLE_URL),
            )
    }
}
