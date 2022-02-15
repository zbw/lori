package de.zbw.api.lori.server.connector

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.api.lori.server.type.DACommunity
import de.zbw.api.lori.server.type.DACredentials
import de.zbw.api.lori.server.type.DAItem
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

/**
 * Connector for the Digital Archive (DA).
 *
 * Created on 02-10-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DAConnector(
    val config: LoriConfiguration,
    engine: HttpClientEngine = CIO.create(),
    private val client: HttpClient = HttpClient(engine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(
                Json {
                    prettyPrint = true
                    isLenient = true
                }
            )
        }
    }
) {
    private val restURL = "${config.digitalArchiveAddress}/rest"

    suspend fun login(): String {
        val statement: HttpStatement =
            client.post("$restURL/login") {
                contentType(ContentType.Application.Json)
                body = DACredentials(
                    email = config.digitalArchiveUsername,
                    password = config.digitalArchivePassword,
                )
            }
        val r = statement.execute()
        return r.receive()
    }

    suspend fun getCommunity(loginToken: String): DACommunity {
        val statement: HttpStatement =
            client.get("$restURL/communities/${config.digitalArchiveCommunity}") {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
                headers {
                    append(DSPACE_TOKEN, loginToken)
                }
                parameter("expand", "all")
            }
        val r = statement.execute()
        return r.receive()
    }

    suspend fun startFullImport(loginToken: String, collectionIds: List<Int>): List<DAItem> =
        coroutineScope {
            collectionIds.map { cId ->
                importCollection(loginToken, cId)
            }.flatten()
        }

    suspend fun importCollection(loginToken: String, cId: Int): List<DAItem> {
        val statement: HttpStatement =
            client.get("$restURL/collections/$cId/items") {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                }
                headers {
                    append(DSPACE_TOKEN, loginToken)
                }
                parameter("expand", "all")
                parameter("offset", "0")
                parameter("limit", "100")
            }
        val r = statement.execute()
        return r.receive() as List<DAItem>
    }

    companion object {
        const val DSPACE_TOKEN = "rest-dspace-token"
    }
}
