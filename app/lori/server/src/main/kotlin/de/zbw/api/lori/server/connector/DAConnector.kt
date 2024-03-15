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
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.coroutineScope
import org.apache.logging.log4j.LogManager
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
    private val client: HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            gson {}
        }
    }
) {
    private val restURL = "${config.digitalArchiveAddress}/rest"

    suspend fun login(): String {
        val statement: HttpResponse = client.request("$restURL/login") {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
            }
            setBody(
                DACredentials(
                    email = config.digitalArchiveUsername,
                    password = config.digitalArchivePassword,
                )
            )
        }
        return statement.bodyAsText()
    }

    suspend fun getAllCommunityIds(loginToken: String): List<Int> {
        val response = client.request("$restURL/communities") {
            method = HttpMethod.Get
            headers {
                append(HttpHeaders.Accept, "text/json")
                append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
            }
            headers {
                append(DSPACE_TOKEN, loginToken)
            }
            parameter("expand", "all")
        }.body<List<DACommunity>>()
        return response.map { it.id }
    }

    suspend fun getCommunity(loginToken: String, community: Int): DACommunity {
        val response = client.request("$restURL/communities/$community") {
            method = HttpMethod.Get
            headers {
                append(HttpHeaders.Accept, "text/json")
                append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
            }
            headers {
                append(DSPACE_TOKEN, loginToken)
            }
            parameter("expand", "all")
        }.body<DACommunity>()
        return response
    }

    suspend fun startFullImport(loginToken: String, community: DACommunity): List<Int> =
        coroutineScope {
            val collectionIds = community.collections?.map { it.id } ?: emptyList()
            collectionIds.map { cId ->
                val daItemList: List<DAItem> =
                    importCollection(loginToken, cId).map { item -> item.copy(parentCommunityList = listOf(community)) }
                val metadataList: List<ItemMetadata> =
                    daItemList
                        .mapNotNull { it.toBusiness() }
                        .map { shortenHandle(it) }
                backend.upsertMetaData(metadataList).filter { it == 1 }.size
            }
        }

    suspend fun importCollection(loginToken: String, cId: Int): List<DAItem> {
        val numberItems: Int = client.get("$restURL/collections/$cId") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
            }
            headers {
                append(DSPACE_TOKEN, loginToken)
            }
        }.body<DACollection>().numberItems ?: 0
        LOG.info("CollectionId $cId: Number of Items: $numberItems")

        return (1..ceil(numberItems.toDouble() / 100).toInt()).map {
            LOG.info("CollectionId $cId: Offset ${(it - 1) * 100}")

            client.get("$restURL/collections/$cId/items") {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
                }
                headers {
                    append(DSPACE_TOKEN, loginToken)
                }
                parameter("expand", "all")
                parameter("offset", "${(it - 1) * 100}")
                parameter("limit", "100")
            }.body<List<DAItem>>()
        }.flatten()
    }

    companion object {
        const val DSPACE_TOKEN = "rest-dspace-token"
        private const val HANDLE_URL = "http://hdl.handle.net/"
        private val LOG = LogManager.getLogger(DAConnector::class.java)

        internal fun shortenHandle(item: ItemMetadata) = item.copy(
            handle = item.handle.substringAfter(HANDLE_URL)
        )
    }
}
