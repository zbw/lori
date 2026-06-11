package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.ItemRow
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ACCESS_STATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_LICENCE_CONTRACT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_TEMPLATE_NAME
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ZBW_USER_AGREEMENT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_RIGHT
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.ItemDB.Companion.COLUMN_ITEM_CREATED_BY
import de.zbw.persistence.lori.server.ItemDB.Companion.COLUMN_ITEM_CREATED_ON
import de.zbw.persistence.lori.server.ItemDB.Companion.COLUMN_ITEM_HANDLE
import de.zbw.persistence.lori.server.ItemDB.Companion.COLUMN_ITEM_LAST_UPDATED_BY
import de.zbw.persistence.lori.server.ItemDB.Companion.COLUMN_ITEM_LAST_UPDATED_ON
import de.zbw.persistence.lori.server.ItemDB.Companion.COLUMN_ITEM_RIGHT_ID
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_HANDLE
import io.opentelemetry.api.trace.Tracer
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Calendar
import java.util.TimeZone

/**
 * Execute SQL queries strongly related to rights.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class RightDB(
    connectionPool: ConnectionPool,
    batchConnectionPool: ConnectionPool,
    tracer: Tracer,
    private val groupDB: GroupDB,
) : AbstractDB(
        connectionPool,
        batchConnectionPool,
        tracer,
        TABLE_NAME_ITEM_RIGHT,
    ) {
    suspend fun insertRight(
        right: ItemRight,
        isBatchJob: Boolean = false,
    ): String =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_INSERT_RIGHT,
                tracer = tracer,
                spanName = "insertRight",
                params = { stmt ->
                    insertRightSetParameters(
                        right,
                        stmt,
                    )
                },
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
            ).first()

    suspend fun upsertRight(
        right: ItemRight,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_UPSERT_RIGHT,
            tracer = tracer,
            spanName = "upsertRight",
            params = { stmt ->
                upsertRightSetParameters(
                    right,
                    stmt,
                )
            },
        )

    private fun upsertRightSetParameters(
        right: ItemRight,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()
        var localCounter = 1
        return prep.apply {
            this.setString(localCounter++, right.rightId)
            this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
            this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
            this.setIfNotNull(localCounter++, right.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(localCounter++, right.startDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(localCounter++, right.endDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(localCounter++, right.notesGeneral) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.licenceContract) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.zbwUserAgreement) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.restrictedOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.notesFormalRules) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.basisStorage) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(localCounter++, right.basisAccessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(localCounter++, right.notesProcessDocumentation) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.notesManagementRelated) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.isTemplate) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.templateName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.templateDescription) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.exceptionOfId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.hasLegalRisk) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.hasExceptionId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.predecessorId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.successorId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    private fun insertRightSetParameters(
        right: ItemRight,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()
        var localCounter = 1
        return prep.apply {
            this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
            this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
            this.setIfNotNull(localCounter++, right.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.accessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(localCounter++, right.startDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(localCounter++, right.endDate) { value, idx, prepStmt ->
                prepStmt.setDate(idx, Date.valueOf(value))
            }
            this.setIfNotNull(localCounter++, right.notesGeneral) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.licenceContract) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.zbwUserAgreement) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.restrictedOpenContentLicence) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.notesFormalRules) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.basisStorage) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(localCounter++, right.basisAccessState) { value, idx, prepStmt ->
                prepStmt.setString(idx, value.toString())
            }
            this.setIfNotNull(localCounter++, right.notesProcessDocumentation) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.notesManagementRelated) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.isTemplate) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.templateName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.templateDescription) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.exceptionOfId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.hasLegalRisk) { value, idx, prepStmt ->
                prepStmt.setBoolean(idx, value)
            }
            this.setIfNotNull(localCounter++, right.hasExceptionId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.predecessorId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(localCounter++, right.successorId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    suspend fun deleteRightsByIds(
        rightIds: List<String>,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_RIGHTS,
            tracer = tracer,
            spanName = "deleteRightsByIds",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", rightIds.toTypedArray()))
            },
        )

    suspend fun getRightsByIds(
        rightsIds: List<String>,
        isBatchJob: Boolean = false,
    ): List<ItemRight> =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_RIGHTS,
                tracer = tracer,
                spanName = "getRightsByIds",
                params = { stmt ->
                    stmt.setArray(1, stmt.connection.createArrayOf("text", rightsIds.toTypedArray()))
                },
                mapper = { rs ->
                    extractRightFromRS(rs)
                },
            ).let { rights ->
                addGroupInformationToRights(rights)
            }

    suspend fun rightContainsId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): Boolean =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_RIGHT_CONTAINS_ID,
                tracer = tracer,
                spanName = "rightContainsId",
                params = { stmt ->
                    stmt.setString(1, rightId)
                },
                mapper = { rs ->
                    rs.getBoolean(1)
                },
            ).first()

    suspend fun getItemRowsByHandle(
        handle: String,
        isBatchJob: Boolean = false,
    ): List<ItemRow> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_GET_RIGHTS_IDS_FOR_METADATA,
            tracer = tracer,
            spanName = "getItemRowsByHandle",
            params = { stmt ->
                stmt.setString(1, handle)
            },
            mapper = { rs ->
                ItemRow(
                    rightId = rs.getString(1),
                    handle = rs.getString(2),
                    createdBy = rs.getString(3),
                    createdOn =
                        rs.getTimestamp(4, utcCalendar)?.let {
                            OffsetDateTime.ofInstant(
                                it.toInstant(),
                                TimezoneUtil.TIME_ZONE_UTC,
                            )
                        },
                    lastUpdatedBy = rs.getString(5),
                    lastUpdatedOn =
                        rs.getTimestamp(6, utcCalendar)?.let {
                            OffsetDateTime.ofInstant(
                                it.toInstant(),
                                TimezoneUtil.TIME_ZONE_UTC,
                            )
                        },
                )
            },
        )

    suspend fun getTemplateList(
        limit: Int,
        offset: Int,
        draftFilter: Boolean? = null,
        exceptionFilter: Boolean? = null,
        excludes: List<String>? = null,
        hasException: Boolean? = null,
        isBatchJob: Boolean = false,
    ): List<ItemRight> {
        val sql =
            STATEMENT_GET_TEMPLATES
                .let {
                    if (draftFilter == null) {
                        it
                    } else if (draftFilter) {
                        "$it AND $COLUMN_RIGHT_LAST_APPLIED_ON IS NULL"
                    } else {
                        "$it AND $COLUMN_RIGHT_LAST_APPLIED_ON IS NOT NULL"
                    }
                }.let {
                    if (exceptionFilter == null) {
                        it
                    } else if (exceptionFilter) {
                        "$it AND $COLUMN_RIGHT_EXCEPTION_OF_ID IS NOT NULL"
                    } else {
                        "$it AND $COLUMN_RIGHT_EXCEPTION_OF_ID IS NULL"
                    }
                }.let {
                    if (excludes != null) {
                        "$it AND NOT $COLUMN_RIGHT_ID = ANY(?)"
                    } else {
                        it
                    }
                }.let {
                    if (hasException == null) {
                        it
                    } else if (hasException) {
                        "$it AND $COLUMN_RIGHT_HAS_EXCEPTION_ID IS NOT NULL"
                    } else {
                        "$it AND $COLUMN_RIGHT_HAS_EXCEPTION_ID IS NULL"
                    }
                }.let {
                    "$it ORDER BY created_on DESC LIMIT ? OFFSET ?;"
                }
        return DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = sql,
                tracer = tracer,
                spanName = "getTemplateList",
                params = { stmt ->
                    var parameterCounter = 1
                    if (excludes != null) {
                        stmt.setArray(
                            parameterCounter++,
                            stmt.connection.createArrayOf("text", excludes.toTypedArray()),
                        )
                    }
                    stmt.setInt(parameterCounter++, limit)
                    stmt.setInt(parameterCounter++, offset)
                },
                mapper = { rs ->
                    extractRightFromRS(rs)
                },
            ).let { rights ->
                addGroupInformationToRights(rights)
            }
    }

    /**
     * Get all RightIds for all templates.
     */
    suspend fun getRightIdsForAllTemplates(): List<String> =
        DatabaseConnector
            .select(
                connectionPool = connectionPool,
                sql = STATEMENT_GET_ALL_IDS_OF_TEMPLATES,
                tracer = tracer,
                spanName = "getRightIdsForAllTemplates",
                mapper = { rs ->
                    rs.getString(1)
                },
            )

    suspend fun updateAppliedOnByTemplateId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_UPDATE_TEMPLATE_APPLIED_ON,
            tracer = tracer,
            spanName = "updateTemplateById",
            params = { stmt ->
                val now = Instant.now()
                stmt.setTimestamp(1, Timestamp.from(now), utcCalendar) // last_applied_on
                stmt.setTimestamp(2, Timestamp.from(now), utcCalendar) // first_applied_on
                stmt.setString(3, rightId)
            },
        )

    suspend fun getRightsByTemplateNames(
        templateNames: List<String>,
        isBatchJob: Boolean = false,
    ): List<ItemRight> =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_RIGHTS_BY_TEMPLATE_NAME,
                tracer = tracer,
                spanName = "getRightsByTemplateNames",
                params = { stmt ->
                    stmt.setArray(1, stmt.connection.createArrayOf("varchar", templateNames.toTypedArray()))
                },
                mapper = { rs ->
                    extractRightFromRS(rs)
                },
            ).let { rights ->
                addGroupInformationToRights(rights)
            }

    /**
     * Return all Templates that are an exception for the given rightId.
     */
    suspend fun getExceptionByRightId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): ItemRight? =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_EXCEPTIONS_BY_RIGHT_ID,
                tracer = tracer,
                spanName = "getExceptionByRightId",
                params = { stmt ->
                    stmt.setString(1, rightId)
                },
                mapper = { rs ->
                    extractRightFromRS(rs)
                },
            ).let { rights ->
                return rights.firstOrNull()?.let { r ->
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
    suspend fun isException(
        rightId: String,
        isBatchJob: Boolean = false,
    ): Boolean =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_IS_EXCEPTION,
                tracer = tracer,
                spanName = "isException",
                params = { stmt ->
                    stmt.setString(1, rightId)
                },
                mapper = { rs ->
                    rs.getInt(1) == 1
                },
            ).first()

    /**
     * Connects an exception with a template.
     */
    suspend fun addExceptionToTemplate(
        rightIdTemplate: String,
        rightIdException: String,
        isBatchJob: Boolean = false,
    ): Int {
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_SET_EXCEPTION_OF_ID,
            tracer = tracer,
            spanName = "addTemplateToException",
            params = { stmt ->
                stmt.setString(1, rightIdTemplate)
                stmt.setString(2, rightIdException)
            },
        )
        return DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_SET_HAS_EXCEPTION_ID,
            tracer = tracer,
            spanName = "addExceptionToTemplate",
            params = { stmt ->
                stmt.setString(1, rightIdException)
                stmt.setString(2, rightIdTemplate)
            },
        )
    }

    suspend fun removeExceptionTemplateConnection(
        rightIdTemplate: String,
        rightIdException: String,
        isBatchJob: Boolean = false,
    ): Int {
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_SET_EXCEPTION_OF_ID,
            tracer = tracer,
            spanName = "deleteExceptionTemplateConnection",
            params = { stmt ->
                stmt.setString(1, null)
                stmt.setString(2, rightIdException)
            },
        )

        return DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_SET_HAS_EXCEPTION_ID,
            tracer = tracer,
            spanName = "addExceptionToTemplate",
            params = { stmt ->
                stmt.setString(1, null)
                stmt.setString(2, rightIdTemplate)
            },
        )
    }

    suspend fun setPredecessor(
        sourceRightId: String,
        targetRightId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_SET_PREDECESSOR,
            tracer = tracer,
            spanName = "addPredecessor",
            params = { stmt ->
                stmt.setString(1, targetRightId)
                stmt.setString(2, sourceRightId)
            },
        )

    suspend fun setSuccessor(
        sourceRightId: String,
        targetRightId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_SET_SUCCESSOR,
            tracer = tracer,
            spanName = "addSuccessor",
            params = { stmt ->
                stmt.setString(1, targetRightId)
                stmt.setString(2, sourceRightId)
            },
        )

    suspend fun addGroupInformationToRights(
        rights: List<ItemRight>,
        isBatchJob: Boolean = false,
    ): List<ItemRight> {
        val rightToGroups: Map<String, List<Group>> =
            groupDB.getGroupsByRightIds(
                rights.map { it.rightId!! },
                isBatchJob,
            )
        return rights.map { r ->
            r.copy(
                groups = rightToGroups[r.rightId] ?: emptyList(),
                groupIds = rightToGroups[r.rightId]?.map { it.groupId } ?: emptyList(),
            )
        }
    }

    companion object {
        const val COLUMN_RIGHT_IS_TEMPLATE = "is_template"
        private const val COLUMN_RIGHT_EXCEPTION_OF_ID = "exception_of_id"
        private const val COLUMN_RIGHT_FIRST_APPLIED_ON = "first_applied_on"
        private const val COLUMN_RIGHT_HAS_EXCEPTION_ID = "has_exception_id"
        const val COLUMN_RIGHT_HAS_LEGAL_RISK = "has_legal_risk"
        private const val COLUMN_RIGHT_LAST_APPLIED_ON = "last_applied_on"
        private const val COLUMN_RIGHT_PREDECESSOR_ID = "predecessor_id"
        private const val COLUMN_RIGHT_SUCCESSOR_ID = "successor_id"

        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone(TimezoneUtil.TIME_ZONE_UTC))

        const val STATEMENT_SELECT_ALL =
            "SELECT $COLUMN_RIGHT_ID,created_on,last_updated_on,created_by," +
                "last_updated_by,$COLUMN_RIGHT_ACCESS_STATE,start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules, basis_storage," +
                "basis_access_state,notes_process_documentation, notes_management_related," +
                "$COLUMN_RIGHT_IS_TEMPLATE,template_name,template_description,$COLUMN_RIGHT_LAST_APPLIED_ON," +
                "$COLUMN_RIGHT_EXCEPTION_OF_ID,$COLUMN_RIGHT_HAS_LEGAL_RISK,$COLUMN_RIGHT_HAS_EXCEPTION_ID," +
                "$COLUMN_RIGHT_PREDECESSOR_ID,$COLUMN_RIGHT_SUCCESSOR_ID,$COLUMN_RIGHT_FIRST_APPLIED_ON"

        const val STATEMENT_GET_ALL_IDS_OF_TEMPLATES =
            "SELECT $COLUMN_RIGHT_ID" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_IS_TEMPLATE = true"

        const val STATEMENT_GET_RIGHTS =
            STATEMENT_SELECT_ALL +
                " FROM $TABLE_NAME_ITEM_RIGHT " +
                " WHERE $COLUMN_RIGHT_ID = ANY(?)"

        const val STATEMENT_GET_RIGHTS_IDS_FOR_METADATA =
            "SELECT $COLUMN_ITEM_RIGHT_ID,$COLUMN_ITEM_HANDLE,$COLUMN_ITEM_CREATED_BY," +
                "$COLUMN_ITEM_CREATED_ON,$COLUMN_ITEM_LAST_UPDATED_BY,$COLUMN_ITEM_LAST_UPDATED_ON" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE $COLUMN_METADATA_HANDLE = ?"

        const val STATEMENT_RIGHT_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_RIGHT WHERE right_id=?)"

        const val STATEMENT_INSERT_RIGHT =
            "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
                "(created_on,last_updated_on," +
                "created_by,last_updated_by,$COLUMN_RIGHT_ACCESS_STATE," +
                "start_date,end_date,notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules,basis_storage," +
                "basis_access_state,notes_process_documentation,notes_management_related," +
                "$COLUMN_RIGHT_IS_TEMPLATE,template_name,template_description,$COLUMN_RIGHT_EXCEPTION_OF_ID," +
                "$COLUMN_RIGHT_HAS_LEGAL_RISK,$COLUMN_RIGHT_HAS_EXCEPTION_ID,$COLUMN_RIGHT_PREDECESSOR_ID," +
                "$COLUMN_RIGHT_SUCCESSOR_ID) " +
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
                "$COLUMN_RIGHT_LICENCE_CONTRACT,$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE,notes_formal_rules, basis_storage," +
                "basis_access_state,notes_process_documentation,notes_management_related," +
                "$COLUMN_RIGHT_IS_TEMPLATE,template_name,template_description,$COLUMN_RIGHT_EXCEPTION_OF_ID," +
                "$COLUMN_RIGHT_HAS_LEGAL_RISK,$COLUMN_RIGHT_HAS_EXCEPTION_ID,$COLUMN_RIGHT_PREDECESSOR_ID," +
                "$COLUMN_RIGHT_SUCCESSOR_ID) " +
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
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE = EXCLUDED.$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_ZBW_USER_AGREEMENT = EXCLUDED.$COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "notes_formal_rules = EXCLUDED.notes_formal_rules," +
                "basis_storage = EXCLUDED.basis_storage," +
                "basis_access_state = EXCLUDED.basis_access_state," +
                "notes_process_documentation = EXCLUDED.notes_process_documentation," +
                "notes_management_related = EXCLUDED.notes_management_related," +
                "$COLUMN_RIGHT_IS_TEMPLATE = EXCLUDED.$COLUMN_RIGHT_IS_TEMPLATE," +
                "template_name = EXCLUDED.template_name," +
                "template_description = EXCLUDED.template_description," +
                "$COLUMN_RIGHT_EXCEPTION_OF_ID = EXCLUDED.$COLUMN_RIGHT_EXCEPTION_OF_ID," +
                "$COLUMN_RIGHT_HAS_EXCEPTION_ID = EXCLUDED.$COLUMN_RIGHT_HAS_EXCEPTION_ID," +
                "$COLUMN_RIGHT_HAS_LEGAL_RISK = EXCLUDED.$COLUMN_RIGHT_HAS_LEGAL_RISK," +
                "$COLUMN_RIGHT_PREDECESSOR_ID = EXCLUDED.$COLUMN_RIGHT_PREDECESSOR_ID," +
                "$COLUMN_RIGHT_SUCCESSOR_ID = EXCLUDED.$COLUMN_RIGHT_SUCCESSOR_ID;"

        const val STATEMENT_DELETE_RIGHTS =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_RIGHT r " +
                "WHERE r.$COLUMN_RIGHT_ID = ANY(?)"

        const val STATEMENT_GET_EXCEPTIONS_BY_RIGHT_ID =
            STATEMENT_SELECT_ALL +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_EXCEPTION_OF_ID = ?"

        const val STATEMENT_GET_TEMPLATES =
            STATEMENT_SELECT_ALL +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_IS_TEMPLATE = true"

        const val STATEMENT_GET_RIGHTS_BY_TEMPLATE_NAME =
            STATEMENT_SELECT_ALL +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_TEMPLATE_NAME = ANY(?)"

        const val STATEMENT_UPDATE_TEMPLATE_APPLIED_ON =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET $COLUMN_RIGHT_LAST_APPLIED_ON=?," +
                " $COLUMN_RIGHT_FIRST_APPLIED_ON = CASE" +
                " WHEN $COLUMN_RIGHT_FIRST_APPLIED_ON IS NULL THEN ?" +
                " ELSE $COLUMN_RIGHT_FIRST_APPLIED_ON" +
                " END" +
                " WHERE $COLUMN_RIGHT_ID = ?"

        const val STATEMENT_IS_EXCEPTION =
            "SELECT COUNT(*)" +
                " FROM $TABLE_NAME_ITEM_RIGHT" +
                " WHERE $COLUMN_RIGHT_ID = ? AND $COLUMN_RIGHT_EXCEPTION_OF_ID IS NOT NULL AND $COLUMN_RIGHT_IS_TEMPLATE;"

        const val STATEMENT_SET_EXCEPTION_OF_ID =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET $COLUMN_RIGHT_EXCEPTION_OF_ID=?" +
                " WHERE $COLUMN_RIGHT_ID=?;"

        const val STATEMENT_SET_HAS_EXCEPTION_ID =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET $COLUMN_RIGHT_HAS_EXCEPTION_ID=?" +
                " WHERE $COLUMN_RIGHT_ID=?;"

        const val STATEMENT_SET_PREDECESSOR =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET $COLUMN_RIGHT_PREDECESSOR_ID=?" +
                " WHERE $COLUMN_RIGHT_ID=?;"

        const val STATEMENT_SET_SUCCESSOR =
            "UPDATE $TABLE_NAME_ITEM_RIGHT" +
                " SET $COLUMN_RIGHT_SUCCESSOR_ID=?" +
                " WHERE $COLUMN_RIGHT_ID=?;"

        fun extractRightFromRS(rs: ResultSet): ItemRight {
            var localCounter = 1
            val currentRightId = rs.getString(localCounter++)
            return ItemRight(
                rightId = currentRightId,
                createdOn =
                    rs.getTimestamp(localCounter++, utcCalendar)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            TimezoneUtil.TIME_ZONE_UTC,
                        )
                    },
                lastUpdatedOn =
                    rs.getTimestamp(localCounter++, utcCalendar)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            TimezoneUtil.TIME_ZONE_UTC,
                        )
                    },
                createdBy = rs.getString(localCounter++),
                lastUpdatedBy = rs.getString(localCounter++),
                accessState = rs.getString(localCounter++)?.let { AccessState.valueOf(it) },
                startDate = rs.getDate(localCounter++).toLocalDate(),
                endDate = rs.getDate(localCounter++)?.toLocalDate(),
                notesGeneral = rs.getString(localCounter++),
                licenceContract = rs.getString(localCounter++),
                zbwUserAgreement = rs.getBoolean(localCounter++),
                restrictedOpenContentLicence = rs.getBoolean(localCounter++),
                notesFormalRules = rs.getString(localCounter++),
                basisStorage = rs.getString(localCounter++)?.let { BasisStorage.valueOf(it) },
                basisAccessState = rs.getString(localCounter++)?.let { BasisAccessState.valueOf(it) },
                notesProcessDocumentation = rs.getString(localCounter++),
                notesManagementRelated = rs.getString(localCounter++),
                isTemplate = rs.getBoolean(localCounter++),
                templateName = rs.getString(localCounter++),
                templateDescription = rs.getString(localCounter++),
                lastAppliedOn =
                    rs.getTimestamp(localCounter++, utcCalendar)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            TimezoneUtil.TIME_ZONE_UTC,
                        )
                    },
                exceptionOfId = rs.getString(localCounter++),
                hasLegalRisk = rs.getObject(localCounter++) as? Boolean, // Retrieving NULL from booleans is tricky
                hasExceptionId = rs.getString(localCounter++),
                predecessorId = rs.getString(localCounter++),
                successorId = rs.getString(localCounter++),
                firstAppliedOn =
                    rs.getTimestamp(localCounter++)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            TimezoneUtil.TIME_ZONE_UTC,
                        )
                    },
                groups = null,
                groupIds = null,
            )
        }
    }
}
