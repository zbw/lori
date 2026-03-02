package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.BookmarkTemplate
import io.opentelemetry.api.trace.Tracer
import java.sql.ResultSet

/**
 * Execute SQL queries related to templates.
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkTemplateDB(
    connectionPool: ConnectionPool,
    tracer: Tracer,
) : AbstractDB(
        connectionPool = connectionPool,
        tracer = tracer,
        tableName = TABLE_NAME_TEMPLATE_BOOKMARK_MAP,
    ) {
    /**
     * Queries on Template-Bookmark Pairs Table.
     */
    suspend fun deletePairsByRightId(rightId: String): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql = STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR_BY_TEMP,
            tracer = tracer,
            spanName = "deletePairsByRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
        )

    /**
     * Get all bookmark ids that are connected to a given RightId.
     */
    suspend fun getBookmarkIdsByRightId(rightId: String): List<Int> =
        DatabaseConnector.select(
            sql = STATEMENT_GET_BOOKMARKS_BY_RIGHT_ID,
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "getBookmarkIdsByRightId",
            params = { stmt ->
                stmt.setString(1, rightId)
            },
            mapper = { rs: ResultSet -> rs.getInt(1) },
        )

    suspend fun getBookmarkIdsByRightIds(rightIds: List<String>): Set<Int> =
        DatabaseConnector
            .select(
                sql = STATEMENT_GET_BOOKMARKS_BY_RIGHT_IDS,
                connectionPool = connectionPool,
                tracer = tracer,
                spanName = "getBookmarkIdsByRightIds",
                params = { stmt ->
                    stmt.setArray(1, stmt.connection.createArrayOf("text", rightIds.toTypedArray()))
                },
                mapper = { rs: ResultSet -> rs.getInt(1) },
            ).toSet()

    /**
     * Get all bookmark ids that are connected to a given template-id.
     */
    suspend fun getRightIdsByBookmarkId(bookmarkId: Int): List<String> =
        DatabaseConnector.select(
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "getRightIdsByBookmarkId",
            sql = STATEMENT_GET_TEMPLATES_BY_BOOKMARK_ID,
            params = { stmt ->
                stmt.setInt(1, bookmarkId)
            },
            mapper = { rs: ResultSet -> rs.getString(1) },
        )

    suspend fun insertTemplateBookmarkPair(bookmarkTemplate: BookmarkTemplate): Int =
        DatabaseConnector
            .insertReturningKeys(
                connectionPool = connectionPool,
                tracer = tracer,
                spanName = "insertTemplateBookmarkPair",
                sql = STATEMENT_INSERT_TEMPLATE_BOOKMARK_PAIR,
                params = { stmt ->
                    stmt.setString(1, bookmarkTemplate.rightId)
                    stmt.setInt(2, bookmarkTemplate.bookmarkId)
                },
                fetchGenerated = { rs ->
                    rs.getInt(1)
                },
            ).first()

    suspend fun deleteTemplateBookmarkPair(bookmarkTemplate: BookmarkTemplate): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "deleteTemplateBookmarkPair",
            sql = STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR,
            params = { stmt ->
                stmt.setString(1, bookmarkTemplate.rightId)
                stmt.setInt(2, bookmarkTemplate.bookmarkId)
            },
        )

    suspend fun upsertTemplateBookmarkBatch(bookmarkTemplates: List<BookmarkTemplate>): List<BookmarkTemplate> =
        DatabaseConnector.insertBatchReturningKeys(
            connectionPool = connectionPool,
            tracer = tracer,
            spanName = "upsertTemplateBookmarkBatch",
            sql = STATEMENT_UPSERT_TEMPLATE_BOOKMARK_PAIR,
            params = { stmt ->
                bookmarkTemplates.map { bookmarkTemplate ->
                    val p =
                        stmt.apply {
                            this.setInt(1, bookmarkTemplate.bookmarkId)
                            this.setString(2, bookmarkTemplate.rightId)
                        }
                    p.addBatch()
                }
            },
            fetchGenerated = { rs ->
                BookmarkTemplate(
                    bookmarkId = rs.getInt(1),
                    rightId = rs.getString(2),
                )
            },
        )

    companion object {
        private const val TABLE_NAME_TEMPLATE_BOOKMARK_MAP = "template_bookmark_map"
        private const val COLUMN_RIGHT_ID = "right_id"
        private const val COLUMN_BOOKMARK_ID = "bookmark_id"
        private const val CONSTRAINT_TEMPLATE_BOOKMARK_MAP = "template_bookmark_map_pkey"

        const val STATEMENT_GET_BOOKMARKS_BY_RIGHT_ID =
            "SELECT $COLUMN_BOOKMARK_ID" +
                " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " WHERE $COLUMN_RIGHT_ID = ?"

        const val STATEMENT_GET_BOOKMARKS_BY_RIGHT_IDS =
            "SELECT $COLUMN_BOOKMARK_ID" +
                " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " WHERE $COLUMN_RIGHT_ID = ANY(?)"

        const val STATEMENT_GET_TEMPLATES_BY_BOOKMARK_ID =
            "SELECT $COLUMN_RIGHT_ID" +
                " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " WHERE $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_INSERT_TEMPLATE_BOOKMARK_PAIR =
            "INSERT INTO $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " ($COLUMN_RIGHT_ID, $COLUMN_BOOKMARK_ID)" +
                " VALUES(?,?)"

        const val STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR =
            "DELETE" +
                " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " WHERE $COLUMN_RIGHT_ID = ? AND $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR_BY_TEMP =
            "DELETE " +
                "FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " WHERE $COLUMN_RIGHT_ID = ?"

        const val STATEMENT_UPSERT_TEMPLATE_BOOKMARK_PAIR =
            "INSERT INTO $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " ($COLUMN_BOOKMARK_ID, $COLUMN_RIGHT_ID)" +
                " VALUES(?,?)" +
                " ON CONFLICT ON CONSTRAINT $CONSTRAINT_TEMPLATE_BOOKMARK_MAP" +
                " DO NOTHING"
    }
}
