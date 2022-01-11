package de.zbw.business.access.server

import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.persistence.access.server.DatabaseConnector

/**
 * Backend for the Access-Server.
 *
 * Created on 07-15-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class AccessServerBackend(
    private val dbConnector: DatabaseConnector,
) {
    constructor(
        config: AccessConfiguration,
    ) : this(
        DatabaseConnector(config),
    )

    fun insertAccessRightEntries(items: List<Item>): List<String> =
        items.map { insertAccessRightEntry(it) }

    fun insertAccessRightEntry(item: Item): String {
        val fkAccessRight: String = dbConnector.insertMetadata(item.metadata)
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
        return dbConnector.getHeaders(ids).map {
            Item(
                metadata = it,
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
            getAccessRightEntries(headerIds).sortedBy { it.metadata.id }
        } ?: emptyList()
    }
}
