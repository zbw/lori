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

    fun insertItems(items: List<Item>): List<String> =
        items.map { insertItem(it) }

    fun insertItem(item: Item): String {
        val fkItem: String = dbConnector.insertMetadata(item.itemMetadata)
        item.actions.forEach { act ->
            val fkAction = dbConnector.insertAction(act, fkItem)
            act.restrictions.forEach { r ->
                fkAction.let { dbConnector.insertRestriction(r, fkAction) }
            }
        }
        return fkItem
    }

    fun upsertItems(items: List<Item>) {
        dbConnector.upsertMetadataBatch(items.map { it.itemMetadata })
        // delete
        dbConnector.deleteActionAndRestrictedEntries(items.map { it.itemMetadata.id })
        items.forEach { item ->
            item.actions.forEach { act ->
                val fkAction = dbConnector.insertAction(act, item.itemMetadata.id)
                act.restrictions.forEach { r ->
                    fkAction.let { dbConnector.insertRestriction(r, fkAction) }
                }
            }
        }
    }

    fun upsertMetaData(metadata: List<ItemMetadata>): IntArray = dbConnector.upsertMetadataBatch(metadata)

    fun getItems(ids: List<String>): List<Item> {
        val headerToActions: Map<String, List<Action>> = dbConnector.getActions(ids)
        return dbConnector.getMetadata(ids).map {
            Item(
                itemMetadata = it,
                actions = headerToActions[it.id] ?: emptyList()
            )
        }
    }

    fun deleteAccessRightEntries(ids: List<String>): Int = dbConnector.deleteItems(ids)

    fun containsAccessRightId(id: String): Boolean = dbConnector.containsHeader(id)

    fun getAccessRightList(limit: Int, offset: Int): List<Item> {
        return dbConnector.getItemIds(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { ids ->
            getItems(ids).sortedBy { it.itemMetadata.id }
        } ?: emptyList()
    }
}
