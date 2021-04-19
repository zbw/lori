package de.zbw.server.helloworld

import org.slf4j.LoggerFactory

object HelloWorldServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting HelloWorldServer :)")
    }

    private val LOG = LoggerFactory.getLogger(HelloWorldServer::class.java)
}
