package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.type.ItemRow
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Calendar
import java.util.TimeZone

/**
 * Execute SQL queries strongly related to items.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ItemDB(
    connectionPool: ConnectionPool,
    batchConnectionPool: ConnectionPool,
    tracer: Tracer,
) : AbstractDB(connectionPool, batchConnectionPool, tracer, TABLE_NAME_ITEM) {
    suspend fun getRightIdsByHandle(
        handle: String,
        isBatchJob: Boolean = false,
    ): List<String> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_GET_RIGHT_IDS_BY_HANDLE_ID,
            tracer = tracer,
            spanName = "getRightIdsByHandle",
            params = { stmt ->
                stmt.setString(1, handle)
            },
            mapper = { rs: ResultSet ->
                rs.getString(1)
            },
        )

    suspend fun getHandlesByRightId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): List<String> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_GET_HANDLES_BY_RIGHT_ID,
            tracer = tracer,
            spanName = "getHandlesByRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
            mapper = { rs: ResultSet ->
                rs.getString(1)
            },
        )

    suspend fun getHandlesCount(isBatchJob: Boolean = false): Int =
        DatabaseConnector.count(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_COUNT_DISTINCT_HANDLE,
            tracer = tracer,
            spanName = "getHandlesCount",
        )

    suspend fun getDistinctHandlesByOffset(
        limit: Int,
        offset: Int,
        isBatchJob: Boolean = false,
    ): List<String> =
        DatabaseConnector.select(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            tracer = tracer,
            sql = STATEMENT_SELECT_DISTINCT_HANDLE,
            spanName = "getDistinctHandlesByOffset",
            params = { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
            },
            mapper = { rs ->
                rs.getString(1)
            },
        )

    suspend fun itemContainsEntry(
        handle: String,
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
                sql = STATEMENT_ITEM_CONTAINS_ENTRY,
                tracer = tracer,
                spanName = "itemContainsEntry",
                params = { stmt ->
                    stmt.setString(1, handle)
                    stmt.setString(2, rightId)
                },
                mapper = { rs ->
                    rs.getBoolean(1)
                },
            ).first()

    /**
     * Check if a given rightId is still used in the table.
     */
    suspend fun itemContainsRightId(
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
                sql = STATEMENT_ITEM_CONTAINS_RIGHT,
                tracer = tracer,
                spanName = "itemContainsRight",
                params = { stmt ->
                    stmt.setString(1, rightId)
                },
                mapper = { rs ->
                    rs.getBoolean(1)
                },
            ).first()

    suspend fun insertItem(
        itemId: ItemId,
        createdBy: String,
        isBatchJob: Boolean = false,
    ): String? =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_INSERT_ITEM,
                tracer = tracer,
                spanName = "insertItem",
                params = { stmt ->
                    val now = Instant.now()
                    var localCounter = 1
                    stmt.setString(localCounter++, itemId.handle)
                    stmt.setString(localCounter++, itemId.rightId)
                    stmt.setString(localCounter++, createdBy)
                    stmt.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                    stmt.setString(localCounter++, createdBy)
                    stmt.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                },
                fetchGenerated = { rs ->
                    rs.getString(1)
                },
            ).firstOrNull()

    suspend fun upsertItemBatch(
        itemIds: List<ItemId>,
        createdBy: String,
        isBatchJob: Boolean = false,
    ): IntArray =
        DatabaseConnector.insertBatch(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_INSERT_ITEM,
            tracer = tracer,
            spanName = "insertItemBatch",
            params = { stmt ->
                val now = Instant.now()
                var localCounter = 1
                itemIds.map {
                    val p =
                        stmt.apply {
                            this.setString(localCounter++, it.handle)
                            this.setString(localCounter++, it.rightId)
                            this.setString(localCounter++, createdBy)
                            this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                            this.setString(localCounter++, createdBy)
                            this.setTimestamp(localCounter, Timestamp.from(now), utcCalendar)
                        }
                    p.addBatch()
                    localCounter = 1
                }
            },
        )

    suspend fun deleteItem(
        handle: String,
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
            sql = STATEMENT_DELETE_ITEM,
            tracer = tracer,
            spanName = "deleteItem",
            params = { stmt ->
                stmt.setString(1, rightId)
                stmt.setString(2, handle)
            },
        )

    suspend fun countItemByRightId(
        rightId: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.count(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_COUNT_ITEM_BY_RIGHTID,
            tracer = tracer,
            spanName = "countItemByRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
        )

    suspend fun deleteItemByHandle(
        handle: String,
        isBatchJob: Boolean = false,
    ): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql = STATEMENT_DELETE_ITEM_BY_HANDLE,
            tracer = tracer,
            spanName = "deleteItemByHandle",
            params = { stmt ->
                stmt.setString(1, handle)
            },
        )

    suspend fun deleteItemByRightId(
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
            sql = STATEMENT_DELETE_ITEM_BY_RIGHT,
            tracer = tracer,
            spanName = "deleteItemByRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
        )

    suspend fun getItemRowByHandleAndRightId(
        handle: String,
        rightId: String,
        isBatchJob: Boolean = false,
    ): ItemRow? =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_RIGHTS_IDS_FOR_METADATA,
                tracer = tracer,
                spanName = "getItemRowByHandleAndRightId",
                params = { stmt ->
                    stmt.setString(1, handle)
                    stmt.setString(2, rightId)
                },
                mapper = { rs -> rs.toItemRow() },
            ).firstOrNull()

    suspend fun getItemsByLastUpdatedBeforeAndRightId(
        rightId: String,
        lastUpdatedBefore: Instant,
        isBatchJob: Boolean = false,
    ): List<ItemRow> =
        DatabaseConnector
            .select(
                connectionPool =
                    if (isBatchJob) {
                        batchConnectionPool
                    } else {
                        connectionPool
                    },
                sql = STATEMENT_GET_RIGHTS_UPDATED_BEFORE_BY_RIGHT_ID,
                tracer = tracer,
                spanName = "getItemLastUpdatedBeforeByRightId",
                mapper = { rs -> rs.toItemRow() },
                params = { stmt ->
                    stmt.setString(1, rightId)
                    stmt.setTimestamp(2, Timestamp.from(lastUpdatedBefore), utcCalendar)
                },
            )

    companion object {
        private const val CONSTRAINT_ITEM_PKEY = "item_pkey"
        const val COLUMN_ITEM_HANDLE = "handle"
        const val COLUMN_ITEM_RIGHT_ID = "right_id"
        const val COLUMN_ITEM_CREATED_BY = "created_by"
        const val COLUMN_ITEM_CREATED_ON = "created_on"
        const val COLUMN_ITEM_LAST_UPDATED_BY = "last_updated_by"
        const val COLUMN_ITEM_LAST_UPDATED_ON = "last_updated_on"

        val utcCalendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone(TimezoneUtil.TIME_ZONE_UTC))

        const val STATEMENT_COUNT_ITEM_BY_RIGHTID =
            "SELECT COUNT(*) " +
                "FROM $TABLE_NAME_ITEM " +
                "WHERE $COLUMN_ITEM_RIGHT_ID = ?;"

        const val STATEMENT_GET_RIGHT_IDS_BY_HANDLE_ID =
            "SELECT $COLUMN_ITEM_RIGHT_ID" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE $COLUMN_ITEM_HANDLE = ?"

        const val STATEMENT_GET_HANDLES_BY_RIGHT_ID =
            "SELECT $COLUMN_ITEM_HANDLE" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE $COLUMN_ITEM_RIGHT_ID = ?"

        const val STATEMENT_SELECT_DISTINCT_HANDLE =
            "SELECT DISTINCT ($COLUMN_ITEM_HANDLE)" +
                " FROM $TABLE_NAME_ITEM" +
                " ORDER BY $COLUMN_ITEM_HANDLE" +
                " LIMIT ? OFFSET ?;"

        const val STATEMENT_COUNT_DISTINCT_HANDLE =
            "SELECT COUNT(DISTINCT ($COLUMN_ITEM_HANDLE))" +
                " FROM $TABLE_NAME_ITEM;"

        const val STATEMENT_INSERT_ITEM =
            "INSERT INTO $TABLE_NAME_ITEM" +
                "($COLUMN_ITEM_HANDLE,$COLUMN_ITEM_RIGHT_ID,$COLUMN_ITEM_CREATED_BY," +
                "$COLUMN_ITEM_CREATED_ON,$COLUMN_ITEM_LAST_UPDATED_BY,$COLUMN_ITEM_LAST_UPDATED_ON)" +
                " VALUES(?,?,?," +
                "?,?,?)" +
                " ON CONFLICT ON CONSTRAINT $CONSTRAINT_ITEM_PKEY" +
                " DO UPDATE SET " +
                "$COLUMN_ITEM_LAST_UPDATED_BY = EXCLUDED.$COLUMN_ITEM_LAST_UPDATED_BY," +
                "$COLUMN_ITEM_LAST_UPDATED_ON = EXCLUDED.$COLUMN_ITEM_LAST_UPDATED_ON;"

        const val STATEMENT_DELETE_ITEM =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM i " +
                "WHERE i.$COLUMN_ITEM_RIGHT_ID = ? " +
                "AND i.$COLUMN_ITEM_HANDLE = ?"

        const val STATEMENT_DELETE_ITEM_BY_HANDLE =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM i " +
                "WHERE i.$COLUMN_ITEM_HANDLE = ?"

        const val STATEMENT_DELETE_ITEM_BY_RIGHT =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM i " +
                "WHERE i.$COLUMN_ITEM_RIGHT_ID = ?"

        const val STATEMENT_ITEM_CONTAINS_ENTRY =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE $COLUMN_ITEM_HANDLE=? AND $COLUMN_ITEM_RIGHT_ID=?)"

        const val STATEMENT_ITEM_CONTAINS_RIGHT =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE $COLUMN_ITEM_RIGHT_ID=?)"

        const val STATEMENT_GET_RIGHTS_IDS_FOR_METADATA =
            "SELECT $COLUMN_ITEM_RIGHT_ID,$COLUMN_ITEM_HANDLE,$COLUMN_ITEM_CREATED_BY," +
                "$COLUMN_ITEM_CREATED_ON,$COLUMN_ITEM_LAST_UPDATED_BY,$COLUMN_ITEM_LAST_UPDATED_ON" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE $COLUMN_ITEM_HANDLE = ? AND $COLUMN_ITEM_RIGHT_ID = ?;"

        const val STATEMENT_GET_RIGHTS_UPDATED_BEFORE_BY_RIGHT_ID =
            "SELECT $COLUMN_ITEM_RIGHT_ID,$COLUMN_ITEM_HANDLE,$COLUMN_ITEM_CREATED_BY," +
                "$COLUMN_ITEM_CREATED_ON,$COLUMN_ITEM_LAST_UPDATED_BY,$COLUMN_ITEM_LAST_UPDATED_ON" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE $COLUMN_ITEM_RIGHT_ID = ? AND $COLUMN_ITEM_LAST_UPDATED_ON < ?;"

        fun ResultSet.toItemRow(): ItemRow =
            ItemRow(
                rightId = this.getString(1),
                handle = this.getString(2),
                createdBy = this.getString(3),
                createdOn =
                    this.getTimestamp(4, RightDB.utcCalendar)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            TimezoneUtil.TIME_ZONE_UTC,
                        )
                    },
                lastUpdatedBy = this.getString(5),
                lastUpdatedOn =
                    this.getTimestamp(6, RightDB.utcCalendar)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            TimezoneUtil.TIME_ZONE_UTC,
                        )
                    },
            )
    }
}
