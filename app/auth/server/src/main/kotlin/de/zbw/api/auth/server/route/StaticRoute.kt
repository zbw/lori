package de.zbw.api.auth.server.route

import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route

/**
 * Route static content.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
fun Route.staticRoutes() {
    static() {
        resources("dist")
        defaultResource("index.html", "dist")
    }
}
