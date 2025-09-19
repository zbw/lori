package de.zbw.api.lori.server.route

import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route

/**
 * Route static content.
 *
 * Created on 05-05-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Route.staticRoutes() {
    staticResources(
        remotePath = "/", // URL path prefix
        basePackage = "dist", // folder in resources
    )
}
