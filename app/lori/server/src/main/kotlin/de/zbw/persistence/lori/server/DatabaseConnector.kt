package de.zbw.persistence.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.Action
import de.zbw.business.lori.server.ActionType
import de.zbw.business.lori.server.Attribute
import de.zbw.business.lori.server.AttributeType
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.PublicationType
import de.zbw.business.lori.server.Restriction
import de.zbw.business.lori.server.RestrictionType
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    val connection: Connection,
    private val tracer: Tracer,
) {

    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword),
        tracer
    )

    fun upsertMetadataBatch(itemMetadatas: List<ItemMetadata>): IntArray {

        val prep = connection.prepareStatement(STATEMENT_UPSERT_METADATA)
        connection.autoCommit = false
        itemMetadatas.map {
            val p = insertUpsertSetParameters(it, prep)
            p.addBatch()
        }
        val span = tracer.spanBuilder("upsertMetadataBatch").startSpan()
        try {
            span.makeCurrent()
            val rows: IntArray = prep.executeBatch()
            connection.commit()
            connection.autoCommit = true
            return rows
        } finally {
            span.end()
        }
    }

    fun insertMetadata(itemMetadata: ItemMetadata): String {

        val prepStmt = insertUpsertSetParameters(
            itemMetadata,
            connection.prepareStatement(STATEMENT_INSERT_METADATA, Statement.RETURN_GENERATED_KEYS),
        )

        val span = tracer.spanBuilder("insertMetadata").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = prepStmt.run { this.executeUpdate() }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    private fun insertUpsertSetParameters(
        itemMetadata: ItemMetadata,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()
        return prep.apply {
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
            this.setTimestamp(21, Timestamp.from(now))
            this.setTimestamp(22, Timestamp.from(now))
            this.setIfNotNull(23, itemMetadata.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(24, itemMetadata.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
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
        val span = tracer.spanBuilder("insertAction").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = prepStmt.run { this.executeUpdate() }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getLong(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun insertRestriction(restriction: Restriction, fkActionId: Long): Long {
        val prepStmt =
            connection.prepareStatement(STATEMENT_INSERT_RESTRICTION, Statement.RETURN_GENERATED_KEYS).apply {
                this.setString(1, restriction.type.toString())
                this.setString(2, restriction.attribute.type.name)
                this.setString(3, restriction.attribute.values.joinToString(separator = ";"))
                this.setLong(4, fkActionId)
            }
        val span = tracer.spanBuilder("insertRestriction").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = prepStmt.executeUpdate()
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getLong(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun getMetadata(headerIds: List<String>): List<ItemMetadata> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_METADATA).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
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
                    createdOn = rs.getTimestamp(21)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    lastUpdatedOn = rs.getTimestamp(22)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    createdBy = rs.getString(23),
                    lastUpdatedBy = rs.getString(24),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun deleteItems(itemIds: List<String>): Int {
        val keys = getItemPrimaryKeys(itemIds)
        val existingHeaderIds = keys.map { it.headerId }
        val actionIds = keys.map { it.actionId }
        val restrictionIds = keys.map { it.restrictionId }

        deleteRestrictions(restrictionIds)
        deleteActions(actionIds)
        return deleteHeader(existingHeaderIds)
    }

    fun deleteActionAndRestrictedEntries(itemIds: List<String>): Int {
        val keys = getItemPrimaryKeys(itemIds)
        val actionIds = keys.map { it.actionId }
        val restrictionIds = keys.map { it.restrictionId }

        deleteRestrictions(restrictionIds)
        return deleteActions(actionIds)
    }

    internal fun getItemPrimaryKeys(headerIds: List<String>): List<JoinHeaderActionRestrictionIdTransient> {
        // First, receive the required primary keys.
        val prepStmt = connection.prepareStatement(STATEMENT_GET_PRIMARY_KEYS).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("getItemPrimaryKeys").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
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
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_RESTRICTIONS).apply {
            this.setArray(1, connection.createArrayOf("integer", restrictionIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteRestrictions").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    private fun deleteActions(actionIds: List<Int>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ACTIONS).apply {
            this.setArray(1, connection.createArrayOf("integer", actionIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteActions").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    private fun deleteHeader(headerIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_HEADER).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteMetadata").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    fun getActions(headerIds: List<String>): Map<String, List<Action>> {

        val prepStmt = connection.prepareStatement(STATEMENT_GET_ACTIONS).apply {
            this.setArray(1, connection.createArrayOf("text", headerIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
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
        val span = tracer.spanBuilder("containsHeader").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getItemIds(limit: Int, offset: Int): List<String> {
        val stmt = "SELECT header_id from $TABLE_NAME_ITEM_METADATA ORDER BY header_id ASC LIMIT ? OFFSET ?"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }

        val span = tracer.spanBuilder("getItemIds").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }

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
        const val STATEMENT_UPSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(header_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,access_state,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "license_conditions,provenance_license,created_on," +
            "last_updated_on,created_by,last_updated_by) " +
            "VALUES(?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?) " +
            "ON CONFLICT (header_id) " +
            "DO UPDATE SET " +
            "handle = EXCLUDED.handle," +
            "ppn = EXCLUDED.ppn," +
            "ppn_ebook = EXCLUDED.ppn_ebook," +
            "title = EXCLUDED.title," +
            "title_journal = EXCLUDED.title_journal," +
            "title_series = EXCLUDED.title_series," +
            "access_state = EXCLUDED.access_state," +
            "published_year = EXCLUDED.published_year," +
            "band = EXCLUDED.band," +
            "publication_type = EXCLUDED.publication_type," +
            "doi = EXCLUDED.doi," +
            "serial_number = EXCLUDED.serial_number," +
            "isbn = EXCLUDED.isbn," +
            "rights_k10plus = EXCLUDED.rights_k10plus," +
            "paket_sigel = EXCLUDED.paket_sigel," +
            "zbd_id = EXCLUDED.zbd_id," +
            "issn = EXCLUDED.issn," +
            "license_conditions = EXCLUDED.license_conditions," +
            "provenance_license = EXCLUDED.provenance_license," +
            "last_updated_on = EXCLUDED.last_updated_on," +
            "last_updated_by = EXCLUDED.last_updated_by;"

        const val STATEMENT_INSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(header_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,access_state,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "license_conditions,provenance_license,created_on," +
            "last_updated_on,created_by,last_updated_by) " +
            "VALUES(?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?)"

        const val STATEMENT_INSERT_RESTRICTION = "INSERT INTO $TABLE_NAME_ITEM_RESTRICTION" +
            "(type, attribute_type, attribute_values, action_id) " +
            "VALUES(?,?,?,?)"

        const val STATEMENT_GET_METADATA = "SELECT header_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,access_state,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "license_conditions,provenance_license,created_on,last_updated_on,created_by,last_updated_by " +
            "FROM $TABLE_NAME_ITEM_METADATA " +
            "WHERE header_id = ANY(?)"

        const val STATEMENT_GET_PRIMARY_KEYS = "SELECT a.header_id, a.action_id, r.restriction_id " +
            "FROM $TABLE_NAME_ITEM_ACTION a " +
            "LEFT JOIN $TABLE_NAME_ITEM_RESTRICTION r ON a.action_id = r.action_id " +
            "WHERE a.header_id = ANY(?)"

        const val STATEMENT_DELETE_RESTRICTIONS = "DELETE " +
            "FROM $TABLE_NAME_ITEM_RESTRICTION r " +
            "WHERE r.restriction_id = ANY(?)"

        const val STATEMENT_DELETE_ACTIONS = "DELETE " +
            "FROM $TABLE_NAME_ITEM_ACTION a " +
            "WHERE a.action_id = ANY(?)"

        const val STATEMENT_DELETE_HEADER =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.header_id = ANY(?)"

        const val STATEMENT_GET_ACTIONS =
            "SELECT a.header_id, a.type, a.permission, r.type, r.attribute_type, r.attribute_values " +
                "FROM $TABLE_NAME_ITEM_ACTION a " +
                "LEFT JOIN $TABLE_NAME_ITEM_RESTRICTION r ON a.action_id = r.action_id " +
                "WHERE a.header_id = ANY(?)"
    }
}
