package de.zbw.api.lori.server.route

import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.routing.Route

/**
 * Route static content.
 *
 * Created on 05-05-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Route.staticRoutes() {
    static() {
        resources("dist")
        defaultResource("index.html", "dist")
    }
}
