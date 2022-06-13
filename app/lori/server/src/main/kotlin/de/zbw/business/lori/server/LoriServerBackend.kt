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

    fun insertItemEntry(metadataId: String, rightId: String) = dbConnector.insertItem(metadataId, rightId)

    fun insertMetadataElements(metadataElems: List<ItemMetadata>): List<String> =
        metadataElems.map { insertMetadataElement(it) }

    fun insertMetadataElement(metadata: ItemMetadata): String =
        dbConnector.insertMetadata(metadata)

    fun insertRight(right: ItemRight): String = dbConnector.insertRight(right)

    fun upsertRight(right: ItemRight): Int = dbConnector.upsertRight(right)

    fun upsertMetadataElements(metadataElems: List<ItemMetadata>): IntArray =
        dbConnector.upsertMetadataBatch(metadataElems.map { it })

    fun upsertMetaData(metadata: List<ItemMetadata>): IntArray = dbConnector.upsertMetadataBatch(metadata)

    fun getMetadataList(limit: Int, offset: Int): List<ItemMetadata> =
        dbConnector.getMetadataRange(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { ids ->
            getMetadataElementsByIds(ids).sortedBy { it.metadataId }
        } ?: emptyList()

    fun getMetadataElementsByIds(metadataIds: List<String>): List<ItemMetadata> = dbConnector.getMetadata(metadataIds)

    fun metadataContainsId(id: String): Boolean = dbConnector.metadataContainsId(id)

    fun rightContainsId(rightId: String): Boolean = dbConnector.rightContainsId(rightId)

    fun getRightsByIds(rightIds: List<String>): List<ItemRight> = dbConnector.getRights(rightIds)

    fun getItemByMetadataId(metadataId: String): Item? =
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
                    getMetadataElementsByIds(listOf(p.first)).first(),
                    dbConnector.getRights(p.second)
                )
            }
        } ?: emptyList()
    }

    fun itemContainsRight(rightId: String): Boolean = dbConnector.itemContainsRight(rightId)

    fun itemContainsMetadata(metadataId: String): Boolean = dbConnector.itemContainsMetadata(metadataId)

    fun itemContainsEntry(metadataId: String, rightId: String): Boolean =
        dbConnector.itemContainsEntry(metadataId, rightId)

    fun countItemByRightId(rightId: String) = dbConnector.countItemByRightId(rightId)

    fun deleteItemEntry(metadataId: String, rightId: String) = dbConnector.deleteItem(metadataId, rightId)

    fun deleteItemEntriesByMetadataId(metadataId: String) = dbConnector.deleteItemByMetadata(metadataId)

    fun deleteItemEntriesByRightId(rightId: String) = dbConnector.deleteItemByRight(rightId)

    fun deleteMetadata(metadataId: String): Int = dbConnector.deleteMetadata(listOf(metadataId))

    fun deleteRight(rightId: String): Int = dbConnector.deleteRights(listOf(rightId))

    fun getRightEntriesByMetadataId(metadataId: String): List<ItemRight> =
        dbConnector.getRightIdsByMetadata(metadataId).let {
            dbConnector.getRights(it)
        }
}
