package de.zbw.api.lori.server.connector

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.DACredentials
import de.zbw.api.lori.server.type.DAItem
import de.zbw.api.lori.server.type.toBusiness
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.LoriServerBackend
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

    suspend fun getCommunity(loginToken: String): DACommunity {
        val response = client.request("$restURL/communities/${config.digitalArchiveCommunity}") {
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

    suspend fun startFullImport(loginToken: String, collectionIds: List<Int>): List<Int> =
        coroutineScope {
            collectionIds.map { cId ->
                val daItemList: List<DAItem> = importCollection(loginToken, cId)
                val metadataList: List<ItemMetadata?> = daItemList.map { it.toBusiness() }
                backend.upsertMetaData(metadataList.filterNotNull()).filter { it == 1 }.size
            }
        }

    suspend fun importCollection(loginToken: String, cId: Int): List<DAItem> {
        val response: List<DAItem> = client.get("$restURL/collections/$cId/items") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Basic ${config.digitalArchiveBasicAuth}")
            }
            headers {
                append(DSPACE_TOKEN, loginToken)
            }
            parameter("expand", "all")
            parameter("offset", "0")
            parameter("limit", "100")
        }.body<List<DAItem>>()
        return response
    }

    companion object {
        const val DSPACE_TOKEN = "rest-dspace-token"
    }
}
