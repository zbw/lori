package de.zbw.persistence.access.server

/**
 * Data class for storing results from more
 * complex database queries.
 *
 * Created on 07-20-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class JoinActionRestrictionTransient(
    val headerId: String,
    val actionType: String,
    val actionPermission: Boolean,
    val restrictionType: String?,
    val attributeType: String?,
    val attributeValues: String?,
)
