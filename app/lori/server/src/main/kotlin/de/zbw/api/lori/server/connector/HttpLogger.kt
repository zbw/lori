package de.zbw.api.lori.server.connector

import io.ktor.client.plugins.logging.*
import org.apache.logging.log4j.LogManager

class HttpLogger : Logger {
    override fun log(message: String) {
        LOG.debug(message)
    }

    companion object {
        internal val LOG: org.apache.logging.log4j.Logger = LogManager.getLogger(HttpLogger::class.java)
    }
}
