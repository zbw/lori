package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.AccessStateOnDateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.ManualRightFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.PublicationYearFilter
import de.zbw.business.lori.server.RightIdFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SeriesFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.ZDBIdFilter
import de.zbw.business.lori.server.type.Bookmark
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.TABLE_NAME_BOOKMARK
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Tracer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Execute SQL queries strongly related to bookmarks.
 *
 * Created on 03-15-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class BookmarkDB(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
) {
    suspend fun deleteBookmarkById(bookmarkId: Int): Int =
        connectionPool.useConnection("deleteBookmarkById") { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_DELETE_BOOKMARK_BY_ID).apply {
                    this.setInt(1, bookmarkId)
                }
            val span = tracer.spanBuilder("deleteBookmarkById").startSpan()
            return@useConnection try {
                span.makeCurrent()
                runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun insertBookmark(bookmark: Bookmark): Int =
        connectionPool.useConnection("insertBookmark") { connection ->
            val prepStmt =
                insertUpdateSetParameters(
                    bookmark,
                    connection.prepareStatement(STATEMENT_INSERT_BOOKMARK, Statement.RETURN_GENERATED_KEYS),
                )
            val span = tracer.spanBuilder("insertBookmark").startSpan()
            try {
                span.makeCurrent()
                val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
                return@useConnection if (affectedRows > 0) {
                    val rs: ResultSet = prepStmt.generatedKeys
                    rs.next()
                    rs.getInt(1)
                } else {
                    throw IllegalStateException("No row has been inserted.")
                }
            } finally {
                span.end()
            }
        }

    suspend fun getBookmarksByIds(bookmarkIds: List<Int>): List<Bookmark> =
        connectionPool.useConnection("getBookmarksByIds") { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_BOOKMARKS).apply {
                    this.setArray(1, connection.createArrayOf("integer", bookmarkIds.toTypedArray()))
                }
            val span = tracer.spanBuilder("getBookmarksByIds").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            return@useConnection generateSequence {
                if (rs.next()) {
                    extractBookmark(rs)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun updateBookmarkById(
        bookmarkId: Int,
        bookmark: Bookmark,
    ): Int =
        connectionPool.useConnection("updateBookmarkById") { connection ->
            val prepStmt =
                insertUpdateSetParameters(
                    bookmark,
                    connection.prepareStatement(STATEMENT_UPDATE_BOOKMARK),
                ).apply {
                    this.setInt(24, bookmarkId)
                }
            val span = tracer.spanBuilder("updateBookmarkById").startSpan()
            try {
                span.makeCurrent()
                return@useConnection runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            } finally {
                span.end()
            }
        }

    suspend fun getBookmarkList(
        limit: Int,
        offset: Int,
    ): List<Bookmark> =
        connectionPool.useConnection("getBookmarkList") { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_BOOKMARK_LIST).apply {
                    this.setInt(1, limit)
                    this.setInt(2, offset)
                }

            val span = tracer.spanBuilder("getBookmarkList").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }

            return@useConnection generateSequence {
                if (rs.next()) {
                    extractBookmark(rs)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    suspend fun getBookmarkIdByQuerystring(query: String): List<Int> =
        connectionPool.useConnection("getBookmarkNamesByQuerystring") { connection ->
            val prepStmt =
                connection.prepareStatement(STATEMENT_GET_ID_BY_QUERYSTRING).apply {
                    this.setString(1, query)
                }

            val span = tracer.spanBuilder("getBookmarkIdsByQuerystring").startSpan()
            val rs =
                try {
                    span.makeCurrent()
                    runInTransaction(connection) { prepStmt.executeQuery() }
                } finally {
                    span.end()
                }
            return@useConnection generateSequence {
                if (rs.next()) {
                    rs.getInt(1)
                } else {
                    null
                }
            }.takeWhile { true }.toList()
        }

    companion object {
        private const val COLUMN_BOOKMARK_ID = "bookmark_id"
        private const val COLUMN_BOOKMARK_NAME = "bookmark_name"
        const val COLUMN_FILTER_LICENCE_URL = "filter_licence_url"
        const val COLUMN_FILTER_MANUAL_RIGHT = "filter_manual_right"
        const val COLUMN_FILTER_ACCESS_STATE_ON = "filter_access_state_on"
        const val COLUMN_QUERYSTRING = "querystring"

        const val STATEMENT_GET_BOOKMARKS =
            "SELECT " +
                "$COLUMN_BOOKMARK_ID,$COLUMN_BOOKMARK_NAME,description,search_term," +
                "filter_publication_year,filter_access_state," +
                "filter_start_date,filter_end_date,filter_formal_rule," +
                "filter_valid_on,filter_paket_sigel,filter_zdb_id," +
                "filter_no_right_information,filter_publication_type," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL," +
                "$COLUMN_FILTER_MANUAL_RIGHT,$COLUMN_FILTER_ACCESS_STATE_ON,$COLUMN_QUERYSTRING" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " WHERE $COLUMN_BOOKMARK_ID = ANY(?)"

        const val STATEMENT_INSERT_BOOKMARK =
            "INSERT INTO $TABLE_NAME_BOOKMARK" +
                "($COLUMN_BOOKMARK_NAME,search_term,description,filter_publication_year," +
                "filter_access_state,filter_start_date," +
                "filter_end_date,filter_formal_rule,filter_valid_on," +
                "filter_paket_sigel,filter_zdb_id,filter_no_right_information," +
                "filter_publication_type,created_on,last_updated_on,created_by," +
                "last_updated_by,filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL," +
                "$COLUMN_FILTER_MANUAL_RIGHT,$COLUMN_FILTER_ACCESS_STATE_ON,$COLUMN_QUERYSTRING" +
                ")" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?)"

        const val STATEMENT_DELETE_BOOKMARK_BY_ID =
            "DELETE" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " WHERE $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_UPDATE_BOOKMARK =
            "INSERT INTO $TABLE_NAME_BOOKMARK" +
                "($COLUMN_BOOKMARK_NAME,search_term,description,filter_publication_year," +
                "filter_access_state,filter_start_date," +
                "filter_end_date,filter_formal_rule,filter_valid_on," +
                "filter_paket_sigel,filter_zdb_id,filter_no_right_information," +
                "filter_publication_type,created_on,last_updated_on," +
                "created_by,last_updated_by," +
                "filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL," +
                "$COLUMN_FILTER_MANUAL_RIGHT,$COLUMN_FILTER_ACCESS_STATE_ON," +
                "$COLUMN_QUERYSTRING,$COLUMN_BOOKMARK_ID)" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?," +
                "?)" +
                " ON CONFLICT ($COLUMN_BOOKMARK_ID)" +
                " DO UPDATE SET" +
                " $COLUMN_BOOKMARK_NAME = EXCLUDED.$COLUMN_BOOKMARK_NAME," +
                " search_term = EXCLUDED.search_term," +
                " description = EXCLUDED.description," +
                " filter_publication_year = EXCLUDED.filter_publication_year," +
                " filter_access_state = EXCLUDED.filter_access_state," +
                " filter_start_date = EXCLUDED.filter_start_date," +
                " filter_end_date = EXCLUDED.filter_end_date," +
                " filter_formal_rule = EXCLUDED.filter_formal_rule," +
                " filter_valid_on = EXCLUDED.filter_valid_on," +
                " filter_paket_sigel = EXCLUDED.filter_paket_sigel," +
                " filter_zdb_id = EXCLUDED.filter_zdb_id," +
                " filter_no_right_information = EXCLUDED.filter_no_right_information," +
                " filter_publication_type = EXCLUDED.filter_publication_type," +
                " last_updated_on = EXCLUDED.last_updated_on," +
                " last_updated_by = EXCLUDED.last_updated_by," +
                " filter_series = EXCLUDED.filter_series," +
                " filter_template_name = EXCLUDED.filter_template_name," +
                " $COLUMN_FILTER_LICENCE_URL = EXCLUDED.$COLUMN_FILTER_LICENCE_URL," +
                " $COLUMN_FILTER_MANUAL_RIGHT = EXCLUDED.$COLUMN_FILTER_MANUAL_RIGHT," +
                " $COLUMN_QUERYSTRING = EXCLUDED.$COLUMN_QUERYSTRING," +
                " $COLUMN_FILTER_ACCESS_STATE_ON = EXCLUDED.$COLUMN_FILTER_ACCESS_STATE_ON;"

        const val STATEMENT_GET_BOOKMARK_LIST =
            "SELECT" +
                " $COLUMN_BOOKMARK_ID,$COLUMN_BOOKMARK_NAME,description,search_term," +
                "filter_publication_year,filter_access_state," +
                "filter_start_date,filter_end_date,filter_formal_rule," +
                "filter_valid_on,filter_paket_sigel,filter_zdb_id," +
                "filter_no_right_information,filter_publication_type," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL," +
                "$COLUMN_FILTER_MANUAL_RIGHT,$COLUMN_FILTER_ACCESS_STATE_ON,$COLUMN_QUERYSTRING" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " ORDER BY created_on DESC LIMIT ? OFFSET ?;"

        const val STATEMENT_GET_ID_BY_QUERYSTRING =
            "SELECT $COLUMN_BOOKMARK_ID" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " WHERE $COLUMN_QUERYSTRING = ?;"

        private fun extractBookmark(rs: ResultSet): Bookmark {
            var localCounter = 1
            return Bookmark(
                bookmarkId = rs.getInt(localCounter++),
                bookmarkName = rs.getString(localCounter++),
                description = rs.getString(localCounter++),
                searchTerm = rs.getString(localCounter++),
                publicationYearFilter = PublicationYearFilter.fromString(rs.getString(localCounter++)),
                accessStateFilter = AccessStateFilter.fromString(rs.getString(localCounter++)),
                startDateFilter = StartDateFilter.fromString(rs.getString(localCounter++)),
                endDateFilter = EndDateFilter.fromString(rs.getString(localCounter++)),
                formalRuleFilter = FormalRuleFilter.fromString(rs.getString(localCounter++)),
                validOnFilter = RightValidOnFilter.fromString(rs.getString(localCounter++)),
                paketSigelFilter = PaketSigelFilter.fromString(rs.getString(localCounter++)),
                zdbIdFilter = ZDBIdFilter.fromString(rs.getString(localCounter++)),
                noRightInformationFilter =
                    NoRightInformationFilter.fromString(
                        rs.getBoolean(localCounter++).toString(),
                    ),
                publicationTypeFilter = PublicationTypeFilter.fromString(rs.getString(localCounter++)),
                createdOn =
                    rs.getTimestamp(localCounter++)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                lastUpdatedOn =
                    rs.getTimestamp(localCounter++)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                createdBy = rs.getString(localCounter++),
                lastUpdatedBy = rs.getString(localCounter++),
                seriesFilter = SeriesFilter.fromString(rs.getString(localCounter++)),
                rightIdFilter = RightIdFilter.fromString(rs.getString(localCounter++)),
                licenceURLFilter = LicenceUrlFilter.fromString(rs.getString(localCounter++)),
                manualRightFilter = ManualRightFilter.fromString(rs.getBoolean(localCounter++).toString()),
                accessStateOnFilter = AccessStateOnDateFilter.fromString(rs.getString(localCounter++)),
                queryString = rs.getString(localCounter++),
            )
        }

        private fun insertUpdateSetParameters(
            bookmark: Bookmark,
            prepStmt: PreparedStatement,
        ): PreparedStatement {
            val now = Instant.now()
            var localCounter = 1
            return prepStmt.apply {
                this.setString(localCounter++, bookmark.bookmarkName)
                this.setIfNotNull(localCounter++, bookmark.searchTerm) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, bookmark.description) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, bookmark.publicationYearFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.accessStateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.startDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.endDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.formalRuleFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.validOnFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.paketSigelFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.zdbIdFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.noRightInformationFilter) { _, idx, prepStmt ->
                    prepStmt.setBoolean(idx, true)
                }
                this.setIfNotNull(localCounter++, bookmark.publicationTypeFilter?.toSQLString()) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setTimestamp(localCounter++, Timestamp.from(now))
                this.setTimestamp(localCounter++, Timestamp.from(now))
                this.setIfNotNull(localCounter++, bookmark.createdBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, bookmark.lastUpdatedBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(localCounter++, bookmark.seriesFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.rightIdFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.licenceURLFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(localCounter++, bookmark.manualRightFilter) { _, idx, prepStmt ->
                    prepStmt.setBoolean(idx, true)
                }
                this.setIfNotNull(localCounter++, bookmark.accessStateOnFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setString(localCounter++, bookmark.computeQueryString())
            }
        }
    }
}
