package de.zbw.persistence.lori.server

import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.AccessState
import de.zbw.business.lori.server.ItemMetadata
import de.zbw.business.lori.server.ItemRight
import de.zbw.business.lori.server.PublicationType
import io.opentelemetry.api.trace.Tracer
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
) {

    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword),
        tracer
    )

    fun insertItem(metadataId: String, rightId: String): String {
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, metadataId)
            this.setString(2, rightId)
        }

        val span = tracer.spanBuilder("insertItem").startSpan()
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
            this.setInt(8, itemMetadata.publicationYear)
            this.setIfNotNull(9, itemMetadata.band) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setString(10, itemMetadata.publicationType.toString())
            this.setIfNotNull(11, itemMetadata.doi) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(12, itemMetadata.serialNumber) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(13, itemMetadata.isbn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(14, itemMetadata.rightsK10plus) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(15, itemMetadata.paketSigel) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(16, itemMetadata.zbdId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(17, itemMetadata.issn) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setTimestamp(18, Timestamp.from(now))
            this.setTimestamp(19, Timestamp.from(now))
            this.setIfNotNull(20, itemMetadata.createdBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(21, itemMetadata.lastUpdatedBy) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    fun insertRight(right: ItemRight): String {
        val now = Instant.now()

        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_RIGHT, Statement.RETURN_GENERATED_KEYS).apply {
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
            this.setIfNotNull(9, right.licenseConditions) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(10, right.provenanceLicense) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
        val span = tracer.spanBuilder("insertAction").startSpan()
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

    fun getMetadata(metadataIds: List<String>): List<ItemMetadata> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_METADATA).apply {
            this.setArray(1, connection.createArrayOf("text", metadataIds.toTypedArray()))
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
                    publicationYear = rs.getInt(8),
                    band = rs.getString(9),
                    publicationType = PublicationType.valueOf(rs.getString(10)),
                    doi = rs.getString(11),
                    serialNumber = rs.getString(12),
                    isbn = rs.getString(13),
                    rightsK10plus = rs.getString(14),
                    paketSigel = rs.getString(15),
                    zbdId = rs.getString(16),
                    issn = rs.getString(17),
                    createdOn = rs.getTimestamp(18)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    lastUpdatedOn = rs.getTimestamp(19)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                    createdBy = rs.getString(20),
                    lastUpdatedBy = rs.getString(21),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    private fun deleteRights(rightIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_RIGHTS).apply {
            this.setArray(1, connection.createArrayOf("text", rightIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("deleteRights").startSpan()
        return try {
            span.makeCurrent()
            prepStmt.run { this.executeUpdate() }
        } finally {
            span.end()
        }
    }

    private fun deleteMetadata(headerIds: List<String>): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_METADATA).apply {
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

    fun getRights(rightsIds: List<String>): List<ItemRight> {

        val prepStmt = connection.prepareStatement(STATEMENT_GET_RIGHTS).apply {
            this.setArray(1, connection.createArrayOf("text", rightsIds.toTypedArray()))
        }

        val span = tracer.spanBuilder("getRights").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
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
                    licenseConditions = rs.getString(9),
                    provenanceLicense = rs.getString(10),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    fun containsMetadata(headerId: String): Boolean {
        val stmt = "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE metadata_id=?)"
        val prepStmt = connection.prepareStatement(stmt).apply {
            this.setString(1, headerId)
        }
        val span = tracer.spanBuilder("containsMetadata").startSpan()
        val rs = try {
            span.makeCurrent()
            prepStmt.executeQuery()
        } finally {
            span.end()
        }
        rs.next()
        return rs.getBoolean(1)
    }

    fun getMetadataRange(limit: Int, offset: Int): List<String> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_METADATA_RANGE).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }

        val span = tracer.spanBuilder("getMetadataRange").startSpan()
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

    fun getRightIdsByMetadata(metadataId: String): List<String> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_RIGHTSIDS_FOR_METADATA).apply {
            this.setString(1, metadataId)
        }
        val span = tracer.spanBuilder("getRightIdsByMetadata").startSpan()
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
        const val TABLE_NAME_ITEM = "item"
        const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        const val TABLE_NAME_ITEM_RIGHT = "item_right"

        const val STATEMENT_INSERT_ITEM = "INSERT INTO $TABLE_NAME_ITEM" +
            "(metadata_id, right_id) " +
            "VALUES(?,?)"

        const val STATEMENT_UPSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "created_on,last_updated_on,created_by,last_updated_by) " +
            "VALUES(?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?) " +
            "ON CONFLICT (metadata_id) " +
            "DO UPDATE SET " +
            "handle = EXCLUDED.handle," +
            "ppn = EXCLUDED.ppn," +
            "ppn_ebook = EXCLUDED.ppn_ebook," +
            "title = EXCLUDED.title," +
            "title_journal = EXCLUDED.title_journal," +
            "title_series = EXCLUDED.title_series," +
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
            "last_updated_on = EXCLUDED.last_updated_on," +
            "last_updated_by = EXCLUDED.last_updated_by;"

        const val STATEMENT_INSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "created_on,last_updated_on,created_by,last_updated_by) " +
            "VALUES(?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?,?,?,?," +
            "?,?,?)"

        const val STATEMENT_INSERT_RIGHT = "INSERT INTO $TABLE_NAME_ITEM_RIGHT" +
            "(right_id, created_on, last_updated_on," +
            "created_by, last_updated_by, access_state," +
            "start_date, end_date, license_conditions," +
            "provenance_license) " +
            "VALUES(?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?)"

        const val STATEMENT_GET_METADATA = "SELECT metadata_id,handle,ppn,ppn_ebook,title,title_journal," +
            "title_series,published_year,band,publication_type,doi," +
            "serial_number,isbn,rights_k10plus,paket_sigel,zbd_id,issn," +
            "created_on,last_updated_on,created_by,last_updated_by " +
            "FROM $TABLE_NAME_ITEM_METADATA " +
            "WHERE metadata_id = ANY(?)"

        const val STATEMENT_DELETE_RIGHTS = "DELETE " +
            "FROM $TABLE_NAME_ITEM_RIGHT a " +
            "WHERE a.action_id = ANY(?)"

        const val STATEMENT_DELETE_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.metadata_id = ANY(?)"

        const val STATEMENT_GET_RIGHTS =
            "SELECT right_id, created_on, last_updated_on, created_by," +
                "last_updated_by, access_state, start_date, end_date," +
                "license_conditions, provenance_license " +
                "FROM $TABLE_NAME_ITEM_RIGHT " +
                "WHERE right_id = ANY(?)"

        const val STATEMENT_GET_METADATA_RANGE =
            "SELECT metadata_id FROM $TABLE_NAME_ITEM_METADATA ORDER BY metadata_id ASC LIMIT ? OFFSET ?"

        const val STATEMENT_GET_RIGHTSIDS_FOR_METADATA = "SELECT right_id" +
            " FROM $TABLE_NAME_ITEM" +
            " WHERE metadata_id = ?"
    }
}
