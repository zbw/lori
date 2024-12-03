package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.AccessStateFilter
import de.zbw.business.lori.server.EndDateFilter
import de.zbw.business.lori.server.FormalRuleFilter
import de.zbw.business.lori.server.LicenceUrlFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.PaketSigelFilter
import de.zbw.business.lori.server.PublicationDateFilter
import de.zbw.business.lori.server.PublicationTypeFilter
import de.zbw.business.lori.server.RightValidOnFilter
import de.zbw.business.lori.server.SeriesFilter
import de.zbw.business.lori.server.StartDateFilter
import de.zbw.business.lori.server.TemplateNameFilter
import de.zbw.business.lori.server.TemporalValidityFilter
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
        connectionPool.useConnection { connection ->
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

    suspend fun insertBookmark(bookmarkRest: Bookmark): Int =
        connectionPool.useConnection { connection ->
            val prepStmt =
                insertUpdateSetParameters(
                    bookmarkRest,
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
        connectionPool.useConnection { connection ->
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
        connectionPool.useConnection { connection ->
            val prepStmt =
                insertUpdateSetParameters(
                    bookmark,
                    connection.prepareStatement(STATEMENT_UPDATE_BOOKMARK),
                ).apply {
                    this.setInt(22, bookmarkId)
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
        connectionPool.useConnection { connection ->
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

    companion object {
        private const val COLUMN_BOOKMARK_ID = "bookmark_id"
        const val COLUMN_FILTER_LICENCE_URL = "filter_licence_url"

        const val STATEMENT_GET_BOOKMARKS =
            "SELECT " +
                "bookmark_id,bookmark_name,description,search_term," +
                "filter_publication_date,filter_access_state,filter_temporal_validity," +
                "filter_start_date,filter_end_date,filter_formal_rule," +
                "filter_valid_on,filter_paket_sigel,filter_zdb_id," +
                "filter_no_right_information,filter_publication_type," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " WHERE $COLUMN_BOOKMARK_ID = ANY(?)"

        const val STATEMENT_INSERT_BOOKMARK =
            "INSERT INTO $TABLE_NAME_BOOKMARK" +
                "(bookmark_name,search_term,description,filter_publication_date," +
                "filter_access_state,filter_temporal_validity,filter_start_date," +
                "filter_end_date,filter_formal_rule,filter_valid_on," +
                "filter_paket_sigel,filter_zdb_id,filter_no_right_information," +
                "filter_publication_type,created_on,last_updated_on,created_by," +
                "last_updated_by,filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL" +
                ")" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?)"

        const val STATEMENT_DELETE_BOOKMARK_BY_ID =
            "DELETE" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " WHERE $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_UPDATE_BOOKMARK =
            "INSERT INTO $TABLE_NAME_BOOKMARK" +
                "(bookmark_name,search_term,description,filter_publication_date," +
                "filter_access_state,filter_temporal_validity,filter_start_date," +
                "filter_end_date,filter_formal_rule,filter_valid_on," +
                "filter_paket_sigel,filter_zdb_id,filter_no_right_information," +
                "filter_publication_type,created_on,last_updated_on," +
                "created_by,last_updated_by," +
                "filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL,$COLUMN_BOOKMARK_ID)" +
                " VALUES(?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?,?,?," +
                "?)" +
                " ON CONFLICT ($COLUMN_BOOKMARK_ID)" +
                " DO UPDATE SET" +
                " bookmark_name = EXCLUDED.bookmark_name," +
                " search_term = EXCLUDED.search_term," +
                " description = EXCLUDED.description," +
                " filter_publication_date = EXCLUDED.filter_publication_date," +
                " filter_access_state = EXCLUDED.filter_access_state," +
                " filter_temporal_validity = EXCLUDED.filter_temporal_validity," +
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
                " $COLUMN_FILTER_LICENCE_URL = EXCLUDED.$COLUMN_FILTER_LICENCE_URL;"

        const val STATEMENT_GET_BOOKMARK_LIST =
            "SELECT" +
                " bookmark_id,bookmark_name,description,search_term," +
                "filter_publication_date,filter_access_state,filter_temporal_validity," +
                "filter_start_date,filter_end_date,filter_formal_rule," +
                "filter_valid_on,filter_paket_sigel,filter_zdb_id," +
                "filter_no_right_information,filter_publication_type," +
                "created_on,last_updated_on,created_by,last_updated_by," +
                "filter_series,filter_template_name,$COLUMN_FILTER_LICENCE_URL" +
                " FROM $TABLE_NAME_BOOKMARK" +
                " ORDER BY created_on DESC LIMIT ? OFFSET ?;"

        private fun extractBookmark(rs: ResultSet): Bookmark =
            Bookmark(
                bookmarkId = rs.getInt(1),
                bookmarkName = rs.getString(2),
                description = rs.getString(3),
                searchTerm = rs.getString(4),
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
                publicationTypeFilter = PublicationTypeFilter.fromString(rs.getString(15)),
                createdOn =
                    rs.getTimestamp(16)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                lastUpdatedOn =
                    rs.getTimestamp(17)?.let {
                        OffsetDateTime.ofInstant(
                            it.toInstant(),
                            ZoneId.of("UTC+00:00"),
                        )
                    },
                createdBy = rs.getString(18),
                lastUpdatedBy = rs.getString(19),
                seriesFilter = SeriesFilter.fromString(rs.getString(20)),
                templateNameFilter = TemplateNameFilter.fromString(rs.getString(21)),
                licenceURLFilter = LicenceUrlFilter.fromString(rs.getString(22)),
            )

        private fun insertUpdateSetParameters(
            bookmark: Bookmark,
            prepStmt: PreparedStatement,
        ): PreparedStatement {
            val now = Instant.now()
            return prepStmt.apply {
                this.setString(1, bookmark.bookmarkName)
                this.setIfNotNull(2, bookmark.searchTerm) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(3, bookmark.description) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(4, bookmark.publicationDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(5, bookmark.accessStateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(6, bookmark.temporalValidityFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(7, bookmark.startDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(8, bookmark.endDateFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(9, bookmark.formalRuleFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(10, bookmark.validOnFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(11, bookmark.paketSigelFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(12, bookmark.zdbIdFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(13, bookmark.noRightInformationFilter) { _, idx, prepStmt ->
                    prepStmt.setBoolean(idx, true)
                }
                this.setIfNotNull(14, bookmark.publicationTypeFilter?.toSQLString()) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setTimestamp(15, Timestamp.from(now))
                this.setTimestamp(16, Timestamp.from(now))
                this.setIfNotNull(17, bookmark.createdBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(18, bookmark.lastUpdatedBy) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value)
                }
                this.setIfNotNull(19, bookmark.seriesFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(20, bookmark.templateNameFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
                this.setIfNotNull(21, bookmark.licenceURLFilter) { value, idx, prepStmt ->
                    prepStmt.setString(idx, value.toSQLString())
                }
            }
        }
    }
}
