package de.zbw.api.helloworld.server

import org.slf4j.LoggerFactory

object HelloWorldServer {
    @JvmStatic
    fun main(args: Array<String>) {
        LOG.info("Starting the HelloWorldServer :)")
        ServicePoolWithProbes(listOf()).start()
    }

    private val LOG = LoggerFactory.getLogger(HelloWorldServer::class.java)
}
