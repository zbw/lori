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
    config: AccessConfiguration,
    private val dbConnector: DatabaseConnector = DatabaseConnector(config),
) {

    fun insertAccessRightEntries(accessRights: List<AccessRight>) =
        accessRights.forEach { insertAccessRightEntry(it) }

    fun insertAccessRightEntry(accessRight: AccessRight) {
        val fkAccessRight: String = dbConnector.insertHeader(accessRight.header)
        accessRight.actions.forEach { act ->
            val fkAction = dbConnector.insertAction(act, fkAccessRight)
            act.restrictions.forEach { r ->
                fkAction.let { dbConnector.insertRestriction(r, fkAction) }
            }
        }
    }

    fun getAccessRightEntries(ids: List<String>): List<AccessRight> {
        val headerToActions: Map<String, List<Action>> = dbConnector.getActions(ids)
        return dbConnector.getHeaders(ids).map {
            AccessRight(
                header = it,
                actions = headerToActions[it.id] ?: listOf()
            )
        }
    }
}
