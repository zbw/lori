package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
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
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    internal suspend fun deleteMetadata(handles: List<String>): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_METADATA).apply {
                    this.setArray(1, connection.createArrayOf("text", handles.toTypedArray()))
                }
            val span = tracer.spanBuilder("deleteMetadata").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun metadataContainsHandle(handle: String): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_METADATA_CONTAINS_HANDLE).apply {
                    this.setString(1, handle)
                }
            val span = tracer.spanBuilder("metadataContainsHandle").startSpan()
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

    suspend fun getMetadataRange(
        limit: Int,
        offset: Int,
    ): List<ItemMetadata> =
        connectionPool.useConnection { connection: Connection ->
            val prepStmt: PreparedStatement =
                connection
                    .prepareStatement(
                        STATEMENT_SELECT_ALL_METADATA_FROM +
                            " ORDER BY $COLUMN_METADATA_HANDLE ASC LIMIT ? OFFSET ?;",
                    ).apply {
                        this.setInt(1, limit)
                        this.setInt(2, offset)
                    }
            val span: Span = tracer.spanBuilder("getMetadataRange").startSpan()
            return@useConnection runMetadataStatement(prepStmt, span, connection)
        }

    private fun runMetadataStatement(
        prepStmt: PreparedStatement,
        span: Span,
        connection: Connection,
    ): List<ItemMetadata> {
        val rs =
            try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.executeQuery() }
            } finally {
                span.end()
            }

        return generateSequence {
            if (rs.next()) {
                extractMetadataRS(rs)
            } else {
                null
            }
        }.takeWhile { true }.toList()
    }

    suspend fun itemContainsHandle(handle: String): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_ITEM_CONTAINS_METADATA).apply {
                    this.setString(1, handle)
                }
            val span = tracer.spanBuilder("itemContainsMetadata").startSpan()
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

    suspend fun getMetadata(handles: List<String>): List<ItemMetadata> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_METADATA).apply {
                    this.setArray(1, connection.createArrayOf("text", handles.toTypedArray()))
                }

            val span = tracer.spanBuilder("getMetadata").startSpan()
            return@useConnection runMetadataStatement(prepStmt, span, connection)
        }

    suspend fun getDeletedMetadata(): List<ItemMetadata> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_DELETED_METADATA)

            val span = tracer.spanBuilder("getDeletedMetadata").startSpan()
            return@useConnection runMetadataStatement(prepStmt, span, connection)
        }

    suspend fun upsertMetadataBatch(itemMetadata: List<ItemMetadata>): IntArray =
        connectionPool.useConnection { connection ->
            val prep = connection.prepareStatement(STATEMENT_UPSERT_METADATA)
            itemMetadata.map {
                val p = insertUpsertMetadataSetParameters(it, prep)
                p.addBatch()
            }
            val span = tracer.spanBuilder("upsertMetadataBatch").startSpan()
            try {
                span.makeCurrent()
                return@useConnection runInTransaction(connection) { prep.executeBatch() }
            } finally {
                span.end()
            }
        }

    suspend fun insertMetadata(itemMetadata: ItemMetadata): String =
        connectionPool.useConnection { connection ->
            val prepStmt =
                insertUpsertMetadataSetParameters(
                    itemMetadata,
                    connection.prepareStatement(STATEMENT_INSERT_METADATA, Statement.RETURN_GENERATED_KEYS),
                )

            val span = tracer.spanBuilder("insertMetadata").startSpan()
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

    suspend fun getMetadataHandlesOlderThanLastUpdatedOn(instant: Instant): List<String> =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_HANDLES_BY_OLDER_THAN_LAST_UPDATED_ON).apply {
                    this.setTimestamp(1, Timestamp.from(instant))
                }
            val span = tracer.spanBuilder("getMetadataHandlesOlderThanLastUpdatedOn").startSpan()
            try {
                span.makeCurrent()
                val rs = runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
                generateSequence {
                    if (rs.next()) {
                        rs.getString(1)
                    } else {
                        null
                    }
                }.takeWhile { true }.toList()
            } finally {
                span.end()
            }
        }

    suspend fun updateMetadataDeleteStatus(
        handles: List<String>,
        status: Boolean,
    ): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_UPDATE_DELETE_STATUS).apply {
                    this.setBoolean(1, status)
                    this.setArray(2, connection.createArrayOf("text", handles.toTypedArray()))
                }
            val span = tracer.spanBuilder("updateMetatadataDeleteStatus").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    companion object {
        const val TS_COMMUNITY = "ts_community"
        const val TS_COMMUNITY_HANDLE = "ts_com_hdl"
        const val TS_COLLECTION = "ts_collection"
        const val TS_COLLECTION_HANDLE = "ts_col_hdl"
        const val TS_HANDLE = "ts_hdl"
        const val TS_SUBCOMMUNITY_HANDLE = "ts_subcom_hdl"
        const val TS_SUBCOMMUNITY_NAME = "ts_subcom_name"
        const val TS_TITLE = "ts_title"

        const val COLUMN_METADATA_AUTHOR = "author"
        const val COLUMN_METADATA_BAND = "band"
        const val COLUMN_METADATA_COLLECTION_HANDLE = "collection_handle"
        const val COLUMN_METADATA_COMMUNITY_HANDLE = "community_handle"
        const val COLUMN_METADATA_COMMUNITY_NAME = "community_name"
        const val COLUMN_METADATA_COLLECTION_NAME = "collection_name"
        const val COLUMN_METADATA_CREATED_BY = "created_by"
        const val COLUMN_METADATA_CREATED_ON = "created_on"
        const val COLUMN_METADATA_DELETED = "deleted"
        const val COLUMN_METADATA_DOI = "doi"
        const val COLUMN_METADATA_ISBN = "isbn"
        const val COLUMN_METADATA_ISSN = "issn"
        const val COLUMN_METADATA_IS_PART_OF_SERIES = "is_part_of_series"
        const val COLUMN_METADATA_HANDLE = "handle"
        const val COLUMN_METADATA_LAST_UPDATED_BY = "last_updated_by"
        const val COLUMN_METADATA_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_METADATA_LICENCE_URL = "licence_url"
        const val COLUMN_METADATA_LICENCE_URL_FILTER = "licence_url_filter"
        const val COLUMN_METADATA_PAKET_SIGEL = "paket_sigel"
        const val COLUMN_METADATA_PPN = "ppn"
        const val COLUMN_METADATA_PUBLICATION_DATE = "publication_date"
        const val COLUMN_METADATA_PUBLICATION_TYPE = "publication_type"
        const val COLUMN_METADATA_RIGHTS_K10PLUS = "rights_k10plus"
        const val COLUMN_METADATA_STORAGE_DATE = "storage_date"
        const val COLUMN_METADATA_SUBCOMMUNITY_HANDLE = "sub_community_handle"
        const val COLUMN_METADATA_SUBCOMMUNITY_NAME = "sub_community_name"
        const val COLUMN_METADATA_TITLE = "title"
        const val COLUMN_METADATA_TITLE_JOURNAL = "title_journal"
        const val COLUMN_METADATA_TITLE_SERIES = "title_series"
        const val COLUMN_METADATA_ZDB_ID_JOURNAL = "zdb_id_journal"
        const val COLUMN_METADATA_ZDB_ID_SERIES = "zdb_id_series"

        const val STATEMENT_METADATA_CONTAINS_HANDLE =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE handle=?)"

        const val STATEMENT_DELETE_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.handle = ANY(?)"

        const val STATEMENT_SELECT_ALL_METADATA_FROM =
            "SELECT $TABLE_NAME_ITEM_METADATA.handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "collection_handle,licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED" +
                " FROM $TABLE_NAME_ITEM_METADATA"

        const val STATEMENT_GET_HANDLES_BY_OLDER_THAN_LAST_UPDATED_ON =
            "SELECT $COLUMN_METADATA_HANDLE" +
                " FROM $TABLE_NAME_ITEM_METADATA" +
                " WHERE $COLUMN_METADATA_LAST_UPDATED_ON < ?;"

        const val STATEMENT_UPDATE_DELETE_STATUS =
            "UPDATE $TABLE_NAME_ITEM_METADATA" +
                " SET $COLUMN_METADATA_DELETED=?" +
                " WHERE $COLUMN_METADATA_HANDLE=ANY(?)"

        const val STATEMENT_SELECT_ALL_METADATA =
            "SELECT $TABLE_NAME_ITEM_METADATA.handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle," +
                "licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME,$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER,$COLUMN_METADATA_DELETED," +
                "$TS_COLLECTION,$TS_COMMUNITY,$TS_TITLE,$TS_COLLECTION_HANDLE," +
                "$TS_COMMUNITY_HANDLE,$TS_SUBCOMMUNITY_HANDLE,$TS_HANDLE,$TS_SUBCOMMUNITY_NAME"

        const val STATEMENT_GET_METADATA =
            STATEMENT_SELECT_ALL_METADATA_FROM +
                " WHERE $COLUMN_METADATA_HANDLE = ANY(?)"

        const val STATEMENT_GET_DELETED_METADATA =
            STATEMENT_SELECT_ALL_METADATA_FROM +
                " WHERE $COLUMN_METADATA_DELETED = true"

        const val STATEMENT_UPSERT_METADATA =
            "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
                "(handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle,licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED) " +
                "VALUES(" +
                "?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?) " +
                "ON CONFLICT (handle) " +
                "DO UPDATE SET " +
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
                "$COLUMN_METADATA_ZDB_ID_JOURNAL = EXCLUDED.$COLUMN_METADATA_ZDB_ID_JOURNAL," +
                "issn = EXCLUDED.issn," +
                "last_updated_on = EXCLUDED.last_updated_on," +
                "last_updated_by = EXCLUDED.last_updated_by," +
                "author = EXCLUDED.author," +
                "collection_name = EXCLUDED.collection_name," +
                "community_name = EXCLUDED.community_name," +
                "storage_date = EXCLUDED.storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE = EXCLUDED.$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle = EXCLUDED.community_handle," +
                "collection_handle = EXCLUDED.collection_handle," +
                "licence_url = EXCLUDED.licence_url," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME = EXCLUDED.$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES = EXCLUDED.$COLUMN_METADATA_IS_PART_OF_SERIES," +
                "$COLUMN_METADATA_ZDB_ID_SERIES = EXCLUDED.$COLUMN_METADATA_ZDB_ID_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER = EXCLUDED.$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED = EXCLUDED.$COLUMN_METADATA_DELETED;"

        const val STATEMENT_ITEM_CONTAINS_METADATA =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE $COLUMN_METADATA_HANDLE=?)"

        const val STATEMENT_INSERT_METADATA =
            "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
                "(handle,ppn,title,title_journal," +
                "title_series,$COLUMN_METADATA_PUBLICATION_DATE,band,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,rights_k10plus,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_ID_JOURNAL,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "author,collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,collection_handle,licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_ZDB_ID_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED" +
                ") " +
                "VALUES(" +
                "?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?)"

        fun extractMetadataRS(rs: ResultSet) =
            ItemMetadata(
                handle = rs.getString(1),
                ppn = rs.getString(2),
                title = rs.getString(3),
                titleJournal = rs.getString(4),
                titleSeries = rs.getString(5),
                publicationDate = rs.getDate(6)?.toLocalDate(),
                band = rs.getString(7),
                publicationType = PublicationType.valueOf(rs.getString(8)),
                doi = rs.getString(9),
                isbn = rs.getString(10),
                rightsK10plus = rs.getString(11),
                paketSigel = rs.getString(12),
                zdbIdJournal = rs.getString(13),
                issn = rs.getString(14),
                createdOn = rs.getTimestamp(15)?.toOffsetDateTime(),
                lastUpdatedOn = rs.getTimestamp(16)?.toOffsetDateTime(),
                createdBy = rs.getString(17),
                lastUpdatedBy = rs.getString(18),
                author = rs.getString(19),
                collectionName = rs.getString(20),
                communityName = rs.getString(21),
                storageDate = rs.getTimestamp(22)?.toOffsetDateTime(),
                subCommunityHandle = rs.getString(23),
                communityHandle = rs.getString(24),
                collectionHandle = rs.getString(25),
                licenceUrl = rs.getString(26),
                subCommunityName = rs.getString(27),
                isPartOfSeries = rs.getString(28),
                zdbIdSeries = rs.getString(29),
                licenceUrlFilter = rs.getString(30),
                deleted = rs.getBoolean(31),
            )

        private fun insertUpsertMetadataSetParameters(
            itemMetadata: ItemMetadata,
            prep: PreparedStatement,
        ): PreparedStatement {
            val now = Instant.now()
            return prep.apply {
                this.setString(1, itemMetadata.handle)
                this.setIfNotNull(2, itemMetadata.ppn) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setString(3, itemMetadata.title)
                this.setIfNotNull(4, itemMetadata.titleJournal) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(5, itemMetadata.titleSeries) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(6, itemMetadata.publicationDate) { value, idx, prepStmt ->
                    prepStmt.setDate(idx, Date.valueOf(value))
                }
                this.setIfNotNull(7, itemMetadata.band) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setString(8, itemMetadata.publicationType.toString())
                this.setIfNotNull(9, itemMetadata.doi) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(10, itemMetadata.isbn) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(11, itemMetadata.rightsK10plus) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(12, itemMetadata.paketSigel) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(13, itemMetadata.zdbIdJournal) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(14, itemMetadata.issn) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setTimestamp(15, Timestamp.from(now))
                this.setTimestamp(16, Timestamp.from(now))
                this.setIfNotNull(17, itemMetadata.createdBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(18, itemMetadata.lastUpdatedBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(19, itemMetadata.author) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(20, itemMetadata.collectionName) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(21, itemMetadata.communityName) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(22, itemMetadata.storageDate) { value, idx, prepStmt ->
                    prepStmt.setTimestamp(idx, Timestamp.from(value.toInstant()))
                }
                this.setIfNotNull(23, itemMetadata.subCommunityHandle) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(24, itemMetadata.communityHandle) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(25, itemMetadata.collectionHandle) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(26, itemMetadata.licenceUrl) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(27, itemMetadata.subCommunityName) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(28, itemMetadata.isPartOfSeries) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(29, itemMetadata.zdbIdSeries) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(30, itemMetadata.licenceUrlFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setBoolean(31, itemMetadata.deleted)
            }
        }
    }
}
