package de.zbw.persistence.lori.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.SearchKey
import de.zbw.business.lori.server.type.AccessState
import de.zbw.business.lori.server.type.BasisAccessState
import de.zbw.business.lori.server.type.BasisStorage
import de.zbw.business.lori.server.type.Group
import de.zbw.business.lori.server.type.GroupIpAddress
import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.User
import de.zbw.business.lori.server.type.UserRole
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import org.postgresql.util.PGobject
import java.lang.reflect.Type
import java.sql.Connection
import java.sql.Date
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
    private val gson: Gson,
) {
    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword),
        tracer,
        Gson().newBuilder().create(),
    )

    init {
        connection.autoCommit = false
        connection
            .prepareStatement("create EXTENSION IF NOT EXISTS \"pg_trgm\"")
            .execute()
        connection.commit()
    }

    /**
     * ITEM related queries.
     */
    fun itemContainsEntry(metadataId: String, rightId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_ITEM_CONTAINS_ENTRY).apply {
            this.setString(1, metadataId)
            this.setString(2, rightId)
        }
        val span = tracer.spanBuilder("itemContainsEntry").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun itemContainsRight(rightId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_ITEM_CONTAINS_RIGHT).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("itemContainsRight").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun insertItem(metadataId: String, rightId: String): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, metadataId)
            this.setString(2, rightId)
        }

        val span = tracer.spanBuilder("insertItem").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun deleteItem(
        metadataId: String,
        rightId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ITEM).apply {
            this.setString(1, rightId)
            this.setString(2, metadataId)
        }
        val span = tracer.spanBuilder("deleteItem").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun countItemByRightId(rightId: String): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_COUNT_ITEM_BY_RIGHTID).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("countItemByRightId").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }
        if (rs.next()) {
            return rs.getInt(1)
        } else throw IllegalStateException("No count found.")
    }

    fun deleteItemByMetadata(
        metadataId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("deleteItem").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun deleteItemByRight(
        rightId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_RIGHT).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("deleteItem").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    /**
     * GROUP RELATED QUERIES.
     */
    /**
     * Insert an entry into RIGHT_GROUP table.
     */
    fun insertGroup(group: Group): String {
        val prepStmt: PreparedStatement =
            connection.prepareStatement(STATEMENT_INSERT_GROUP, Statement.RETURN_GENERATED_KEYS)
                .apply {
                    this.setString(1, group.name)
                    this.setIfNotNull(2, group.description) { value, idx, prepStmt ->
                        prepStmt.setString(idx, value)
                    }
                    val jsonObj = PGobject()
                    jsonObj.type = "json"
                    jsonObj.value = gson.toJson(group.ipAddresses)
                    this.setObject(3, jsonObj)
                }
        val span = tracer.spanBuilder("insertGroup").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun getGroupById(groupId: String): Group? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_GROUP_BY_ID).apply {
            this.setString(1, groupId)
        }

        val span = tracer.spanBuilder("getGroupById").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        return if (rs.next()) {
            val groupListType: Type = object : TypeToken<ArrayList<GroupIpAddress>>() {}.type
            val name = rs.getString(1)
            val description = rs.getString(2)
            val ipAddressJson: String? = rs
                .getObject(3, PGobject::class.java)
                .value
            Group(
                name = name,
                description = description,
                ipAddresses = ipAddressJson
                    ?.let { gson.fromJson(it, groupListType) }
                    ?: emptyList()
            )
        } else {
            null
        }
    }

    fun deleteGroupById(
        groupId: String,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_GROUP_BY_ID).apply {
            this.setString(1, groupId)
        }
        val span = tracer.spanBuilder("deleteGroup").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun updateGroup(
        group: Group,
    ): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_UPDATE_GROUP).apply {
            this.setIfNotNull(1, group.description) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            val jsonObj = PGobject()
            jsonObj.type = "json"
            jsonObj.value = gson.toJson(group.ipAddresses)
            this.setObject(2, jsonObj)
            this.setString(3, group.name)
        }
        val span = tracer.spanBuilder("updateGroup").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    /**
     * Metadata RELATED QUERIES
     */
    fun deleteMetadata(metadataIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_METADATA).apply {
            this.setArray(1, connection.createArrayOf("text", metadataIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteMetadata").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun metadataContainsId(metadataId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_METADATA_CONTAINS_ID).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("metadataContainsId").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun itemContainsMetadata(metadataId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_ITEM_CONTAINS_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("itemContainsMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getMetadata(metadataIds: List<String>): List<ItemMetadata> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_METADATA).apply {
            this.setArray(1, connection.createArrayOf("text", metadataIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getMetadata").startSpan()
        return runMetadataStatement(prepStmt, span)
    }

    fun upsertMetadataBatch(itemMetadatas: List<ItemMetadata>): IntArray {
        val prep = connection.prepareStatement(STATEMENT_UPSERT_METADATA)
        itemMetadatas.map {
            val p = insertUpsertMetadataSetParameters(it, prep)
            p.addBatch()
        }
        val span = tracer.spanBuilder("upsertMetadataBatch").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prep.executeBatch() }
        } finally {
            span.end()
        }
    }

    fun insertMetadata(itemMetadata: ItemMetadata): String {
        val prepStmt = insertUpsertMetadataSetParameters(
            itemMetadata,
            connection.prepareStatement(STATEMENT_INSERT_METADATA, Statement.RETURN_GENERATED_KEYS),
        )

        val span = tracer.spanBuilder("insertMetadata").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    private fun insertUpsertMetadataSetParameters(
        itemMetadata: ItemMetadata,
        prep: PreparedStatement,
    ): PreparedStatement {
        val now = Instant.now()
        return prep.apply {
            this.setString(1, itemMetadata.metadataId)
            this.setString(2, itemMetadata.handle)
            this.setIfNotNull(3, itemMetadata.ppn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(4, itemMetadata.title)
            this.setIfNotNull(5, itemMetadata.titleJournal) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(6, itemMetadata.titleSeries) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setDate(7, Date.valueOf(itemMetadata.publicationDate))
            this.setIfNotNull(8, itemMetadata.band) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(9, itemMetadata.publicationType.toString())
            this.setIfNotNull(10, itemMetadata.doi) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(11, itemMetadata.isbn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(12, itemMetadata.rightsK10plus) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(13, itemMetadata.paketSigel) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, itemMetadata.zdbId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(15, itemMetadata.issn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setTimestamp(16, Timestamp.from(now))
            this.setTimestamp(17, Timestamp.from(now))
            this.setIfNotNull(18, itemMetadata.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(19, itemMetadata.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(20, itemMetadata.author) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(21, itemMetadata.collectionName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(22, itemMetadata.communityName) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(23, itemMetadata.storageDate) { value, idx, prepStmt ->
                prepStmt.setTimestamp(idx, Timestamp.from(value.toInstant()))
            }
        }
    }

    fun getMetadataRange(
        limit: Int,
        offset: Int,
    ): List<ItemMetadata> {
        val prepStmt: PreparedStatement = connection.prepareStatement(
            STATEMENT_SELECT_ALL_METADATA_FROM +
                " ORDER BY metadata_id ASC LIMIT ? OFFSET ?;"
        ).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }
        val span: Span = tracer.spanBuilder("getMetadataRange").startSpan()
        return runMetadataStatement(prepStmt, span)
    }

    private fun runMetadataStatement(prepStmt: PreparedStatement, span: Span): List<ItemMetadata> {
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        return generateSequence {
            if (rs.next()) {
                extractMetadataRS(rs)
            } else null
        }.takeWhile { true }.toList()
    }

    /**
     * Right related queries.
     */
    fun insertRight(right: ItemRight): String {
        val prepStmt =
            insertRightSetParameters(
                right,
                connection.prepareStatement(STATEMENT_INSERT_RIGHT, Statement.RETURN_GENERATED_KEYS)
            )
        val span = tracer.spanBuilder("insertRight").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun upsertRight(right: ItemRight): Int {
        val prepStmt =
            upsertRightSetParameters(
                right,
                connection.prepareStatement(STATEMENT_UPSERT_RIGHT)
            )
        val span = tracer.spanBuilder("upsertRight").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
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
        }
    }

    fun deleteRights(rightIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_RIGHTS).apply {
            this.setArray(1, connection.createArrayOf("text", rightIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteRights").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun getRights(rightsIds: List<String>): List<ItemRight> {

        val prepStmt = connection.prepareStatement(STATEMENT_GET_RIGHTS).apply {
            this.setArray(1, connection.createArrayOf("text", rightsIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getRights").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                ItemRight(
                    rightId = rs.getString(1),
                    createdOn = rs.getTimestamp(2)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    lastUpdatedOn = rs.getTimestamp(3)?.let {
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
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun rightContainsId(rightId: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_RIGHT_CONTAINS_ID).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("rightContainsId").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getRightIdsByMetadata(metadataId: String): List<String> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_RIGHTSIDS_FOR_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("getRightIdsByMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                rs.getString(1)
            } else null
        }.takeWhile { true }.toList()
    }

    /**
     * USER related queries.
     */
    fun userTableContainsName(username: String): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_USER_CONTAINS_NAME).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("userContainsName").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun insertUser(user: User): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_USER, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, user.name)
            this.setString(2, user.passwordHash)
            this.setIfNotNull(3, user.role.toString()) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }

        val span = tracer.spanBuilder("insertUser").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getString(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun userExistsByNameAndPassword(
        username: String,
        hashedPassword: String
    ): Boolean {
        val prepStmt = connection.prepareStatement(STATEMENT_USER_CREDENTIALS_EXIST).apply {
            this.setString(1, username)
            this.setString(2, hashedPassword)
        }
        val span = tracer.spanBuilder("userExistByNameAndPassword").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getRoleByUsername(username: String): UserRole? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_ROLE_BY_USER).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("getRoleByUser").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return if (rs.next()) {
            UserRole.valueOf(rs.getString(1))
        } else null
    }

    fun updateUserNonRoleProperties(user: User): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_UPDATE_USER).apply {
            this.setString(1, user.passwordHash)
            this.setString(2, user.name)
        }
        val span = tracer.spanBuilder("updateUserNonRoleProperties").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun getUserByName(username: String): User? {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_USER_BY_NAME).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("getUserByName").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }
        return if (rs.next()) {
            User(
                name = rs.getString(1),
                passwordHash = rs.getString(2),
                role = UserRole.valueOf(
                    rs.getString(3),
                ),
            )
        } else null
    }

    fun deleteUser(username: String): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_USER).apply {
            this.setString(1, username)
        }
        val span = tracer.spanBuilder("deleteUser").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun updateUserRoleProperty(username: String, role: UserRole): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_UPDATE_USER_ROLE).apply {
            this.setString(1, role.toString())
            this.setString(2, username)
        }
        val span = tracer.spanBuilder("updateUserRole").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    /**
     * Search related queries.
     */
    fun countSearchMetadata(
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter> = emptyList(),
    ): Int {
        val entries = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildCountSearchQuery(searchTerms, metadataSearchFilter, rightSearchFilter)
        )
            .apply {
                var counter = 1
                entries.forEach { entry ->
                    this.setString(counter++, entry.value.joinToString(" "))
                }
                rightSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
                metadataSearchFilter.forEach { f ->
                    counter = f.setSQLParameter(counter, this)
                }
            }
        val span = tracer.spanBuilder("countMetadataSearch").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }
        if (rs.next()) {
            return rs.getInt(1)
        } else throw IllegalStateException("No count found.")
    }

    fun searchMetadata(
        searchTerms: Map<SearchKey, List<String>>,
        limit: Int,
        offset: Int,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
    ): List<ItemMetadata> {
        val entries: List<Map.Entry<SearchKey, List<String>>> = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildSearchQuery(
                searchTerms,
                metadataSearchFilter,
                rightSearchFilter,
            )
        ).apply {
            var counter = 1
            entries.forEach { entry ->
                this.setString(counter++, entry.value.joinToString(" "))
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            this.setInt(counter++, limit)
            this.setInt(counter, offset)
        }
        val span = tracer.spanBuilder("searchMetadataWithRightsFilter").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }
        return generateSequence {
            if (rs.next()) {
                extractMetadataRS(rs)
            } else null
        }.takeWhile { true }.toList()
    }

    fun searchMetadataWithRightFilterForZDBAndSigel(
        searchTerms: Map<SearchKey, List<String>>,
        metadataSearchFilter: List<MetadataSearchFilter>,
        rightSearchFilter: List<RightSearchFilter>,
    ): PaketSigelZDBIdPubTypeSet {
        val entries: List<Map.Entry<SearchKey, List<String>>> = searchTerms.entries.toList()
        val prepStmt = connection.prepareStatement(
            buildSearchQueryForFacets(
                searchTerms,
                metadataSearchFilter,
                rightSearchFilter,
                true,
            )
        ).apply {
            var counter = 1
            entries.forEach { entry ->
                this.setString(counter++, entry.value.joinToString(" "))
            }
            rightSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
            metadataSearchFilter.forEach { f ->
                counter = f.setSQLParameter(counter, this)
            }
        }
        val span = tracer.spanBuilder("searchMetadataWithRightsFilterForZDBAndSigel").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
        } finally {
            span.end()
        }

        val received: List<PaketSigelZDBIdPubType> = generateSequence {
            if (rs.next()) {
                PaketSigelZDBIdPubType(
                    accessState = rs.getString(1)?.let { AccessState.valueOf(it) },
                    paketSigel = rs.getString(2),
                    publicationType = PublicationType.valueOf(rs.getString(3)),
                    zdbId = rs.getString(4),
                )
            } else null
        }.takeWhile { true }.toList()
        return PaketSigelZDBIdPubTypeSet(
            accessState = received.mapNotNull { it.accessState }.toSet(),
            paketSigels = received.mapNotNull { it.paketSigel }.toSet(),
            publicationType = received.map { it.publicationType }.toSet(),
            zdbIds = received.mapNotNull { it.zdbId }.toSet(),
        )
    }

    private fun <T> PreparedStatement.setIfNotNull(
        idx: Int,
        element: T?,
        setter: (T, Int, PreparedStatement) -> Unit,
    ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)

    companion object {
        private const val TABLE_NAME_ITEM = "item"
        private const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        private const val TABLE_NAME_ITEM_RIGHT = "item_right"
        private const val TABLE_NAME_RIGHT_GROUP = "right_group"
        private const val TABLE_NAME_USERS = "users"

        const val COLUMN_METADATA_COLLECTION_NAME = "collection_name"
        const val COLUMN_METADATA_COMMUNITY_NAME = "community_name"
        const val COLUMN_METADATA_PAKET_SIGEL = "paket_sigel"
        const val COLUMN_METADATA_PUBLICATION_DATE = "publication_date"
        const val COLUMN_METADATA_PUBLICATION_TYPE = "publication_type"
        const val COLUMN_METADATA_ZDB_ID = "zdb_id"

        const val COLUMN_RIGHT_START_DATE = "start_date"
        const val COLUMN_RIGHT_END_DATE = "end_date"
        const val COLUMN_RIGHT_ACCESS_STATE = "access_state"
        const val COLUMN_RIGHT_LICENCE_CONTRACT = "licence_contract"
        const val COLUMN_RIGHT_ZBW_USER_AGREEMENT = "zbw_user_agreement"
        const val COLUMN_RIGHT_OPEN_CONTENT_LICENCE = "open_content_licence"
        const val COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE = "non_standard_open_content_licence"
        const val COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL = "non_standard_open_content_licence_url"
        const val COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE = "restricted_open_content_licence"

        const val STATEMENT_COUNT_METADATA = "SELECT COUNT(*) " +
            "FROM $TABLE_NAME_ITEM_METADATA"

        const val STATEMENT_COUNT_ITEM_BY_RIGHTID = "SELECT COUNT(*) " +
            "FROM $TABLE_NAME_ITEM " +
            "WHERE right_id = ?;"

        const val STATEMENT_INSERT_ITEM = "INSERT INTO $TABLE_NAME_ITEM" +
            "(metadata_id, right_id) " +
            "VALUES(?,?)"

        const val STATEMENT_UPSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,title,title_journal," +
            "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
            "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author,collection_name,community_name,storage_date) " +
            "VALUES(?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?) " +
            "ON CONFLICT (metadata_id) " +
            "DO UPDATE SET " +
            "handle = EXCLUDED.handle," +
            "ppn = EXCLUDED.ppn," +
            "title = EXCLUDED.title," +
            "title_journal = EXCLUDED.title_journal," +
            "title_series = EXCLUDED.title_series," +
            "$COLUMN_METADATA_PUBLICATION_DATE = EXCLUDED.$COLUMN_METADATA_PUBLICATION_DATE," +
            "band = EXCLUDED.band," +
            "$COLUMN_METADATA_PUBLICATION_TYPE = EXCLUDED.$COLUMN_METADATA_PUBLICATION_TYPE," +
            "doi = EXCLUDED.doi," +
            "isbn = EXCLUDED.isbn," +
            "rights_k10plus = EXCLUDED.rights_k10plus," +
            "$COLUMN_METADATA_PAKET_SIGEL = EXCLUDED.$COLUMN_METADATA_PAKET_SIGEL," +
            "$COLUMN_METADATA_ZDB_ID = EXCLUDED.$COLUMN_METADATA_ZDB_ID," +
            "issn = EXCLUDED.issn," +
            "last_updated_on = EXCLUDED.last_updated_on," +
            "last_updated_by = EXCLUDED.last_updated_by," +
            "author = EXCLUDED.author," +
            "collection_name = EXCLUDED.collection_name," +
            "community_name = EXCLUDED.community_name," +
            "storage_date = EXCLUDED.storage_date;"

        const val STATEMENT_INSERT_GROUP = "INSERT INTO $TABLE_NAME_RIGHT_GROUP" +
            " (name, description, ip_addresses)" +
            " VALUES(?,?,?)"

        const val STATEMENT_INSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,title,title_journal," +
            "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
            "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author,collection_name,community_name,storage_date) " +
            "VALUES(?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?)"

        const val STATEMENT_INSERT_RIGHT = "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
            "(created_on, last_updated_on," +
            "created_by, last_updated_by, $COLUMN_RIGHT_ACCESS_STATE," +
            "start_date, end_date, notes_general," +
            "$COLUMN_RIGHT_LICENCE_CONTRACT, author_right_exception, $COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
            "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
            "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE, notes_formal_rules, basis_storage," +
            "basis_access_state, notes_process_documentation, notes_management_related) " +
            "VALUES(?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?)"

        const val STATEMENT_INSERT_USER = "INSERT INTO $TABLE_NAME_USERS" +
            "(username, password, role) " +
            "VALUES(?,?,?::role_enum)"

        const val STATEMENT_UPSERT_RIGHT =
            "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
                "(right_id, created_on, last_updated_on," +
                "created_by, last_updated_by, $COLUMN_RIGHT_ACCESS_STATE," +
                "start_date, end_date, notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT, author_right_exception, $COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE, notes_formal_rules, basis_storage," +
                "basis_access_state, notes_process_documentation, notes_management_related) " +
                "VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?)" +
                " ON CONFLICT (right_id) " +
                "DO UPDATE SET " +
                "last_updated_on = EXCLUDED.last_updated_on," +
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
                "notes_management_related = EXCLUDED.notes_management_related;"

        const val STATEMENT_SELECT_ALL_METADATA_DISTINCT =
            "SELECT DISTINCT ON ($TABLE_NAME_ITEM_METADATA.metadata_id) $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,$TABLE_NAME_ITEM_RIGHT.$COLUMN_RIGHT_ACCESS_STATE"

        const val STATEMENT_SELECT_ALL_METADATA_FROM =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author, collection_name, community_name, storage_date " +
                "FROM $TABLE_NAME_ITEM_METADATA"

        const val STATEMENT_SELECT_ALL_METADATA =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date"

        private const val STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES =
            "SELECT metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "created_on,last_updated_on," +
                "created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date"

        private const val STATEMENT_SELECT_SIGEL_ZDB =
            "SELECT ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID"

        const val STATEMENT_GET_GROUP_BY_ID = "SELECT name, description, ip_addresses" +
            " FROM $TABLE_NAME_RIGHT_GROUP" +
            " WHERE name = ?"

        const val STATEMENT_GET_METADATA = STATEMENT_SELECT_ALL_METADATA_FROM +
            " WHERE metadata_id = ANY(?)"

        const val STATEMENT_DELETE_ITEM = "DELETE " +
            "FROM $TABLE_NAME_ITEM i " +
            "WHERE i.right_id = ? " +
            "AND i.metadata_id = ?"

        const val STATEMENT_DELETE_GROUP_BY_ID = "DELETE " +
            "FROM $TABLE_NAME_RIGHT_GROUP" +
            " WHERE name = ?"

        const val STATEMENT_DELETE_ITEM_BY_METADATA = "DELETE " +
            "FROM $TABLE_NAME_ITEM i " +
            "WHERE i.metadata_id = ?"

        const val STATEMENT_DELETE_ITEM_BY_RIGHT = "DELETE " +
            "FROM $TABLE_NAME_ITEM i " +
            "WHERE i.right_id = ?"

        const val STATEMENT_DELETE_RIGHTS = "DELETE " +
            "FROM $TABLE_NAME_ITEM_RIGHT r " +
            "WHERE r.right_id = ANY(?)"

        const val STATEMENT_DELETE_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.metadata_id = ANY(?)"

        const val STATEMENT_GET_RIGHTS =
            "SELECT right_id, created_on, last_updated_on, created_by," +
                "last_updated_by, $COLUMN_RIGHT_ACCESS_STATE, start_date, end_date, notes_general," +
                "$COLUMN_RIGHT_LICENCE_CONTRACT, author_right_exception, $COLUMN_RIGHT_ZBW_USER_AGREEMENT," +
                "$COLUMN_RIGHT_OPEN_CONTENT_LICENCE, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL, $COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE," +
                "$COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE, notes_formal_rules, basis_storage," +
                "basis_access_state, notes_process_documentation, notes_management_related " +
                "FROM $TABLE_NAME_ITEM_RIGHT " +
                "WHERE right_id = ANY(?)"

        const val STATEMENT_GET_METADATA_RANGE =
            "SELECT metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date " +
                "FROM $TABLE_NAME_ITEM_METADATA"

        const val STATEMENT_GET_RIGHTSIDS_FOR_METADATA = "SELECT right_id" +
            " FROM $TABLE_NAME_ITEM" +
            " WHERE metadata_id = ?"

        const val STATEMENT_METADATA_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE metadata_id=?)"

        const val STATEMENT_RIGHT_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_RIGHT WHERE right_id=?)"

        const val STATEMENT_ITEM_CONTAINS_METADATA =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE metadata_id=?)"

        const val STATEMENT_ITEM_CONTAINS_ENTRY =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE metadata_id=? AND right_id=?)"

        const val STATEMENT_ITEM_CONTAINS_RIGHT =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE right_id=?)"

        const val STATEMENT_USER_CONTAINS_NAME =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_USERS WHERE username=?)"

        const val STATEMENT_USER_CREDENTIALS_EXIST =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_USERS WHERE username=? AND password=?)"

        const val STATEMENT_GET_ROLE_BY_USER =
            "SELECT role FROM $TABLE_NAME_USERS WHERE username=?"

        const val STATEMENT_UPDATE_GROUP =
            "UPDATE $TABLE_NAME_RIGHT_GROUP SET description=?, ip_addresses=? WHERE name=?;"

        const val STATEMENT_UPDATE_USER =
            "UPDATE $TABLE_NAME_USERS SET password=? WHERE username=?"

        const val STATEMENT_UPDATE_USER_ROLE =
            "UPDATE $TABLE_NAME_USERS SET role=?::role_enum WHERE username=?"

        const val STATEMENT_GET_USER_BY_NAME =
            "SELECT username, password, role " +
                "FROM $TABLE_NAME_USERS " +
                "WHERE username=?"

        const val STATEMENT_DELETE_USER = "DELETE " +
            "FROM $TABLE_NAME_USERS i " +
            "WHERE i.username = ?"

        fun Timestamp.toOffsetDateTime(): OffsetDateTime =
            OffsetDateTime.ofInstant(
                this.toInstant(),
                ZoneId.of("UTC+00:00"),
            )

        fun <T> runInTransaction(connection: Connection, block: () -> T): T =
            try {
                block().also { connection.commit() }
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }

        fun buildSearchQuery(
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            withLimit: Boolean = true,
        ): String {
            val subquery = if (rightSearchFilters.isEmpty()) {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    )
            } else {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                    )
            }

            val limit = if (withLimit) {
                " LIMIT ? OFFSET ?"
            } else ""

            return if (searchKeyMap.isEmpty()) {
                "$subquery ORDER BY item_metadata.metadata_id ASC$limit"
            } else {
                val trgmWhere = searchKeyMap.entries.joinToString(separator = " AND ") { entry ->
                    entry.key.toWhereClause()
                }
                val coalesceScore = searchKeyMap.entries.joinToString(
                    prefix = "(",
                    postfix = ")",
                    separator = " + ",
                ) { entry ->
                    "coalesce(${SearchKey.SUBQUERY_NAME}.${entry.key.distColumnName},1)"
                } + "/${searchKeyMap.size} as score"
                "$STATEMENT_SELECT_ALL_METADATA_NO_PREFIXES,$coalesceScore" +
                    " FROM ($subquery) as ${SearchKey.SUBQUERY_NAME}" +
                    " WHERE $trgmWhere" +
                    " ORDER BY score" +
                    limit
            }
        }

        fun buildCountSearchQuery(
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
        ): String =
            "SELECT COUNT(*) FROM" +
                " (${
                buildSearchQuery(
                    searchKeyMap,
                    metadataSearchFilter,
                    rightSearchFilter,
                    false,
                )
                }) as foo"

        fun buildSearchQueryForFacets(
            searchKeyMap: Map<SearchKey, List<String>>,
            metadataSearchFilters: List<MetadataSearchFilter>,
            rightSearchFilters: List<RightSearchFilter>,
            collectFacets: Boolean,
        ): String {
            val subquery = if (rightSearchFilters.isEmpty() && !collectFacets) {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                    )
            } else {
                buildSearchQuerySelect(searchKeyMap, rightSearchFilters, collectFacets) +
                    " FROM $TABLE_NAME_ITEM_METADATA" +
                    buildSearchQueryHelper(
                        metadataSearchFilters,
                        rightSearchFilters,
                        collectFacets,
                    )
            }
            val trgmWhere = searchKeyMap.entries.joinToString(separator = " AND ") { entry ->
                entry.key.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?.let {
                    " WHERE $it"
                }
                ?: ""

            return STATEMENT_SELECT_SIGEL_ZDB +
                " FROM ($subquery) as ${SearchKey.SUBQUERY_NAME}" +
                trgmWhere +
                " GROUP BY" +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_RIGHT_ACCESS_STATE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PAKET_SIGEL," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_PUBLICATION_TYPE," +
                " ${SearchKey.SUBQUERY_NAME}.$COLUMN_METADATA_ZDB_ID;"
        }

        private fun buildSearchQueryHelper(
            searchFilter: List<MetadataSearchFilter>,
        ): String {

            val filter = searchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() }
                ?: ""
            return if (filter.isBlank()) {
                ""
            } else {
                " WHERE $filter"
            }
        }

        private fun buildSearchQueryHelper(
            metadataSearchFilter: List<MetadataSearchFilter>,
            rightSearchFilter: List<RightSearchFilter>,
            collectFacets: Boolean = false,
        ): String {
            val metadataFilters = metadataSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }.takeIf { it.isNotBlank() } ?: ""
            val rightFilters = rightSearchFilter.joinToString(separator = " AND ") { f ->
                f.toWhereClause()
            }
            val whereClause = if (metadataFilters.isBlank()) {
                ""
            } else {
                " WHERE $metadataFilters"
            }
            val extendedRightFilter = if (rightFilters.isBlank()) {
                rightFilters
            } else {
                " AND $rightFilters"
            }
            val joinItemRight = if (collectFacets && rightSearchFilter.isEmpty()) {
                "LEFT JOIN"
            } else {
                "JOIN"
            }
            return " LEFT JOIN $TABLE_NAME_ITEM" +
                " ON $TABLE_NAME_ITEM.metadata_id = $TABLE_NAME_ITEM_METADATA.metadata_id" +
                " $joinItemRight $TABLE_NAME_ITEM_RIGHT" +
                " ON $TABLE_NAME_ITEM.right_id = $TABLE_NAME_ITEM_RIGHT.right_id" +
                extendedRightFilter +
                whereClause
        }

        private fun buildSearchQuerySelect(
            searchKeyMap: Map<SearchKey, List<String>>,
            rightSearchFilters: List<RightSearchFilter>,
            forceRightTableJoin: Boolean = false,
        ): String {
            val trgmSelect = searchKeyMap.entries.joinToString(separator = ",") { entry ->
                entry.key.toSelectClause()
            }.takeIf { it.isNotBlank() }
                ?.let {
                    ",$it"
                } ?: ""
            return if (rightSearchFilters.isEmpty() && !forceRightTableJoin) {
                "$STATEMENT_SELECT_ALL_METADATA$trgmSelect"
            } else {
                "$STATEMENT_SELECT_ALL_METADATA_DISTINCT$trgmSelect"
            }
        }

        private fun extractMetadataRS(rs: ResultSet) = ItemMetadata(
            metadataId = rs.getString(1),
            handle = rs.getString(2),
            ppn = rs.getString(3),
            title = rs.getString(4),
            titleJournal = rs.getString(5),
            titleSeries = rs.getString(6),
            publicationDate = rs.getDate(7).toLocalDate(),
            band = rs.getString(8),
            publicationType = PublicationType.valueOf(rs.getString(9)),
            doi = rs.getString(10),
            isbn = rs.getString(11),
            rightsK10plus = rs.getString(12),
            paketSigel = rs.getString(13),
            zdbId = rs.getString(14),
            issn = rs.getString(15),
            createdOn = rs.getTimestamp(16)?.toOffsetDateTime(),
            lastUpdatedOn = rs.getTimestamp(17)?.toOffsetDateTime(),
            createdBy = rs.getString(18),
            lastUpdatedBy = rs.getString(19),
            author = rs.getString(20),
            collectionName = rs.getString(21),
            communityName = rs.getString(22),
            storageDate = rs.getTimestamp(23).toOffsetDateTime(),
        )
    }
}
