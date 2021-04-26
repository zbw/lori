package de.zbw.api.helloworld.server

/**
 * An interface representing the lifecycle of a service.
 *
 * Created on 04-20-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
abstract class ServiceLifecycle {

    /**
     * Returns if the service is ready.
     */
    abstract fun isReady(): Boolean

    /**
     * Returns if the service is healthy.
     * If not, it usually has no way of coming back to a healthy state.
     */
    abstract fun isHealthy(): Boolean

    /**
     * Start the service.
     */
    abstract fun start()

    /**
     * Stop the service.
     */
    abstract fun stop()
}
