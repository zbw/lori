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

    fun movedPermanently(detail: String? = "The target moved to another location."): ErrorRest =
        ErrorRest(
            type = "/redirection/301",
            title = "Target moved permanently.",
            detail = detail,
            status = "301",
        )

    fun internalServerError(detail: String? = "Bitte an Admin wenden."): ErrorRest =
        ErrorRest(
            type = "/errors/internal",
            title = "Unerwarteter interner Fehler.",
            detail = detail,
            status = "500",
        )

    fun unauthorizedError(detail: String? = "Benutzer ist nicht berechtigt."): ErrorRest =
        ErrorRest(
            type = "/errors/unauthorized",
            title = "Authentifizierung ist ungültig.",
            detail = detail,
            status = "401",
        )

    const val BAD_REQUEST_END_DATE = "Enddatum muss nach dem Startdatum liegen."
    const val BAD_REQUEST_RELATIONSHIP = "Ein Template kann nicht Vorgänger/Nachfolger von sich selbst sein."
    const val CONFLICT_RIGHTS = "Es gibt einen Start- und/oder Enddatum Konflikt mit bereits bestehenden Rechten"
    const val INVALID_JSON = "Das JSON Format ist ungültig und konnte nicht gelesen werden."
    const val NO_VALID_ID = "Die URL enthält keine gültige Id."
    const val NO_RESOURCE_FOR_ID = "Für die Id wurde keine Resource gefunden."
    const val RESOURCE_STILL_IN_USE = "Diese Resource wird noch benutzt und kann nicht gelöscht werden."
    const val USER_MISSING_RIGHTS = "Benutzer hat nicht die notwendigen Rechte\""
    const val USER_NOT_AUTHED = "User ist nicht authentifiziert."

    // Postgres Error code when inserting a duplicate (primary key) into the database.
    const val PSQL_CONFLICT_ERR_CODE = "23505"
}
