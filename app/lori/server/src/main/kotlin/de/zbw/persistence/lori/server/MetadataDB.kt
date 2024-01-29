package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_DATE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_PUBLICATION_TYPE
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ZDB_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import java.sql.Array
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant

/**
 * Execute SQL queries strongly related to metadata.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class MetadataDB(
    val connection: Connection,
    private val tracer: Tracer,
) {
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
            this.setIfNotNull(24, itemMetadata.subCommunitiesHandles) { value, idx, prepStmt ->
                prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
            }
            this.setIfNotNull(25, itemMetadata.communityHandle) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(26, itemMetadata.collectionHandle) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
    }

    companion object {
        const val TS_COMMUNITY = "ts_community"
        const val TS_COMMUNITY_HANDLE = "ts_com_hdl"
        const val TS_COLLECTION = "ts_collection"
        const val TS_COLLECTION_HANDLE = "ts_col_hdl"
        const val TS_HANDLE = "ts_hdl"
        const val TS_METADATA_ID = "ts_metadata_id"
        const val TS_SIGEL = "ts_sigel"
        const val TS_SUBCOMMUNITY_HANDLE = "ts_subcom_hdl"
        const val TS_TITLE = "ts_title"
        const val TS_ZDB_ID = "ts_zdb_id"

        const val STATEMENT_METADATA_CONTAINS_ID =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE metadata_id=?)"

        const val STATEMENT_DELETE_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.metadata_id = ANY(?)"

        const val STATEMENT_SELECT_ALL_METADATA_FROM =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle" +
                " FROM $TABLE_NAME_ITEM_METADATA"

        const val STATEMENT_SELECT_ALL_METADATA =
            "SELECT $TABLE_NAME_ITEM_METADATA.metadata_id,handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,sub_communities_handles,community_handle,collection_handle," +
                "$TS_COMMUNITY,$TS_COLLECTION,$TS_SIGEL,$TS_TITLE,$TS_ZDB_ID,$TS_COLLECTION_HANDLE,$TS_COMMUNITY_HANDLE,$TS_SUBCOMMUNITY_HANDLE," +
                "$TS_HANDLE,$TS_METADATA_ID"

        const val STATEMENT_GET_METADATA = STATEMENT_SELECT_ALL_METADATA_FROM +
            " WHERE metadata_id = ANY(?)"

        const val STATEMENT_UPSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,title,title_journal," +
            "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
            "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author,collection_name,community_name,storage_date,sub_communities_handles," +
            "community_handle,collection_handle) " +
            "VALUES(" +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?) " +
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
            "storage_date = EXCLUDED.storage_date," +
            "sub_communities_handles = EXCLUDED.sub_communities_handles," +
            "community_handle = EXCLUDED.community_handle," +
            "collection_handle = EXCLUDED.collection_handle"

        const val STATEMENT_ITEM_CONTAINS_METADATA =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE metadata_id=?)"

        const val STATEMENT_INSERT_METADATA = "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
            "(metadata_id,handle,ppn,title,title_journal," +
            "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
            "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID,issn," +
            "created_on,last_updated_on,created_by,last_updated_by," +
            "author,collection_name,community_name,storage_date,sub_communities_handles," +
            "community_handle,collection_handle) " +
            "VALUES(" +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?,?,?,?,?," +
            "?)"

        fun extractMetadataRS(rs: ResultSet) = ItemMetadata(
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
            storageDate = rs.getTimestamp(23)?.toOffsetDateTime(),
            subCommunitiesHandles = (rs.getArray(24)?.array as? kotlin.Array<out Any?>)?.filterIsInstance<String>(),
            communityHandle = rs.getString(25),
            collectionHandle = rs.getString(26),
        )
    }
}
