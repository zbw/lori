package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ACCESS_STATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_LICENCE_CONTRACT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_TEMPLATE_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ZBW_USER_AGREEMENT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_RIGHT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Execute SQL queries strongly related to rights.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
    private val groupDB: GroupDB,
) {
    suspend fun insertRight(right: ItemRight): String =
        connectionPool.useConnection { connection ->
            val prepStmt =
                insertRightSetParameters(
                    right,
                    connection.prepareStatement(STATEMENT_INSERT_RIGHT, Statement.RETURN_GENERATED_KEYS),
                )
            val span = tracer.spanBuilder("insertRight").startSpan()
            try {
                span.makeCurrent()
                val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
                return@useConnection if (affectedRows > 0) {
                    val rs: ResultSet = prepStmt.generatedKeys
                    rs.next()
                    rs.getString(1)
                } else {
                    throw IllegalStateException("No row has been inserted.")
                }
            } finally {
                span.end()
            }
        }

    suspend fun upsertRight(right: ItemRight): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                upsertRightSetParameters(
                    right,
                    connection.prepareStatement(STATEMENT_UPSERT_RIGHT),
                )
            val span = tracer.spanBuilder("upsertRight").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    private fun upsertRightSetParameters(
        right: ItemRight,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()

        return prep.apply {
            this.setString(1, right.rightId)
            this.setTimestamp(2, Timestamp.from(now))
            this.setTimestamp(3, Timestamp.from(now))
            this.setIfNotNull(4, right.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(5, right.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(6, right.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(7, right.startDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(8, right.endDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(9, right.notesGeneral) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(10, right.licenceContract) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(11, right.authorRightException) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(12, right.zbwUserAgreement) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(13, right.openContentLicence) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, right.nonStandardOpenContentLicenceURL) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(15, right.nonStandardOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(16, right.restrictedOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(17, right.notesFormalRules) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(18, right.basisStorage) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(19, right.basisAccessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(20, right.notesProcessDocumentation) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(21, right.notesManagementRelated) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(22, right.isTemplate) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(23, right.templateName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(24, right.templateDescription) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(25, right.exceptionFrom) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    private fun insertRightSetParameters(
        right: ItemRight,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()

        return prep.apply {
            this.setTimestamp(1, Timestamp.from(now))
            this.setTimestamp(2, Timestamp.from(now))
            this.setIfNotNull(3, right.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(4, right.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(5, right.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(6, right.startDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(7, right.endDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(8, right.notesGeneral) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(9, right.licenceContract) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(10, right.authorRightException) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(11, right.zbwUserAgreement) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(12, right.openContentLicence) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(13, right.nonStandardOpenContentLicenceURL) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, right.nonStandardOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(15, right.restrictedOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(16, right.notesFormalRules) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(17, right.basisStorage) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(18, right.basisAccessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(19, right.notesProcessDocumentation) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(20, right.notesManagementRelated) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(21, right.isTemplate) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(22, right.templateName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(23, right.templateDescription) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(24, right.exceptionFrom) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    suspend fun deleteRightsByIds(rightIds: List<String>): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_RIGHTS).apply {
                    this.setArray(1, connection.createArrayOf("text", rightIds.toTypedArray()))
                }
            val span = tracer.spanBuilder("deleteRightsByIds").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun getRightsByIds(rightsIds: List<String>): List<ItemRight> {
        val rights: List<ItemRight> =
            connectionPool.useConnection { connection ->
                val prepStmt =
                    connection.prepareStatement(STATEMENT_GET_RIGHTS).apply {
                        this.setArray(1, connection.createArrayOf("text", rightsIds.toTypedArray()))
                    }

                val span = tracer.spanBuilder("getRightsByIds").startSpan()
                val rs =
                    try {
                        span.makeCurrent()
                        runInTransaction(connection) { prepStmt.executeQuery() }
                    } finally {
                        span.end()
                    }
                return@useConnection generateSequence {
                    if (rs.next()) {
                        extractRightFromRS(rs)
                    } else {
                        null
                    }
                }.takeWhile { true }.toList()
            }
        return rights.map { r ->
            val groups = groupDB.getGroupsByRightId(r.rightId!!)
            r.copy(
                groups = groups,
                groupIds = groups.map { it.groupId },
            )
        }
    }

    suspend fun rightContainsId(rightId: String): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_RIGHT_CONTAINS_ID).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("rightContainsId").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            rs.next()
            return@useConnection rs.getBoolean(1)
        }

    suspend fun getRightIdsByMetadata(metadataId: String): List<String> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_RIGHTSIDS_FOR_METADATA).apply {
                    this.setString(1, metadataId)
                }
            val span = tracer.spanBuilder("getRightIdsByMetadata").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            return@useConnection generateSequence {
                if (rs.next()) {
                    rs.getString(1)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun getTemplateList(
        limit: Int,
        offset: Int,
    ): List<ItemRight> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_TEMPLATES).apply {
                    this.setInt(1, limit)
                    this.setInt(2, offset)
                }

            val span = tracer.spanBuilder("getTemplatesByIds").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            return@useConnection generateSequence {
                if (rs.next()) {
                    extractRightFromRS(rs)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    /**
     * Get all RightIds for all templates.
     */
    suspend fun getRightIdsForAllTemplates(): List<String> =
        connectionPool.useConnection { connection ->
            val prepStmt = connection.prepareStatement(STATEMENT_GET_ALL_IDS_OF_TEMPLATES)
            val span = tracer.spanBuilder("getRightIdsForAllTemplates").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            return@useConnection generateSequence {
                if (rs.next()) {
                    rs.getString(1)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun updateAppliedOnByTemplateId(rightId: String): Int =
        connectionPool.useConnection { connection ->
            val now = Instant.now()
            val prepStmt =
                connection.prepareStatement(STATEMENT_UPDATE_TEMPLATE_APPLIED_ON).apply {
                    this.setTimestamp(1, Timestamp.from(now)) // last_applied_on
                    this.setString(2, rightId)
                }
            val span = tracer.spanBuilder("updateTemplateById").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun getRightsByTemplateNames(templateNames: List<String>): List<ItemRight> {
        val rights =
            connectionPool.useConnection { connection ->
                if (templateNames.isEmpty()) {
                    emptyList<ItemRight>()
                }

                val prepStmt =
                    connection.prepareStatement(STATEMENT_GET_RIGHTS_BY_TEMPLATE_NAME).apply {
                        this.setArray(1, connection.createArrayOf("varchar", templateNames.toTypedArray()))
                    }

                val span = tracer.spanBuilder("getRightIdsByTemplateNames").startSpan()
                val rs =
                    try {
                        span.makeCurrent()
                        runInTransaction(connection) { prepStmt.executeQuery() }
                    } finally {
                        span.end()
                    }
                return@useConnection generateSequence {
                    if (rs.next()) {
                        extractRightFromRS(rs)
                    } else {
                        null
                    }
                }.takeWhile { true }.toList()
            }
        return rights.map { r ->
            val groups = groupDB.getGroupsByRightId(r.rightId!!)
            r.copy(
                groups = groups,
                groupIds = groups.map { it.groupId },
            )
        }
    }

    /**
     * Return all Templates that are an exception for the given rightId.
     */
    suspend fun getExceptionsByRightId(rightId: String): List<ItemRight> {
        val rights =
            connectionPool.useConnection { connection ->
                val prepStmt =
                    connection.prepareStatement(STATEMENT_GET_EXCEPTIONS_BY_RIGHT_ID).apply {
                        this.setString(1, rightId)
                    }

                val span = tracer.spanBuilder("getExceptionsByTemplateId").startSpan()
                val rs =
                    try {
                        span.makeCurrent()
                        runInTransaction(connection) { prepStmt.executeQuery() }
                    } finally {
                        span.end()
                    }
                return@useConnection generateSequence {
                    if (rs.next()) {
                        extractRightFromRS(rs)
                    } else {
                        null
                    }
                }.takeWhile { true }.toList()
            }
        return rights.map { r ->
            val groups = groupDB.getGroupsByRightId(r.rightId!!)
            r.copy(
                groups = groups,
                groupIds = groups.map { it.groupId },
            )
        }
    }

    /**
     * Checks if a given RightId is an exception.
     */
    suspend fun isException(rightId: String): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_IS_EXCEPTION).apply {
                    this.setString(1, rightId)
                }

            val span = tracer.spanBuilder("isTemplateAnException").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            rs.next()
            return@useConnection (rs.getInt(1) == 1)
        }

    /**
     * Connects an exception with a template.
     */
    suspend fun addExceptionToTemplate(
        rightIdTemplate: String,
        rightIdExceptions: List<String>,
    ): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_UPDATE_TEMPLATE_EXCEPTION_FROM).apply {
                    this.setString(1, rightIdTemplate)
                    this.setArray(2, connection.createArrayOf("text", rightIdExceptions.toTypedArray()))
                }
            val span = tracer.spanBuilder("addExceptionToTemplate").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    companion object {
        private const val COLUMN_IS_TEMPLATE = "is_template"
        private const val COLUMN_EXCEPTION_FROM = "exception_from"

        const val STATEMENT_GET_ALL_IDS_OF_TEMPLATES =
            "SELECT $COLUMN_RIGHT_ID" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_IS_TEMPLATE = true"

        const val STATEMENT_GET_RIGHTS =
            "SELECT $COLUMN_RIGHT_ID, created_on, last_updated_on, created_by," +
                "last_updated_by, $COLUMN_RIGHT_ACCESS_STATE, start_date, end_date, notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT, author_right_exception, $COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE, notes_formal_rules, basis_storage," +
                "basis_access_state, notes_process_documentation, notes_management_related," +
                "$COLUMN_IS_TEMPLATE, template_name, template_description, last_applied_on, $COLUMN_EXCEPTION_FROM," +
                COLUMN_EXCEPTION_FROM +
                " FROM $TABLE_NAME_ITEM_RIGHT " +
                " WHERE $COLUMN_RIGHT_ID = ANY(?)"

        const val STATEMENT_GET_RIGHTSIDS_FOR_METADATA =
            "SELECT right_id" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE metadata_id = ?"

        const val STATEMENT_RIGHT_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_RIGHT WHERE right_id=?)"

        const val STATEMENT_INSERT_RIGHT =
            "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
                "(created_on,last_updated_on," +
                "created_by,last_updated_by,$COLUMN_RIGHT_ACCESS_STATE," +
                "start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT,author_right_exception,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE,$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                " $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules,basis_storage," +
                "basis_access_state,notes_process_documentation,notes_management_related," +
                "$COLUMN_IS_TEMPLATE,template_name,template_description,$COLUMN_EXCEPTION_FROM) " +
                "VALUES(?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?)"

        const val STATEMENT_UPSERT_RIGHT =
            "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
                "($COLUMN_RIGHT_ID,created_on,last_updated_on," +
                "created_by,last_updated_by,$COLUMN_RIGHT_ACCESS_STATE," +
                "start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT,author_right_exception,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE,$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules, basis_storage," +
                "basis_access_state,notes_process_documentation,notes_management_related," +
                "$COLUMN_IS_TEMPLATE,template_name,template_description,$COLUMN_EXCEPTION_FROM) " +
                "VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?)" +
                " ON CONFLICT ($COLUMN_RIGHT_ID)" +
                " DO UPDATE SET" +
                " last_updated_on = EXCLUDED.last_updated_on," +
                "last_updated_by = EXCLUDED.last_updated_by," +
                "$COLUMN_RIGHT_ACCESS_STATE = EXCLUDED.$COLUMN_RIGHT_ACCESS_STATE," +
                "start_date = EXCLUDED.start_date," +
                "end_date = EXCLUDED.end_date," +
                "notes_general = EXCLUDED.notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT = EXCLUDED.$COLUMN_RIGHT_LICENCE_CONTRACT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE = EXCLUDED.$COLUMN_RIGHT_OPEN_CONTENT_LICENCE ," +
                "$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL = EXCLUDED.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE = EXCLUDED.$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE = EXCLUDED.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_ZBW_USER_AGREEMENT = EXCLUDED.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "notes_formal_rules = EXCLUDED.notes_formal_rules," +
                "basis_storage = EXCLUDED.basis_storage," +
                "basis_access_state = EXCLUDED.basis_access_state," +
                "notes_process_documentation = EXCLUDED.notes_process_documentation," +
                "notes_management_related = EXCLUDED.notes_management_related," +
                "$COLUMN_IS_TEMPLATE = EXCLUDED.$COLUMN_IS_TEMPLATE," +
                "template_name = EXCLUDED.template_name," +
                "template_description = EXCLUDED.template_description," +
                "author_right_exception = EXCLUDED.author_right_exception," +
                "$COLUMN_EXCEPTION_FROM = EXCLUDED.$COLUMN_EXCEPTION_FROM;"

        const val STATEMENT_DELETE_RIGHTS =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_RIGHT r " +
                "WHERE r.$COLUMN_RIGHT_ID = ANY(?)"

        const val STATEMENT_GET_EXCEPTIONS_BY_RIGHT_ID =
            "SELECT $COLUMN_RIGHT_ID,created_on,last_updated_on,created_by," +
                "last_updated_by,$COLUMN_RIGHT_ACCESS_STATE,start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT,author_right_exception,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE,$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules,basis_storage," +
                "basis_access_state,notes_process_documentation,notes_management_related," +
                "$COLUMN_IS_TEMPLATE,template_name,template_description,last_applied_on,$COLUMN_EXCEPTION_FROM" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_EXCEPTION_FROM = ?"

        const val STATEMENT_GET_TEMPLATES =
            "SELECT $COLUMN_RIGHT_ID,created_on,last_updated_on,created_by," +
                "last_updated_by,$COLUMN_RIGHT_ACCESS_STATE,start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT, author_right_exception, $COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules, basis_storage," +
                "basis_access_state,notes_process_documentation, notes_management_related," +
                "$COLUMN_IS_TEMPLATE,template_name,template_description,last_applied_on,$COLUMN_EXCEPTION_FROM" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_IS_TEMPLATE = true" +
                " ORDER BY created_on DESC LIMIT ? OFFSET ?"

        const val STATEMENT_GET_RIGHTS_BY_TEMPLATE_NAME =
            "SELECT $COLUMN_RIGHT_ID,created_on,last_updated_on,created_by," +
                "last_updated_by,$COLUMN_RIGHT_ACCESS_STATE,start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT,author_right_exception,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE,$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL," +
                "$COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules,basis_storage," +
                "basis_access_state,notes_process_documentation,notes_management_related," +
                "$COLUMN_IS_TEMPLATE,template_name,template_description,last_applied_on,$COLUMN_EXCEPTION_FROM" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_TEMPLATE_NAME = ANY(?)"

        const val STATEMENT_UPDATE_TEMPLATE_APPLIED_ON =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET last_applied_on=?" +
                " WHERE $COLUMN_RIGHT_ID = ?"

        const val STATEMENT_IS_EXCEPTION =
            "SELECT COUNT(*)" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_ID = ? AND $COLUMN_EXCEPTION_FROM IS NOT NULL AND $COLUMN_IS_TEMPLATE"

        const val STATEMENT_UPDATE_TEMPLATE_EXCEPTION_FROM =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET $COLUMN_EXCEPTION_FROM=?" +
                " WHERE $COLUMN_RIGHT_ID=ANY(?)"

        fun extractRightFromRS(rs: ResultSet): ItemRight {
            val currentRightId = rs.getString(1)
            return ItemRight(
                rightId = currentRightId,
                createdOn =
                    rs.getTimestamp(2)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                lastUpdatedOn =
                    rs.getTimestamp(3)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                createdBy = rs.getString(4),
                lastUpdatedBy = rs.getString(5),
                accessState = rs.getString(6)?.let { AccessState.valueOf(it) },
                startDate = rs.getDate(7).toLocalDate(),
                endDate = rs.getDate(8)?.toLocalDate(),
                notesGeneral = rs.getString(9),
                licenceContract = rs.getString(10),
                authorRightException = rs.getBoolean(11),
                zbwUserAgreement = rs.getBoolean(12),
                openContentLicence = rs.getString(13),
                nonStandardOpenContentLicenceURL = rs.getString(14),
                nonStandardOpenContentLicence = rs.getBoolean(15),
                restrictedOpenContentLicence = rs.getBoolean(16),
                notesFormalRules = rs.getString(17),
                basisStorage = rs.getString(18)?.let { BasisStorage.valueOf(it) },
                basisAccessState = rs.getString(19)?.let { BasisAccessState.valueOf(it) },
                notesProcessDocumentation = rs.getString(20),
                notesManagementRelated = rs.getString(21),
                isTemplate = rs.getBoolean(22),
                templateName = rs.getString(23),
                templateDescription = rs.getString(24),
                lastAppliedOn =
                    rs.getTimestamp(25)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                exceptionFrom = rs.getString(26),
                groups = null,
                groupIds = null,
            )
        }
    }
}
