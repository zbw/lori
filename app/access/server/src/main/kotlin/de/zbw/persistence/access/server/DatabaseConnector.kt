package de.zbw.persistence.access.server

import de.zbw.api.access.server.config.AccessConfiguration
import de.zbw.business.access.server.Action
import de.zbw.business.access.server.ActionType
import de.zbw.business.access.server.Attribute
import de.zbw.business.access.server.AttributeType
import de.zbw.business.access.server.Header
import de.zbw.business.access.server.Restriction
import de.zbw.business.access.server.RestrictionType
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 07-14-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    internal val connection: Connection,
) {

    constructor(
        config: AccessConfiguration,
    ) : this(DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword))

    fun insertHeader(header: Header): String {
        try {
            val stmntAccIns =
                "INSERT INTO $TABLE_NAME_HEADER" +
                    "(header_id,tenant,usage_guide,template,mention,sharealike,commercial_use,copyright) " +
                    "VALUES(?,?,?,?,?,?,?,?)"

            val prepStmt = connection.prepareStatement(stmntAccIns, Statement.RETURN_GENERATED_KEYS).apply {
                this.setString(1, header.id)
                this.setIfNotNull(header.tenant, 2) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(header.usageGuide, 3) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(header.template, 4) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setBoolean(5, header.mention)
                this.setBoolean(6, header.shareAlike)
                this.setBoolean(7, header.commercialUse)
                this.setBoolean(8, header.copyright)
            }
            val affectedRows = prepStmt.run { this.executeUpdate() }

            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw StatusRuntimeException(
                Status.INTERNAL.withDescription(
                    "No row has been inserted."
                )
            )
        } catch (exc: SQLException) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(exc.cause)
                    .withDescription("Error while inserting an access right entry: ${exc.message}")
            )
        }
    }

    fun insertAction(action: Action, fkAccessRight: String): Long {
        try {
            val stmntActIns = "INSERT INTO $TABLE_NAME_ACTION" +
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
            } else throw StatusRuntimeException(
                Status.INTERNAL.withDescription(
                    "No row has been inserted."
                )
            )
        } catch (exc: SQLException) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(exc.cause)
                    .withDescription("Error while inserting an action: ${exc.message}")
            )
        }
    }

    fun insertRestriction(restriction: Restriction, fkActionId: Long): Long {
        try {
            val stmntRestIns = "INSERT INTO $TABLE_NAME_RESTRICTION" +
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
            } else throw StatusRuntimeException(
                Status.INTERNAL.withDescription(
                    "No row has been inserted."
                )
            )
        } catch (exc: SQLException) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(exc.cause)
                    .withDescription("Error while inserting a restriction: ${exc.message}")
            )
        }
    }

    fun getHeaders(headerIds: List<String>): List<Header> {
        try {
            val stmt =
                "SELECT header_id, tenant, usage_guide, template, mention, sharealike, commercial_use, copyright " +
                    "FROM $TABLE_NAME_HEADER " +
                    "WHERE header_id = ANY(?)"

            val prepStmt = connection.prepareStatement(stmt).apply {
                this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
            }
            val rs = prepStmt.executeQuery()
            return generateSequence {
                if (rs.next()) {
                    Header(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getBoolean(5),
                        rs.getBoolean(6),
                        rs.getBoolean(7),
                        rs.getBoolean(8),
                    )
                } else null
            }.takeWhile { true }.toList()
        } catch (exc: SQLException) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(exc.cause)
                    .withDescription("Error while querying header information: ${exc.message}")
                    .withCause(exc)
            )
        }
    }

    fun getActions(headerIds: List<String>): Map<String, List<Action>> {
        try {
            val stmt =
                "SELECT a.header_id, a.type, a.permission, r.type, r.attribute_type, r.attribute_values " +
                    "FROM $TABLE_NAME_ACTION a " +
                    "LEFT JOIN $TABLE_NAME_RESTRICTION r ON a.action_id = r.action_id " +
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
        } catch (exc: SQLException) {
            throw StatusRuntimeException(
                Status.INTERNAL.withCause(exc.cause)
                    .withDescription("Error while querying actions: ${exc.message}")
                    .withCause(exc)
            )
        }
    }

    private fun <T> PreparedStatement.setIfNotNull(
        element: T?,
        idx: Int,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        const val TABLE_NAME_HEADER = "access_right_header"
        const val TABLE_NAME_ACTION = "access_right_action"
        const val TABLE_NAME_RESTRICTION = "access_right_restriction"
    }
}
