package de.zbw.api.handle.server

import de.zbw.api.handle.server.config.HandleConfiguration
import de.zbw.business.handle.server.HandleCommunicator
import de.zbw.persistence.handle.server.FlywayMigrator
import net.handle.hdllib.AbstractMessage

/**
 * Lifecycle service lifecycle.
 *
 * Created on 06-03-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class HandleServiceLifecycle(
    config: HandleConfiguration,
    private val handleCommunicator: HandleCommunicator,
    private val migrator: FlywayMigrator = FlywayMigrator(config)
) : ServiceLifecycle() {

    override fun isReady(): Boolean {
        val resp = handleCommunicator.listHandleValues()
        return resp.responseCode == AbstractMessage.RC_SUCCESS
    }

    override fun start() {
        migrator.migrate()
    }
}
