package de.zbw.api.lori.server.exception

/**
* Received [de.zbw.lori.model.GroupRest] had invalid IP addresses.
*
* Created on 06-28-2025.
* @author Christian Bay (c.bay@zbw.eu)
*/
class InvalidIPAddressException(
    message: String,
) : Exception(message)
