package de.zbw.persistence.lori.server

import de.zbw.business.lori.server.type.BookmarkTemplate
import de.zbw.business.lori.server.type.ItemRight
import de.zbw.business.lori.server.type.Template
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.runInTransaction
import de.zbw.persistence.lori.server.DatabaseConnector.Companion.setIfNotNull
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Execute SQL queries related to templates.
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class TemplateDB(
    val connection: Connection,
    private val tracer: Tracer,
    private val rightDB: RightDB,
) {
    fun deleteTemplateById(templateId: Int): Int {
        val span = tracer.spanBuilder("deleteTemplateById").startSpan()

        // Get Right-Id first
        val rightId: String? = getTemplateTransientById(templateId, span)?.rightId

        // Delete Template
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_TEMPLATE_BY_ID).apply {
            this.setInt(1, templateId)
        }
        val deleteTemplates = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }

        // Delete right information
        rightDB.deleteRightsByIds(listOfNotNull(rightId))
        return deleteTemplates
    }

    fun getTemplatesByIds(templateIds: List<Int>): List<Template> {
        if (templateIds.isEmpty()) {
            return emptyList()
        }
        val prepStmt = connection.prepareStatement(STATEMENT_GET_TEMPLATES).apply {
            this.setArray(1, connection.createArrayOf("integer", templateIds.toTypedArray()))
        }
        val span = tracer.spanBuilder("getTemplatesByIds").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        val transients = generateSequence {
            if (rs.next()) {
                TemplateTransient(
                    templateId = rs.getInt(1),
                    templateName = rs.getString(2),
                    description = rs.getString(3),
                    rightId = rs.getString(4),
                )
            } else null
        }.takeWhile { true }.toList()
        return transients.map {
            Template(
                templateId = it.templateId,
                templateName = it.templateName,
                description = it.description,
                right = rightDB.getRightsByIds(listOf(it.rightId)).first()
            )
        }
    }

    fun insertTemplate(template: Template): TemplateRightIdCreated {
        // First insert a right entry
        val newRightId: String = template.right.let {
            rightDB.insertRight(it)
        }
        val prepStmt = connection.prepareStatement(STATEMENT_INSERT_TEMPLATE, Statement.RETURN_GENERATED_KEYS).apply {
            this.setString(1, template.templateName)
            this.setIfNotNull(2, template.description) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setIfNotNull(3, newRightId) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
        }
        val span = tracer.spanBuilder("insertTemplate").startSpan()
        try {
            span.makeCurrent()
            val affectedRows = runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
            return if (affectedRows > 0) {
                val rs: ResultSet = prepStmt.generatedKeys
                rs.next()
                val newTemplateId = rs.getInt(1)
                TemplateRightIdCreated(templateId = newTemplateId, rightId = newRightId)
            } else throw IllegalStateException("No row has been inserted.")
        } finally {
            span.end()
        }
    }

    fun updateTemplateById(templateId: Int, template: Template): Int {
        // Update Template Table
        val prepStmt = connection.prepareStatement(STATEMENT_UPDATE_TEMPLATE).apply {
            this.setString(1, template.templateName)
            this.setIfNotNull(2, template.description) { value, idx, prepStmt ->
                prepStmt.setString(idx, value)
            }
            this.setInt(3, templateId)
        }
        val span = tracer.spanBuilder("updateTemplateById").startSpan()
        val changedTemplates: Int = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.run { this.executeUpdate() } }
        } finally {
            span.end()
        }
        if (changedTemplates != 1) {
            return 0
        }

        // Get Right Id
        val updatedTemplate = getTemplateTransientById(templateId, span)

        // Update Right Table
        updatedTemplate?.let {
            rightDB.upsertRight(template.right.copy(rightId = it.rightId))
        }
        return changedTemplates
    }

    fun getTemplateList(
        limit: Int,
        offset: Int,
    ): List<Template> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_TEMPLATE_LIST).apply {
            this.setInt(1, limit)
            this.setInt(2, offset)
        }

        val span = tracer.spanBuilder("getTemplateList").startSpan()
        val rs = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmt.executeQuery() }
        } finally {
            span.end()
        }

        val templates = generateSequence {
            if (rs.next()) {
                TemplateTransient(
                    templateId = rs.getInt(1),
                    templateName = rs.getString(2),
                    description = rs.getString(3),
                    rightId = rs.getString(4),
                )
            } else null
        }.takeWhile { true }.toList()

        val rights = rightDB.getRightsByIds(templates.map { it.rightId })
        return templates.map {
            val assRight: ItemRight? = rights.firstOrNull { r -> it.rightId == r.rightId }
            if (assRight == null) {
                throw IllegalStateException("No right was found for template. This should not happen!")
            } else {
                Template(
                    templateId = it.templateId,
                    templateName = it.templateName,
                    description = it.description,
                    right = assRight,
                )
            }
        }
    }

    /**
     * Queries on Template-Bookmark Pairs Table.
     */
    fun deletePairsByTemplateId(templateId: Int): Int {
        val prepStmt = connection.prepareStatement(STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR_BY_TEMP).apply {
            this.setInt(1, templateId)
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
     * Get all bookmark ids that are a connected to a given template-id.
     */
    fun getBookmarkIdsByTemplateId(templateId: Int): List<Int> {
        val prepStmt = connection.prepareStatement(STATEMENT_GET_BOOKMARKS_BY_TEMPLATE_ID).apply {
            this.setInt(1, templateId)
        }
        val span = tracer.spanBuilder("getBookmarkIdsByTemplateId").startSpan()
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

    fun insertTemplateBookmarkPair(
        bookmarkTemplate: BookmarkTemplate,
    ): Int {
        val prepStmt = connection
            .prepareStatement(STATEMENT_INSERT_TEMPLATE_BOOKMARK_PAIR, Statement.RETURN_GENERATED_KEYS)
            .apply {
                this.setInt(1, bookmarkTemplate.templateId)
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
            this.setInt(1, bookmarkTemplate.templateId)
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
                this.setInt(2, bookmarkTemplate.templateId)
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
                    templateId = rs.getInt(2),
                )
            } else null
        }.takeWhile { true }.toList()
    }

    private fun getTemplateTransientById(templateId: Int, span: Span): TemplateTransient? {
        val prepStmtGetTemplate = connection.prepareStatement(STATEMENT_GET_TEMPLATE).apply {
            this.setInt(1, templateId)
        }
        val rsTemplate = try {
            span.makeCurrent()
            runInTransaction(connection) { prepStmtGetTemplate.executeQuery() }
        } finally {
            span.end()
        }
        return if (rsTemplate.next()) {
            TemplateTransient(
                templateId = rsTemplate.getInt(1),
                templateName = rsTemplate.getString(2),
                description = rsTemplate.getString(3),
                rightId = rsTemplate.getString(4),
            )
        } else {
            null
        }
    }

    companion object {
        private const val TABLE_NAME_TEMPLATE = "template"
        private const val TABLE_NAME_TEMPLATE_BOOKMARK_MAP = "template_bookmark_map"
        private const val COLUMN_TEMPLATE_ID = "template_id"
        private const val COLUMN_BOOKMARK_ID = "bookmark_id"
        private const val CONSTRAINT_TEMPLATE_BOOKMARK_MAP = "template_bookmark_map_pkey"

        const val STATEMENT_DELETE_TEMPLATE_BY_ID = "DELETE " +
            "FROM $TABLE_NAME_TEMPLATE" +
            " WHERE $COLUMN_TEMPLATE_ID = ?"

        const val STATEMENT_GET_TEMPLATE = "SELECT " +
            "template_id,template_name,template_description,right_id" +
            " FROM $TABLE_NAME_TEMPLATE" +
            " WHERE $COLUMN_TEMPLATE_ID = ?"

        const val STATEMENT_GET_TEMPLATE_LIST = "SELECT" +
            " template_id,template_name,template_description,right_id" +
            " FROM $TABLE_NAME_TEMPLATE" +
            " ORDER BY template_name ASC LIMIT ? OFFSET ?;"

        const val STATEMENT_GET_TEMPLATES = "SELECT" +
            " template_id,template_name,template_description,right_id" +
            " FROM $TABLE_NAME_TEMPLATE" +
            " WHERE $COLUMN_TEMPLATE_ID = ANY(?)"

        const val STATEMENT_INSERT_TEMPLATE =
            "INSERT INTO $TABLE_NAME_TEMPLATE(template_name, template_description, right_id) VALUES(?,?,?)"

        const val STATEMENT_UPDATE_TEMPLATE =
            "UPDATE $TABLE_NAME_TEMPLATE SET template_name=?,template_description=? WHERE $COLUMN_TEMPLATE_ID = ?"

        const val STATEMENT_GET_BOOKMARKS_BY_TEMPLATE_ID =
            "SELECT bookmark_id" +
                " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " WHERE template_id = ?"

        const val STATEMENT_INSERT_TEMPLATE_BOOKMARK_PAIR =
            "INSERT INTO $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
                " (template_id, bookmark_id)" +
                " VALUES(?,?)"

        const val STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR = "DELETE" +
            " FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
            " WHERE $COLUMN_TEMPLATE_ID = ? AND $COLUMN_BOOKMARK_ID = ?"

        const val STATEMENT_DELETE_TEMPLATE_BOOKMARK_PAIR_BY_TEMP = "DELETE " +
            "FROM $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
            " WHERE $COLUMN_TEMPLATE_ID = ?"

        const val STATEMENT_UPSERT_TEMPLATE_BOOKMARK_PAIR = "INSERT INTO $TABLE_NAME_TEMPLATE_BOOKMARK_MAP" +
            " ($COLUMN_BOOKMARK_ID, $COLUMN_TEMPLATE_ID)" +
            " VALUES(?,?)" +
            " ON CONFLICT ON CONSTRAINT $CONSTRAINT_TEMPLATE_BOOKMARK_MAP" +
            " DO NOTHING"
    }
}
