package de.zbw.api.lori.server.exception

/**
 * Some kind of conflict did occur. Usually related to a 409 status code.
 *
 * Created on 01-30-2024.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ResourceConflictException(
    message: String,
) : Exception(message)
