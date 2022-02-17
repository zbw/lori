package de.zbw.job.loriiimport

import de.zbw.api.lori.client.LoriClient
import de.zbw.api.lori.client.config.LoriClientConfiguration
import de.zbw.lori.api.FullImportRequest
import de.zbw.lori.api.FullImportResponse
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

/**
 * Trigger a full import.
 *
 * Created on 02-03-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val loriClient = LoriClient(
            configuration = LoriClientConfiguration(9092, "lori", 5000)
        )

        val response: FullImportResponse = runBlocking {
            loriClient.fullImport(FullImportRequest.getDefaultInstance())
        }
        LOG.info("Lori Server imported: ${response.itemsImported}")
    }

    private val LOG = LoggerFactory.getLogger(Main::class.java)
}
