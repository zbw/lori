package de.zbw.api.lori.server.route

import de.zbw.lori.model.ErrorRest

/**
 * Errors which can be returned from the API.
 *
 * Created on 01-26-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
object ApiError {
    fun badRequestError(detail: String?): ErrorRest =
        ErrorRest(
            type = "/errors/badrequest",
            title = "Ungültiges Eingabeformat",
            detail = detail,
            status = "400",
        )

    fun notFoundError(detail: String?): ErrorRest =
        ErrorRest(
            type = "/errors/notfound",
            title = "Resource wurde nicht gefunden.",
            detail = detail,
            status = "404",
        )

    fun conflictError(detail: String?): ErrorRest =
        ErrorRest(
            type = "/errors/conflict",
            title = "Resourcen Konflikt",
            detail = detail,
            status = "409",
        )

    fun internalServerError(detail: String? = "Bitte an Admin wenden."): ErrorRest =
        ErrorRest(
            type = "/errors/internal",
            title = "Unerwarteter interner Fehler.",
            detail = detail,
            status = "500",
        )
}
