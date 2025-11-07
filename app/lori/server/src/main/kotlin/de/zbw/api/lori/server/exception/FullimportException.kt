package de.zbw.api.lori.server.exception

/**
* Custom exception which indicates an error during the fullimport.
*
* Created on 07-11-2025.
* @author Christian Bay (c.bay@zbw.eu)
*/
class FullimportException(
    message: String,
) : Exception(message)
