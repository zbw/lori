package de.zbw.persistence.access.server

import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.business.access.server.AccessState
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.ItemMetadata
import de.zbw.business.access.server.PublicationType
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Types

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    internal val connection: Connection,
) {

    constructor(
        config: AccessConfiguration,
    ) : this(DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword))

    fun insertMetadata(itemMetadata: ItemMetadata): String {
        val stmntAccIns =
            "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
                "(header_id,handle,ppn,ppn_ebook,title,title_journal," +
                "title_series,access_state,published_year,band,publication_type,doi," +
                "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
                "license_conditions,provenance_license) " +
                "VALUES(?,?,?,?,?,?," +
                "?,?,?,?,?,?," +
                "?,?,?,?,?,?," +
                "?,?)"

        val prepStmt = connection.prepareStatement(stmntAccIns, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, itemMetadata.id)
            this.setString(2, itemMetadata.handle)
            this.setIfNotNull(3, itemMetadata.ppn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, itemMetadata.ppnEbook) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(5, itemMetadata.title)
            this.setIfNotNull(6, itemMetadata.titleJournal) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(7, itemMetadata.titleSeries) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(8, itemMetadata.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setInt(9, itemMetadata.publicationYear)
            this.setIfNotNull(10, itemMetadata.band) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(11, itemMetadata.publicationType.toString())
            this.setIfNotNull(12, itemMetadata.doi) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(13, itemMetadata.serialNumber) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, itemMetadata.isbn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(15, itemMetadata.rightsK10plus) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(16, itemMetadata.paketSigel) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(17, itemMetadata.zbdId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(18, itemMetadata.issn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(19, itemMetadata.licenseConditions) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(20, itemMetadata.provenanceLicense) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
        val affectedRows = prepStmt.run { this.executeUpdate() }

        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getString(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    fun insertAction(action: Action, fkAccessRight: String): Long {
        val stmntActIns = "INSERT INTO $TABLE_NAME_ITEM_ACTION" +
            "(type, permission, header_id) " +
            "VALUES(?,?,?)"
        val prepStmt = connection.prepareStatement(stmntActIns, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, action.type.toString())
            this.setBoolean(2, action.permission)
            this.setString(3, fkAccessRight)
        }
        val affectedRows = prepStmt.run { this.executeUpdate() }
        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getLong(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    fun insertRestriction(restriction: Restriction, fkActionId: Long): Long {
        val stmntRestIns = "INSERT INTO $TABLE_NAME_ITEM_RESTRICTION" +
            "(type, attribute_type, attribute_values, action_id) " +
            "VALUES(?,?,?,?)"
        val prepStmt = connection.prepareStatement(stmntRestIns, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, restriction.type.toString())
            this.setString(2, restriction.attribute.type.name)
            this.setString(3, restriction.attribute.values.joinToString(separator = ";"))
            this.setLong(4, fkActionId)
        }
        val affectedRows = prepStmt.executeUpdate()
        return if (affectedRows > 0) {
            val rs: ResultSet = prepStmt.generatedKeys
            rs.next()
            rs.getLong(1)
        } else throw IllegalStateException("No row has been inserted.")
    }

    fun getMetadata(headerIds: List<String>): List<ItemMetadata> {
        val stmt =
            "SELECT" +
                " header_id,handle,ppn,ppn_ebook,title,title_journal," +
                "title_series,access_state,published_year,band,publication_type,doi," +
                "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
                "license_conditions,provenance_license " +
                "FROM $TABLE_NAME_ITEM_METADATA " +
                "WHERE header_id = ANY(?)"

        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val rs = prepStmt.executeQuery()
        return generateSequence {
            if (rs.next()) {
                ItemMetadata(
                    id = rs.getString(1),
                    handle = rs.getString(2),
                    ppn = rs.getString(3),
                    ppnEbook = rs.getString(4),
                    title = rs.getString(5),
                    titleJournal = rs.getString(6),
                    titleSeries = rs.getString(7),
                    accessState = rs.getString(8)?.let { AccessState.valueOf(it) },
                    publicationYear = rs.getInt(9),
                    band = rs.getString(10),
                    publicationType = PublicationType.valueOf(rs.getString(11)),
                    doi = rs.getString(12),
                    serialNumber = rs.getString(13),
                    isbn = rs.getString(14),
                    rightsK10plus = rs.getString(15),
                    paketSigel = rs.getString(16),
                    zbdId = rs.getString(17),
                    issn = rs.getString(18),
                    licenseConditions = rs.getString(19),
                    provenanceLicense = rs.getString(20),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun deleteAccessRights(headerIds: List<String>): Int {
        val keys = getAccessInformationKeys(headerIds)
        val existingHeaderIds = keys.map { it.headerId }
        val actionIds = keys.map { it.actionId }
        val restrictionIds = keys.map { it.restrictionId }

        deleteRestrictions(restrictionIds)
        deleteActions(actionIds)
        return deleteHeader(existingHeaderIds)
    }

    internal fun getAccessInformationKeys(headerIds: List<String>): List<JoinHeaderActionRestrictionIdTransient> {
        // First, receive the required primary keys.
        val stmt =
            "SELECT a.header_id, a.action_id, r.restriction_id " +
                "FROM $TABLE_NAME_ITEM_ACTION a " +
                "LEFT JOIN $TABLE_NAME_ITEM_RESTRICTION r ON a.action_id = r.action_id " +
                "WHERE a.header_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val rs = prepStmt.executeQuery()
        return generateSequence {
            if (rs.next()) {
                JoinHeaderActionRestrictionIdTransient(
                    headerId = rs.getString(1),
                    actionId = rs.getInt(2),
                    restrictionId = rs.getInt(3)
                )
            } else null
        }.takeWhile { true }.toList()
    }

    private fun deleteRestrictions(restrictionIds: List<Int>): Int {
        val stmt =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_RESTRICTION r " +
                "WHERE r.restriction_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("integer", restrictionIds.toTypedArray()))
        }
        return prepStmt.run { this.executeUpdate() }
    }

    private fun deleteActions(actionIds: List<Int>): Int {
        val stmt =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_ACTION a " +
                "WHERE a.action_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("integer", actionIds.toTypedArray()))
        }
        return prepStmt.run { this.executeUpdate() }
    }

    private fun deleteHeader(headerIds: List<String>): Int {
        val stmt =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.header_id = ANY(?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        return prepStmt.run { this.executeUpdate() }
    }

    fun getActions(headerIds: List<String>): Map<String, List<Action>> {
        val stmt =
            "SELECT a.header_id, a.type, a.permission, r.type, r.attribute_type, r.attribute_values " +
                "FROM $TABLE_NAME_ITEM_ACTION a " +
                "LEFT JOIN $TABLE_NAME_ITEM_RESTRICTION r ON a.action_id = r.action_id " +
                "WHERE a.header_id = ANY(?)"

        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val rs = prepStmt.executeQuery()
        val joinResult: List<JoinActionRestrictionTransient> = generateSequence {
            if (rs.next()) {
                JoinActionRestrictionTransient(
                    headerId = rs.getString(1),
                    actionType = rs.getString(2),
                    actionPermission = rs.getBoolean(3),
                    restrictionType = rs.getObject(4) as String?,
                    attributeType = rs.getObject(5) as String?,
                    attributeValues = rs.getObject(6) as String?,
                )
            } else null
        }.takeWhile { true }.toList()
        return joinResult.groupBy { it.headerId }.map { entry ->
            Pair(
                entry.key,
                entry.value.groupBy { it.actionType }.map { actionLvl ->
                    Action(
                        type = ActionType.valueOf(actionLvl.key),
                        permission = actionLvl.value.first().actionPermission,
                        restrictions = actionLvl.value.filter { it.restrictionType != null && it.attributeType != null && it.attributeValues != null }
                            .map { restrictionLvl ->
                                Restriction(
                                    type = RestrictionType.valueOf(restrictionLvl.restrictionType!!),
                                    attribute = Attribute(
                                        type = AttributeType.valueOf(restrictionLvl.attributeType!!),
                                        values = restrictionLvl.attributeValues!!.split(","),
                                    )
                                )
                            }
                    )
                }
            )
        }.toMap()
    }

    fun containsHeader(headerId: String): Boolean {
        val stmt = "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE header_id=?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setString(1, headerId)
        }
        val rs = prepStmt.executeQuery()
        rs.next()
        return rs.getBoolean(1)
    }

    fun getAccessRightIds(limit: Int, offset: Int): List<String> {
        val stmt = "SELECT header_id from $TABLE_NAME_ITEM_METADATA ORDER BY header_id ASC LIMIT ? OFFSET ?"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }
        val rs = prepStmt.executeQuery()

        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else null
        }.takeWhile { true }.toList()
    }

    private fun <T> PreparedStatement.setIfNotNull(
        idx: Int,
        element: T?,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        const val TABLE_NAME_ITEM_ACTION = "item_action"
        const val TABLE_NAME_ITEM_RESTRICTION = "item_restriction"
    }
}
