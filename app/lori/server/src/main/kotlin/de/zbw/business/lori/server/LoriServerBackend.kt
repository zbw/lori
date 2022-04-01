package de.zbw.business.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.persistence.lori.server.DatabaseConnector
import io.opentelemetry.api.trace.Tracer

/**
 * Backend for the Access-Server.
 *
 * Created on 07-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class LoriServerBackend(
    private val dbConnector: DatabaseConnector,
) {
    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DatabaseConnector(
            config,
            tracer,
        ),
    )

    fun insertRightForMetadataIds(
        right: ItemRight,
        metadataIds: List<String>,
    ) {
        val pkRight = dbConnector.insertRight(right)
        metadataIds.forEach {
            dbConnector.insertItem(it, pkRight)
        }
    }

    fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> =
        metadataElems.map { insertMetadataElement(it) }

    fun insertMetadataElement(metadata: ItemMetadata): String =
        dbConnector.insertMetadata(metadata)

    fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.upsertMetadataBatch(metadataElems.map { it })

    fun upsertMetaData(metadata: List<ItemMetadata>): IntArray = dbConnector.upsertMetadataBatch(metadata)

    fun getMetadataElements(metadataIds: List<String>): List<ItemMetadata> = dbConnector.getMetadata(metadataIds)

    fun containsMetadataId(id: String): Boolean = dbConnector.containsMetadata(id)

    fun getRightsByMetadataId(metadataId: String): Item? =
        dbConnector.getMetadata(listOf(metadataId)).takeIf { it.isNotEmpty() }
            ?.first()
            ?.let { meta ->
                val rights = dbConnector.getRightIdsByMetadata(metadataId).let {
                    dbConnector.getRights(it)
                }
                Item(
                    meta,
                    rights,
                )
            }

    fun getItemList(limit: Int, offset: Int): List<Item> {
        return dbConnector.getMetadataRange(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { ids ->
            val metadataToRights = ids.map { id ->
                id to dbConnector.getRightIdsByMetadata(id)
            }
            metadataToRights.map { p ->
                Item(
                    getMetadataElements(listOf(p.first)).first(),
                    dbConnector.getRights(p.second)
                )
            }
        } ?: emptyList()
    }
}
