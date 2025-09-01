package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.ItemId
import de.zbw.business.lori.server.utils.TimezoneUtil
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone

/**
 * Execute SQL queries strongly related to items.
 *
 * Created on 03-17-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ItemDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    suspend fun getRightIdsByHandle(handle: String): List<String> =
        connectionPool.useConnection { connection ->
            val span = tracer.spanBuilder("getRightIdsByHandle").startSpan()
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_RIGHT_IDS_BY_HANDLE_ID).apply {
                    this.setString(1, handle)
                }
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

    suspend fun getHandlesByRightId(rightId: String): List<String> =
        connectionPool.useConnection { connection ->
            val span = tracer.spanBuilder("getHandlesByRightId").startSpan()
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_HANDLES_BY_RIGHT_ID).apply {
                    this.setString(1, rightId)
                }
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

    suspend fun getAllHandles(): List<String> =
        connectionPool.useConnection { connection ->
            val span = tracer.spanBuilder("getAllHandles").startSpan()
            val prepStmt = connection.prepareStatement(STATEMENT_SELECT_DISTINCT_HANDLE)
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

    suspend fun itemContainsEntry(
        handle: String,
        rightId: String,
    ): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_ITEM_CONTAINS_ENTRY).apply {
                    this.setString(1, handle)
                    this.setString(2, rightId)
                }
            val span = tracer.spanBuilder("itemContainsEntry").startSpan()
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

    /**
     * Check if a given rightId is still used in the table.
     */
    suspend fun itemContainsRightId(rightId: String): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_ITEM_CONTAINS_RIGHT).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("itemContainsRight").startSpan()
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

    suspend fun insertItem(
        itemId: ItemId,
        createdBy: String,
    ): String? =
        connectionPool.useConnection { connection ->
            val now = Instant.now()
            var localCounter = 1
            val prepStmt =
                connection.prepareStatement(STATEMENT_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS).apply {
                    this.setString(localCounter++, itemId.handle)
                    this.setString(localCounter++, itemId.rightId)
                    this.setString(localCounter++, createdBy)
                    this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                    this.setString(localCounter++, createdBy)
                    this.setTimestamp(localCounter++, Timestamp.from(now), utcCalendar)
                }

            val span = tracer.spanBuilder("insertItem").startSpan()
            try {
                span.makeCurrent()
                val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
                return@useConnection affectedRows.takeIf { it > 0 }?.let {
                    val rs: ResultSet = prepStmt.generatedKeys
                    rs.next()
                    rs.getString(1)
                }
            } finally {
                span.end()
            }
        }

    suspend fun upsertItemBatch(
        itemIds: List<ItemId>,
        createdBy: String,
    ): IntArray =
        connectionPool.useConnection { connection ->
            val now = Instant.now()
            var localCounter = 1
            val prep = connection.prepareStatement(STATEMENT_INSERT_ITEM)
            itemIds.map {
                val p =
                    prep.apply {
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
            val span = tracer.spanBuilder("insertItemBatch").startSpan()
            try {
                span.makeCurrent()
                runInTransaction(connection) {
                    prep.executeBatch()
                }
            } finally {
                span.end()
            }
        }

    suspend fun deleteItem(
        handle: String,
        rightId: String,
    ): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ITEM).apply {
                    this.setString(1, rightId)
                    this.setString(2, handle)
                }
            val span = tracer.spanBuilder("deleteItem").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun countItemByRightId(rightId: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_COUNT_ITEM_BY_RIGHTID).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("countItemByRightId").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.run { this.executeQuery() } }
                } finally {
                    span.end()
                }
            if (rs.next()) {
                return@useConnection rs.getInt(1)
            } else {
                throw IllegalStateException("No count found.")
            }
        }

    suspend fun deleteItemByHandle(handle: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_HANDLE).apply {
                    this.setString(1, handle)
                }
            val span = tracer.spanBuilder("deleteItem").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun deleteItemByRightId(rightId: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_RIGHT).apply {
                    this.setString(1, rightId)
                }
            val span = tracer.spanBuilder("deleteItem").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

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
                "FROM $TABLE_NAME_ITEM;"

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
    }
}
