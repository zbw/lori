package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.LoriServerBackend
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemporalValidityFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement

/**
 * Execute SQL queries strongly related to bookmarks.
 *
 * Created on 03-15-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkDB(
    val connection: Connection,
    private val tracer: Tracer,
) {

    fun deleteBookmarkById(bookmarkId: Int): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_BOOKMARK_BY_ID).apply {
            this.setInt(1, bookmarkId)
        }
        val span = tracer.spanBuilder("deleteBookmarkById").startSpan()
        return try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun insertBookmark(bookmarkRest: Bookmark): Int {
        val prepStmt = insertUpdateSetParameters(
            bookmarkRest,
            connection.prepareStatement(STATEMENT_INSERT_BOOKMARK, Statement.RETURN_GENERATED_KEYS)
        )
        val span = tracer.spanBuilder("insertBookmark").startSpan()
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

    fun getBookmarksByIds(bookmarkIds: List<Int>): List<Bookmark> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_BOOKMARKS).apply {
            this.setArray(1, connection.createArrayOf("integer", bookmarkIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("getBookmarksByIds").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        return generateSequence {
            if (rs.next()) {
                extractBookmark(rs)
            } else null
        }.takeWhile { true }.toList()
    }

    fun updateBookmarkById(bookmarkId: Int, bookmark: Bookmark): Int {
        val prepStmt = insertUpdateSetParameters(
            bookmark,
            connection.prepareStatement(STATEMENT_UPDATE_BOOKMARK)
        ).apply {
            this.setInt(15, bookmarkId)
        }
        val span = tracer.spanBuilder("updateBookmarkById").startSpan()
        try {
            span.makeCurrent()
            return runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
    }

    fun getBookmarkList(
        limit: Int,
        offset: Int,
    ): List<Bookmark> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_BOOKMARK_LIST).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }

        val span = tracer.spanBuilder("getBookmarkList").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        return generateSequence {
            if (rs.next()) {
                extractBookmark(rs)
            } else null
        }.takeWhile { true }.toList()
    }


    companion object {
        private const val TABLE_NAME_BOOKMARK = "bookmark"
        private const val COLUMN_BOOKMARK_ID = "bookmark_id"

        const val STATEMENT_GET_BOOKMARKS = "SELECT " +
            "bookmark_id,bookmark_name,description,search_term," +
            "filter_publication_date,filter_access_state,filter_temporal_validity," +
            "filter_start_date,filter_end_date,filter_formal_rule," +
            "filter_valid_on,filter_paket_sigel,filter_zdb_id," +
            "filter_no_right_information,filter_publication_type" +
            " FROM $TABLE_NAME_BOOKMARK" +
            " WHERE $COLUMN_BOOKMARK_ID = ANY(?)"

        const val STATEMENT_INSERT_BOOKMARK = "INSERT INTO $TABLE_NAME_BOOKMARK" +
            "(bookmark_name,search_term,description,filter_publication_date," +
            "filter_access_state,filter_temporal_validity,filter_start_date," +
            "filter_end_date,filter_formal_rule,filter_valid_on," +
            "filter_paket_sigel,filter_zdb_id,filter_no_right_information," +
            "filter_publication_type)" +
            " VALUES(?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?,?," +
            "?,?)"

        const val STATEMENT_DELETE_BOOKMARK_BY_ID = "DELETE " +
            "FROM $TABLE_NAME_BOOKMARK" +
            " WHERE $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_UPDATE_BOOKMARK =
            "UPDATE $TABLE_NAME_BOOKMARK" +
                " SET bookmark_name=?,search_term=?,description=?,filter_publication_date=?," +
                "filter_access_state=?,filter_temporal_validity=?,filter_start_date=?," +
                "filter_end_date=?,filter_formal_rule=?,filter_valid_on=?," +
                "filter_paket_sigel=?,filter_zdb_id=?,filter_no_right_information=?," +
                "filter_publication_type=?" +
                " WHERE $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_GET_BOOKMARK_LIST = "SELECT" +
            " bookmark_id,bookmark_name,description,search_term," +
            "filter_publication_date,filter_access_state,filter_temporal_validity," +
            "filter_start_date,filter_end_date,filter_formal_rule," +
            "filter_valid_on,filter_paket_sigel,filter_zdb_id," +
            "filter_no_right_information,filter_publication_type" +
            " FROM $TABLE_NAME_BOOKMARK" +
            " ORDER BY $COLUMN_BOOKMARK_ID ASC LIMIT ? OFFSET ?;"

        private fun extractBookmark(rs: ResultSet): Bookmark =
            Bookmark(
                bookmarkId = rs.getInt(1),
                bookmarkName = rs.getString(2),
                description = rs.getString(3),
                searchPairs = LoriServerBackend.parseValidSearchPairs(rs.getString(4)),
                publicationDateFilter = PublicationDateFilter.fromString(rs.getString(5)),
                accessStateFilter = AccessStateFilter.fromString(rs.getString(6)),
                temporalValidityFilter = TemporalValidityFilter.fromString(rs.getString(7)),
                startDateFilter = StartDateFilter.fromString(rs.getString(8)),
                endDateFilter = EndDateFilter.fromString(rs.getString(9)),
                formalRuleFilter = FormalRuleFilter.fromString(rs.getString(10)),
                validOnFilter = RightValidOnFilter.fromString(rs.getString(11)),
                paketSigelFilter = PaketSigelFilter.fromString(rs.getString(12)),
                zdbIdFilter = ZDBIdFilter.fromString(rs.getString(13)),
                noRightInformationFilter = NoRightInformationFilter.fromString(rs.getBoolean(14).toString()),
                publicationTypeFilter = PublicationTypeFilter.fromString(rs.getString(15))
            )

        private fun insertUpdateSetParameters(
            bookmark: Bookmark,
            prepStmt: PreparedStatement,
        ): PreparedStatement {
            return prepStmt.apply {
                this.setString(1, bookmark.bookmarkName)
                this.setIfNotNull(2, bookmark.searchPairs) { value, idx, prepStmt ->
                    prepStmt.setString(idx, LoriServerBackend.searchPairsToString(value))
                }
                this.setIfNotNull(3, bookmark.description) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(4, bookmark.publicationDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(5, bookmark.accessStateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(6, bookmark.temporalValidityFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(7, bookmark.startDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(8, bookmark.endDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(9, bookmark.formalRuleFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(10, bookmark.validOnFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(11, bookmark.paketSigelFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(12, bookmark.zdbIdFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toString())
                }
                this.setIfNotNull(13, bookmark.noRightInformationFilter) { _, idx, prepStmt ->
                    prepStmt.setBoolean(idx, true)
                }
                this.setIfNotNull(14, bookmark.publicationTypeFilter?.toString()) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
            }
        }
    }
}
