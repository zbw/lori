package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Execute SQL queries related to templates.
 * // TODO(CB): Decide if this class should be merged into [RightDB].
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkTemplateDB(
    val connection: Connection,
    private val tracer: Tracer,
) {
    /**
     * Queries on Template-Bookmark Pairs Table.
     */
    fun deletePairsByRightId(rightId: String): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR_BY_TEMP).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("deleteTemplateBookmarkPairByTempId").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    /**
     * Get all bookmark ids that are a connected to a given RightId.
     */
    fun getBookmarkIdsByRightId(rightId: String): List<Int> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_BOOKMARKS_BY_RIGHT_ID).apply {
            this.setString(1, rightId)
        }
        val span = tracer.spanBuilder("getBookmarkIdsByRightId").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        return generateSequence {
            if (rs.next()) {
                rs.getInt(1)
            } else null
        }.takeWhile { true }.toList()
    }

    fun getBookmarkIdsByRightIds(rightIds: List<String>): Set<Int> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_BOOKMARKS_BY_RIGHT_IDS).apply {
            this.setArray(1, connection.createArrayOf("text", rightIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("getBookmarkIdsByRightIds").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        return generateSequence {
            if (rs.next()) {
                rs.getInt(1)
            } else null
        }.takeWhile { true }.toSet()
    }

    /**
     * Get all bookmark ids that are a connected to a given template-id.
     */
    fun getRightIdsByBookmarkId(bookmarkId: Int): List<String> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_TEMPLATES_BY_BOOKMARK_ID).apply {
            this.setInt(1, bookmarkId)
        }
        val span = tracer.spanBuilder("getTemplateIdsByBookmarkId").startSpan()
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

    fun insertTemplateBookmarkPair(
        bookmarkTemplate: BookmarkTemplate,
    ): Int {
        val prepStmt = connection
            .prepareStatement(STATEMENT_INSERT_TEMPLATE_BOOKMARK_PAIR, Statement.RETURN_GENERATED_KEYS)
            .apply {
                this.setString(1, bookmarkTemplate.rightId)
                this.setInt(2, bookmarkTemplate.bookmarkId)
            }
        val span = tracer.spanBuilder("insertTemplateBookmarkPair").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                rs.getInt(1)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun deleteTemplateBookmarkPair(bookmarkTemplate: BookmarkTemplate): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR).apply {
            this.setString(1, bookmarkTemplate.rightId)
            this.setInt(2, bookmarkTemplate.bookmarkId)
        }
        val span = tracer.spanBuilder("deleteTemplateBookmarkPair").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun upsertTemplateBookmarkBatch(
        bookmarkTemplates: List<BookmarkTemplate>
    ): List<BookmarkTemplate> {
        val span = tracer.spanBuilder("upsertBookmarkTemplateBatch").startSpan()
        val prep = connection.prepareStatement(STATEMENT_UPSERT_TEMPLATE_BOOKMARK_PAIR, Statement.RETURN_GENERATED_KEYS)
        bookmarkTemplates.map { bookmarkTemplate ->
            val p = prep.apply {
                this.setInt(1, bookmarkTemplate.bookmarkId)
                this.setString(2, bookmarkTemplate.rightId)
            }
            p.addBatch()
        }
        try {
            span.makeCurrent()
            runInTransaction(connection) { prep.executeBatch() }.sum()
        } finally {
            span.end()
        }
        val rs: ResultSet = prep.generatedKeys
        return generateSequence {
            if (rs.next()) {
                BookmarkTemplate(
                    bookmarkId = rs.getInt(1),
                    rightId = rs.getString(2),
                )
            } else null
        }.takeWhile { true }.toList()
    }

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

        const val STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR = "DELETE" +
            " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
            " WHERE $COLUMN_RIGHT_ID = ? AND $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR_BY_TEMP = "DELETE " +
            "FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
            " WHERE $COLUMN_RIGHT_ID = ?"

        const val STATEMENT_UPSERT_TEMPLATE_BOOKMARK_PAIR = "INSERT INTO $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
            " ($COLUMN_BOOKMARK_ID, $COLUMN_RIGHT_ID)" +
            " VALUES(?,?)" +
            " ON CONFLICT ON CONSTRAINT $CONSTRAINT_TEMPLATE_BOOKMARK_MAP" +
            " DO NOTHING"
    }
}
