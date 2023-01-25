package de.zbw.api.lori.server.exception

/**
 * Exception used whenever a resource is still in use and should
 * be changed in a way that requires it to be unused.
 *
 * Created on 01-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ResourceStillInUseException(message: String) : Exception(message)
