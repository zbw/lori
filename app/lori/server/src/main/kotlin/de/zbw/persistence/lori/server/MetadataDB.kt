package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ItemMetadata
import de.zbw.business.lori.server.type.PublicationType
import de.zbw.business.lori.server.type.SortInformation
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM_METADATA
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.toOffsetDateTime
import de.zbw.persistence.lori.server.statistics.MetadataHandleLastUpdatedTransient
import io.opentelemetry.api.trace.Tracer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import kotlin.collections.first

/**
 * Execute SQL queries strongly related to metadata.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class MetadataDB(
    connectionPool: ConnectionPool,
    tracer: Tracer,
) : AbstractDB(connectionPool, tracer, TABLE_NAME_ITEM_METADATA) {
    internal suspend fun deleteMetadata(handles: List<String>): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_DELETE_METADATA,
            tracer = tracer,
            spanName = "deleteMetadata",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", handles.toTypedArray()))
            },
        )

    suspend fun metadataContainsHandle(handle: String): Boolean =
        DatabaseConnector
            .select(
                connectionPool = connectionPool,
                sql = STATEMENT_METADATA_CONTAINS_HANDLE,
                tracer = tracer,
                spanName = "metadataContainsHandle",
                params = { stmt ->
                    stmt.setString(1, handle)
                },
                mapper = { rs ->
                    rs.getBoolean(1)
                },
            ).first()

    suspend fun getMetadataRange(
        limit: Int,
        offset: Int,
    ): List<ItemMetadata> {
        val sql =
            STATEMENT_SELECT_ALL_METADATA_FROM +
                " ORDER BY ${SortInformation.DEFAULT.sortByField.columnName}" +
                " ${SortInformation.DEFAULT.sortOrder.sqlSyntax} LIMIT ? OFFSET ?;"
        return DatabaseConnector.select(
            connectionPool = connectionPool,
            sql = sql,
            tracer = tracer,
            spanName = "getMetadataRange",
            params = { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
            },
            mapper = { rs ->
                extractMetadataRS(rs)
            },
        )
    }

    suspend fun itemContainsHandle(handle: String): Boolean =
        DatabaseConnector
            .select(
                connectionPool = connectionPool,
                sql = STATEMENT_ITEM_CONTAINS_METADATA,
                tracer = tracer,
                spanName = "itemContainsHandle",
                params = { stmt ->
                    stmt.setString(1, handle)
                },
                mapper = { rs ->
                    rs.getBoolean(1)
                },
            ).first()

    suspend fun getMetadata(handles: List<String>): List<ItemMetadata> =
        DatabaseConnector.select(
            sql = STATEMENT_GET_METADATA,
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "getMetadata",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", handles.toTypedArray()))
            },
            mapper = { rs ->
                extractMetadataRS(rs)
            },
        )

    suspend fun getExistingHandles(handles: List<String>): List<String> =
        DatabaseConnector.select(
            sql = STATEMENT_GET_EXISTING_HANDLES,
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "getExistingHandles",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", handles.toTypedArray()))
            },
            mapper = { rs ->
                rs.getString(1)
            },
        )

    suspend fun getDeletedMetadataByHandles(handles: List<String>): List<String> =
        DatabaseConnector.select(
            sql = STATEMENT_GET_DELETED_METADATA_BY_HANDLES,
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "getMetadataByHandles",
            params = { stmt ->
                stmt.setArray(1, stmt.connection.createArrayOf("text", handles.toTypedArray()))
            },
            mapper = { rs ->
                rs.getString(1)
            },
        )

    suspend fun getDeletedMetadata(
        limit: Int,
        offset: Int,
    ): List<ItemMetadata> =
        DatabaseConnector.select(
            sql =
                STATEMENT_GET_DELETED_METADATA.dropLast(1) +
                    " ORDER BY ${SortInformation.DEFAULT.sortByField.columnName}" +
                    " ${SortInformation.DEFAULT.sortOrder.sqlSyntax} LIMIT ? OFFSET ?;",
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "getDeletedMetadata",
            params = { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
            },
            mapper = { rs ->
                extractMetadataRS(rs)
            },
        )

    suspend fun getDeletedMetadataCount(): Int =
        DatabaseConnector
            .select(
                connectionPool = connectionPool,
                sql = STATEMENT_GET_DELETED_METADATA_COUNT,
                tracer = tracer,
                spanName = "getDeletedMetadataCount",
                mapper = { rs ->
                    rs.getInt(1)
                },
            ).first()

    suspend fun upsertMetadataBatch(itemMetadata: List<ItemMetadata>): IntArray =
        DatabaseConnector.insertBatch(
            connectionPool = connectionPool,
            sql = STATEMENT_UPSERT_METADATA,
            tracer = tracer,
            spanName = "upsertMetadataBatch",
            params = { stmt ->
                itemMetadata.forEach {
                    insertUpsertMetadataSetParameters(
                        itemMetadata = it,
                        prep = stmt,
                    )
                    stmt.addBatch()
                }
            },
        )

    suspend fun insertMetadata(itemMetadata: ItemMetadata): String =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool = connectionPool,
                sql = STATEMENT_INSERT_METADATA,
                tracer = tracer,
                spanName = "insertMetadata",
                params = { stmt ->
                    insertUpsertMetadataSetParameters(
                        itemMetadata,
                        stmt,
                    )
                },
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
            ).first()

    suspend fun getMetadataHandlesOlderThanLastUpdatedOn(instant: Instant): List<MetadataHandleLastUpdatedTransient> =
        DatabaseConnector.select(
            connectionPool = connectionPool,
            sql = STATEMENT_GET_HANDLES_BY_OLDER_THAN_LAST_UPDATED_ON,
            tracer = tracer,
            spanName = "getMetadataHandlesOlderThanLastUpdatedOn",
            params = { stmt ->
                stmt.setTimestamp(1, Timestamp.from(instant))
            },
            mapper = { rs ->
                MetadataHandleLastUpdatedTransient(
                    handle = rs.getString(1),
                    lastUpdatedOn = rs.getTimestamp(2, utcCalendar).toInstant(),
                    createdOn = rs.getTimestamp(3, utcCalendar).toInstant(),
                    isDeleted = rs.getBoolean(4),
                )
            },
        )

    suspend fun updateMetadataDeleteStatus(
        handles: List<String>,
        status: Boolean,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_UPDATE_DELETE_STATUS,
            tracer = tracer,
            spanName = "updateMetadataDeleteStatus",
            params = { stmt ->
                stmt.setBoolean(1, status)
                stmt.setArray(2, stmt.connection.createArrayOf("text", handles.toTypedArray()))
            },
        )

    companion object {
        const val TS_COMMUNITY = "ts_community"
        const val TS_COMMUNITY_HANDLE = "ts_com_hdl"
        const val TS_COLLECTION = "ts_collection"
        const val TS_COLLECTION_HANDLE = "ts_col_hdl"
        const val TS_HANDLE = "ts_hdl"
        const val TS_SUBCOMMUNITY_HANDLE = "ts_subcom_hdl"
        const val TS_SUBCOMMUNITY_NAME = "ts_subcom_name"
        const val TS_LICENCE_URL = "ts_licence_url"
        const val TS_TITLE = "ts_title"

        const val COLUMN_METADATA_COLLECTION_HANDLE = "collection_handle"
        const val COLUMN_METADATA_COMMUNITY_HANDLE = "community_handle"
        const val COLUMN_METADATA_COMMUNITY_NAME = "community_name"
        const val COLUMN_METADATA_COLLECTION_NAME = "collection_name"
        const val COLUMN_METADATA_CREATED_BY = "created_by"
        const val COLUMN_METADATA_CREATED_ON = "created_on"
        const val COLUMN_METADATA_DELETED = "deleted"
        const val COLUMN_METADATA_DOI = "doi"
        const val COLUMN_METADATA_DOI_LOWER = "doi_joined_lower"
        const val COLUMN_METADATA_ECONBIZID = "econbizid"
        const val COLUMN_METADATA_ECONSTOR_ISSUE = "econstor_issue"
        const val COLUMN_METADATA_ECONSTOR_VOLUME = "econstor_volume"
        const val COLUMN_METADATA_ENUMERATION = "enumeration"
        const val COLUMN_METADATA_ISBN = "isbn"
        const val COLUMN_METADATA_ISBN_LOWER = "isbn_joined_lower"
        const val COLUMN_METADATA_ISSN = "issn"
        const val COLUMN_METADATA_ISSN_LOWER = "issn_joined_lower"
        const val COLUMN_METADATA_IS_PART_OF_BOOK = "is_part_of_book"
        const val COLUMN_METADATA_IS_PART_OF_JOURNAL = "is_part_of_journal"
        const val COLUMN_METADATA_IS_PART_OF_SERIES = "is_part_of_series"
        const val COLUMN_METADATA_IS_PART_OF_SERIES_LOWER = "is_part_of_series_joined_lower"
        const val COLUMN_METADATA_HANDLE = "handle"
        const val COLUMN_METADATA_HANDLE_POSTFIX = "handle_postfix"
        const val COLUMN_METADATA_LAST_UPDATED_BY = "last_updated_by"
        const val COLUMN_METADATA_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_METADATA_LICENCE_URL = "licence_url"
        const val COLUMN_METADATA_LICENCE_URL_FILTER = "licence_url_filter"
        const val COLUMN_METADATA_PAKET_SIGEL = "paket_sigel"
        const val COLUMN_METADATA_PAKET_SIGEL_LOWER = "paket_sigel_joined_lower"
        const val COLUMN_METADATA_PPN = "ppn"
        const val COLUMN_METADATA_PPN_BOOK = "ppn_book"
        const val COLUMN_METADATA_PPN_JOURNAL = "ppn_journal"
        const val COLUMN_METADATA_PPN_SERIES = "ppn_series"
        const val COLUMN_METADATA_PUBLICATION_YEAR = "publication_year"
        const val COLUMN_METADATA_PUBLICATION_TYPE = "publication_type"
        const val COLUMN_METADATA_STORAGE_DATE = "storage_date"
        const val COLUMN_METADATA_SUBCOMMUNITY_HANDLE = "sub_community_handle"
        const val COLUMN_METADATA_SUBCOMMUNITY_NAME = "sub_community_name"
        const val COLUMN_METADATA_TITLE = "title"
        const val COLUMN_METADATA_ZDB_IDS = "zdb_ids"
        const val COLUMN_METADATA_ZDB_IDS_LOWER = "zdb_ids_joined_lower"

        val utcCalendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone(TimezoneUtil.TIME_ZONE_UTC))

        const val STATEMENT_METADATA_CONTAINS_HANDLE =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM_METADATA WHERE handle=?)"

        const val STATEMENT_DELETE_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM_METADATA h " +
                "WHERE h.handle = ANY(?)"

        const val STATEMENT_SELECT_ALL_METADATA_FROM =
            "SELECT $TABLE_NAME_ITEM_METADATA.handle,ppn,title," +
                "$COLUMN_METADATA_PUBLICATION_YEAR,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,issn," +
                "$TABLE_NAME_ITEM_METADATA.created_on,$TABLE_NAME_ITEM_METADATA.last_updated_on," +
                "$TABLE_NAME_ITEM_METADATA.created_by,$TABLE_NAME_ITEM_METADATA.last_updated_by," +
                "collection_name,community_name,storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE,community_handle," +
                "$COLUMN_METADATA_COLLECTION_HANDLE,licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED,$COLUMN_METADATA_ECONBIZID,$COLUMN_METADATA_ECONSTOR_ISSUE," +
                "$COLUMN_METADATA_ECONSTOR_VOLUME,$COLUMN_METADATA_IS_PART_OF_BOOK," +
                "$COLUMN_METADATA_IS_PART_OF_JOURNAL," +
                "$COLUMN_METADATA_PPN_BOOK,$COLUMN_METADATA_PPN_JOURNAL,$COLUMN_METADATA_PPN_SERIES," +
                "$COLUMN_METADATA_ENUMERATION" +
                " FROM $TABLE_NAME_ITEM_METADATA"

        const val STATEMENT_GET_HANDLES_BY_OLDER_THAN_LAST_UPDATED_ON =
            "SELECT $COLUMN_METADATA_HANDLE, $COLUMN_METADATA_LAST_UPDATED_ON," +
                " $COLUMN_METADATA_CREATED_ON,$COLUMN_METADATA_DELETED" +
                " FROM $TABLE_NAME_ITEM_METADATA" +
                " WHERE $COLUMN_METADATA_LAST_UPDATED_ON < ?;"

        const val STATEMENT_UPDATE_DELETE_STATUS =
            "UPDATE $TABLE_NAME_ITEM_METADATA" +
                " SET $COLUMN_METADATA_DELETED=?" +
                " WHERE $COLUMN_METADATA_HANDLE=ANY(?);"

        const val STATEMENT_GET_METADATA =
            STATEMENT_SELECT_ALL_METADATA_FROM +
                " WHERE $COLUMN_METADATA_HANDLE = ANY(?);"

        const val STATEMENT_GET_EXISTING_HANDLES =
            "SELECT $COLUMN_METADATA_HANDLE" +
                " FROM $TABLE_NAME_ITEM_METADATA" +
                " WHERE $COLUMN_METADATA_HANDLE = ANY(?);"

        const val STATEMENT_GET_DELETED_METADATA =
            STATEMENT_SELECT_ALL_METADATA_FROM +
                " WHERE $COLUMN_METADATA_DELETED = true;"

        const val STATEMENT_GET_DELETED_METADATA_BY_HANDLES =
            "SELECT $COLUMN_METADATA_HANDLE" +
                " FROM $TABLE_NAME_ITEM_METADATA" +
                " WHERE $COLUMN_METADATA_DELETED = true AND" +
                " $COLUMN_METADATA_HANDLE = ANY(?);"

        const val STATEMENT_GET_DELETED_METADATA_COUNT =
            "SELECT COUNT(*)" +
                " FROM $TABLE_NAME_ITEM_METADATA" +
                " WHERE $COLUMN_METADATA_DELETED = true;"

        const val STATEMENT_UPSERT_METADATA =
            "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
                "(handle,ppn,title," +
                "$COLUMN_METADATA_PUBLICATION_YEAR,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,$COLUMN_METADATA_COLLECTION_HANDLE,licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED,$COLUMN_METADATA_ECONBIZID,$COLUMN_METADATA_ECONSTOR_ISSUE," +
                "$COLUMN_METADATA_ECONSTOR_VOLUME,$COLUMN_METADATA_IS_PART_OF_BOOK,$COLUMN_METADATA_IS_PART_OF_JOURNAL," +
                "$COLUMN_METADATA_PPN_BOOK,$COLUMN_METADATA_PPN_JOURNAL,$COLUMN_METADATA_PPN_SERIES) " +
                "VALUES(" +
                "?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?) " +
                "ON CONFLICT (handle) " +
                "DO UPDATE SET " +
                "ppn = EXCLUDED.ppn," +
                "title = EXCLUDED.title," +
                "$COLUMN_METADATA_PUBLICATION_YEAR = EXCLUDED.$COLUMN_METADATA_PUBLICATION_YEAR," +
                "$COLUMN_METADATA_PUBLICATION_TYPE = EXCLUDED.$COLUMN_METADATA_PUBLICATION_TYPE," +
                "doi = EXCLUDED.doi," +
                "isbn = EXCLUDED.isbn," +
                "$COLUMN_METADATA_PAKET_SIGEL = EXCLUDED.$COLUMN_METADATA_PAKET_SIGEL," +
                "$COLUMN_METADATA_ZDB_IDS = EXCLUDED.$COLUMN_METADATA_ZDB_IDS," +
                "issn = EXCLUDED.issn," +
                "last_updated_on = EXCLUDED.last_updated_on," +
                "last_updated_by = EXCLUDED.last_updated_by," +
                "collection_name = EXCLUDED.collection_name," +
                "community_name = EXCLUDED.community_name," +
                "storage_date = EXCLUDED.storage_date," +
                "$COLUMN_METADATA_SUBCOMMUNITY_HANDLE = EXCLUDED.$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle = EXCLUDED.community_handle," +
                "$COLUMN_METADATA_COLLECTION_HANDLE = EXCLUDED.$COLUMN_METADATA_COLLECTION_HANDLE," +
                "licence_url = EXCLUDED.licence_url," +
                "$COLUMN_METADATA_SUBCOMMUNITY_NAME = EXCLUDED.$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES = EXCLUDED.$COLUMN_METADATA_IS_PART_OF_SERIES," +
                "$COLUMN_METADATA_LICENCE_URL_FILTER = EXCLUDED.$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED = EXCLUDED.$COLUMN_METADATA_DELETED," +
                "$COLUMN_METADATA_ECONBIZID = EXCLUDED.$COLUMN_METADATA_ECONBIZID," +
                "$COLUMN_METADATA_ECONSTOR_ISSUE = EXCLUDED.$COLUMN_METADATA_ECONSTOR_ISSUE," +
                "$COLUMN_METADATA_ECONSTOR_VOLUME = EXCLUDED.$COLUMN_METADATA_ECONSTOR_VOLUME," +
                "$COLUMN_METADATA_IS_PART_OF_BOOK = EXCLUDED.$COLUMN_METADATA_IS_PART_OF_BOOK," +
                "$COLUMN_METADATA_IS_PART_OF_JOURNAL = EXCLUDED.$COLUMN_METADATA_IS_PART_OF_JOURNAL," +
                "$COLUMN_METADATA_PPN_BOOK = EXCLUDED.$COLUMN_METADATA_PPN_BOOK," +
                "$COLUMN_METADATA_PPN_JOURNAL = EXCLUDED.$COLUMN_METADATA_PPN_JOURNAL," +
                "$COLUMN_METADATA_PPN_SERIES = EXCLUDED.$COLUMN_METADATA_PPN_SERIES" +
                ";"

        const val STATEMENT_ITEM_CONTAINS_METADATA =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE $COLUMN_METADATA_HANDLE=?)"

        const val STATEMENT_INSERT_METADATA =
            "INSERT INTO $TABLE_NAME_ITEM_METADATA" +
                "(handle,ppn,title,$COLUMN_METADATA_PUBLICATION_YEAR,$COLUMN_METADATA_PUBLICATION_TYPE,doi," +
                "isbn,$COLUMN_METADATA_PAKET_SIGEL,$COLUMN_METADATA_ZDB_IDS,issn," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "collection_name,community_name,storage_date,$COLUMN_METADATA_SUBCOMMUNITY_HANDLE," +
                "community_handle,$COLUMN_METADATA_COLLECTION_HANDLE,licence_url,$COLUMN_METADATA_SUBCOMMUNITY_NAME," +
                "$COLUMN_METADATA_IS_PART_OF_SERIES,$COLUMN_METADATA_LICENCE_URL_FILTER," +
                "$COLUMN_METADATA_DELETED,$COLUMN_METADATA_ECONBIZID,$COLUMN_METADATA_ECONSTOR_ISSUE," +
                "$COLUMN_METADATA_ECONSTOR_VOLUME,$COLUMN_METADATA_IS_PART_OF_BOOK,$COLUMN_METADATA_IS_PART_OF_JOURNAL," +
                "$COLUMN_METADATA_PPN_BOOK,$COLUMN_METADATA_PPN_JOURNAL,$COLUMN_METADATA_PPN_SERIES" +
                ") " +
                "VALUES(" +
                "?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?,?," +
                "?,?,?,?)"

        fun extractMetadataRS(rs: ResultSet): ItemMetadata {
            var localCounter = 1
            return ItemMetadata(
                handle = rs.getString(localCounter++),
                ppn = rs.getString(localCounter++),
                title = rs.getString(localCounter++),
                publicationYear = rs.getInt(localCounter++),
                publicationType = PublicationType.valueOf(rs.getString(localCounter++)),
                pids = (rs.getArray(localCounter++)?.array as? Array<out Any?>)?.filterIsInstance<String>(),
                isbn = (rs.getArray(localCounter++)?.array as? Array<out Any?>)?.filterIsInstance<String>(),
                paketSigel = (rs.getArray(localCounter++)?.array as? Array<out Any?>)?.filterIsInstance<String>(),
                zdbIds = (rs.getArray(localCounter++)?.array as? Array<out Any?>)?.filterIsInstance<String>(),
                issn = (rs.getArray(localCounter++)?.array as? Array<out Any?>)?.filterIsInstance<String>(),
                createdOn = rs.getTimestamp(localCounter++, utcCalendar)?.toOffsetDateTime(),
                lastUpdatedOn = rs.getTimestamp(localCounter++, utcCalendar)?.toOffsetDateTime(),
                createdBy = rs.getString(localCounter++),
                lastUpdatedBy = rs.getString(localCounter++),
                collectionName = rs.getString(localCounter++),
                communityName = rs.getString(localCounter++),
                storageDate = rs.getTimestamp(localCounter++)?.toOffsetDateTime(),
                subCommunityHandle = rs.getString(localCounter++),
                communityHandle = rs.getString(localCounter++),
                collectionHandle = rs.getString(localCounter++),
                licenceUrl = rs.getString(localCounter++),
                subCommunityName = rs.getString(localCounter++),
                isPartOfSeries = (rs.getArray(localCounter++)?.array as? Array<out Any?>)?.filterIsInstance<String>(),
                licenceUrlFilter = rs.getString(localCounter++),
                deleted = rs.getBoolean(localCounter++),
                econbizId = rs.getString(localCounter++),
                econstorIssue = rs.getString(localCounter++),
                econstorVolume = rs.getString(localCounter++),
                isPartOfBook = rs.getString(localCounter++),
                isPartOfJournal = rs.getString(localCounter++),
                ppnBook = rs.getString(localCounter++),
                ppnJournal = rs.getString(localCounter++),
                ppnSeries = rs.getString(localCounter++),
                enumeration = rs.getString(localCounter++),
            )
        }

        private fun insertUpsertMetadataSetParameters(
            itemMetadata: ItemMetadata,
            prep: PreparedStatement,
            sqlCounter: Int = 1,
        ): PreparedStatement {
            val now = Instant.now()
            var localCounter = sqlCounter
            return prep.apply {
                this.setString(localCounter++, itemMetadata.handle)
                this.setIfNotNull(localCounter++, itemMetadata.ppn) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setString(localCounter++, itemMetadata.title)
                this.setIfNotNull(localCounter++, itemMetadata.publicationYear) { value, idx, prepStmt ->
                    prepStmt.setInt(idx, value)
                }
                this.setString(localCounter++, itemMetadata.publicationType.toString())
                this.setIfNotNull(localCounter++, itemMetadata.pids) { value, idx, prepStmt ->
                    prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
                }
                this.setIfNotNull(localCounter++, itemMetadata.isbn) { value, idx, prepStmt ->
                    prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
                }

                this.setIfNotNull(localCounter++, itemMetadata.paketSigel) { value, idx, prepStmt ->
                    prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
                }
                this.setIfNotNull(localCounter++, itemMetadata.zdbIds) { value, idx, prepStmt ->
                    prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
                }
                this.setIfNotNull(localCounter++, itemMetadata.issn) { value, idx, prepStmt ->
                    prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
                }
                this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                this.setIfNotNull(localCounter++, itemMetadata.createdBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.lastUpdatedBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.collectionName) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.communityName) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.storageDate) { value, idx, prepStmt ->
                    prepStmt.setTimestamp(idx, Timestamp.from(value.toInstant()))
                }
                this.setIfNotNull(localCounter++, itemMetadata.subCommunityHandle) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.communityHandle) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.collectionHandle) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.licenceUrl) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.subCommunityName) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.isPartOfSeries) { value, idx, prepStmt ->
                    prepStmt.setArray(idx, connection.createArrayOf("text", value.toTypedArray()))
                }
                this.setIfNotNull(localCounter++, itemMetadata.licenceUrlFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setBoolean(localCounter++, itemMetadata.deleted)
                this.setIfNotNull(localCounter++, itemMetadata.econbizId) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.econstorIssue) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.econstorVolume) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.isPartOfBook) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.isPartOfJournal) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.ppnBook) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.ppnJournal) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, itemMetadata.ppnSeries) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
            }
        }
    }
}
