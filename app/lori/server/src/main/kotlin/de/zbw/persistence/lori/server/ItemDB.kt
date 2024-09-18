package de.zbw.persistence.lori.server

import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_METADATA_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.COLUMN_RIGHT_ID
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_ITEM
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet
import java.sql.Statement

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
    suspend fun getRightIdsByMetadataId(metadataId: String): List<String> =
        connectionPool.useConnection { connection ->
            val span = tracer.spanBuilder("getRightIdsByMetadataId").startSpan()
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_RIGHT_IDS_BY_METADATA_ID).apply {
                    this.setString(1, metadataId)
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

    suspend fun itemContainsEntry(
        metadataId: String,
        rightId: String,
    ): Boolean =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_ITEM_CONTAINS_ENTRY).apply {
                    this.setString(1, metadataId)
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
        metadataId: String,
        rightId: String,
    ): String? =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_INSERT_ITEM, Statement.RETURN_GENERATED_KEYS).apply {
                    this.setString(1, metadataId)
                    this.setString(2, rightId)
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

    suspend fun deleteItem(
        metadataId: String,
        rightId: String,
    ): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ITEM).apply {
                    this.setString(1, rightId)
                    this.setString(2, metadataId)
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

    suspend fun deleteItemByMetadataId(metadataId: String): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_ITEM_BY_METADATA).apply {
                    this.setString(1, metadataId)
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
        const val CONSTRAINT_ITEM_PKEY = "item_pkey"
        const val STATEMENT_COUNT_ITEM_BY_RIGHTID =
            "SELECT COUNT(*) " +
                "FROM $TABLE_NAME_ITEM " +
                "WHERE $COLUMN_RIGHT_ID = ?;"

        const val STATEMENT_GET_RIGHT_IDS_BY_METADATA_ID =
            "SELECT $COLUMN_RIGHT_ID" +
                " FROM $TABLE_NAME_ITEM" +
                " WHERE $COLUMN_METADATA_ID = ?"

        const val STATEMENT_INSERT_ITEM =
            "INSERT INTO $TABLE_NAME_ITEM" +
                "($COLUMN_METADATA_ID, $COLUMN_RIGHT_ID)" +
                " VALUES(?,?)" +
                " ON CONFLICT ON CONSTRAINT $CONSTRAINT_ITEM_PKEY" +
                " DO NOTHING;"

        const val STATEMENT_DELETE_ITEM =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM i " +
                "WHERE i.$COLUMN_RIGHT_ID = ? " +
                "AND i.$COLUMN_METADATA_ID = ?"

        const val STATEMENT_DELETE_ITEM_BY_METADATA =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM i " +
                "WHERE i.$COLUMN_METADATA_ID = ?"

        const val STATEMENT_DELETE_ITEM_BY_RIGHT =
            "DELETE " +
                "FROM $TABLE_NAME_ITEM i " +
                "WHERE i.$COLUMN_RIGHT_ID = ?"

        const val STATEMENT_ITEM_CONTAINS_ENTRY =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE $COLUMN_METADATA_ID=? AND $COLUMN_RIGHT_ID=?)"

        const val STATEMENT_ITEM_CONTAINS_RIGHT =
            "SELECT EXISTS(SELECT 1 from $TABLE_NAME_ITEM WHERE $COLUMN_RIGHT_ID=?)"
    }
}
