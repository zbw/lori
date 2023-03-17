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

    fun unauthorizedError(detail: String? = "Benutzter ist nicht berechtigt."): ErrorRest =
        ErrorRest(
            type = "/errors/unauthorized",
            title = "User und/oder Passwort ungültig.",
            detail = detail,
            status = "401",
        )

    const val EXPIRED_JWT = "Authentifizierung ist nicht mehr gültig.."
    const val INVALID_JSON = "Das JSON Format ist ungültig und konnte nicht gelesen werden."
    const val NO_VALID_ID = "Die URL enthält keine gültige Id."
    const val NO_RESOURCE_FOR_ID = "Für die Id wurde keine Resource gefunden."
    const val RESOURCE_STILL_IN_USE = "Diese Resource wird noch benutzt und kann nicht gelöscht werden."

    // Postgres Error code when inserting a duplicate (primary key) into the database.
    const val PSQL_CONFLICT_ERR_CODE = "23505"
}
