package de.zbw.business.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.persistence.lori.server.DatabaseConnector

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
    ) : this(
        DatabaseConnector(config),
    )

    fun insertAccessRightEntries(items: List<Item>): List<String> =
        items.map { insertAccessRightEntry(it) }

    fun insertAccessRightEntry(item: Item): String {
        val fkAccessRight: String = dbConnector.insertMetadata(item.itemMetadata)
        item.actions.forEach { act ->
            val fkAction = dbConnector.insertAction(act, fkAccessRight)
            act.restrictions.forEach { r ->
                fkAction.let { dbConnector.insertRestriction(r, fkAction) }
            }
        }
        return fkAccessRight
    }

    fun getAccessRightEntries(ids: List<String>): List<Item> {
        val headerToActions: Map<String, List<Action>> = dbConnector.getActions(ids)
        return dbConnector.getMetadata(ids).map {
            Item(
                itemMetadata = it,
                actions = headerToActions[it.id] ?: emptyList()
            )
        }
    }

    fun deleteAccessRightEntries(ids: List<String>): Int = dbConnector.deleteAccessRights(ids)

    fun containsAccessRightId(id: String): Boolean = dbConnector.containsHeader(id)


    fun getAccessRightList(limit: Int, offset: Int): List<Item> {
        return dbConnector.getAccessRightIds(limit, offset).takeIf {
            it.isNotEmpty()
        }?.let { headerIds ->
            getAccessRightEntries(headerIds).sortedBy { it.itemMetadata.id }
        } ?: emptyList()
    }
}
