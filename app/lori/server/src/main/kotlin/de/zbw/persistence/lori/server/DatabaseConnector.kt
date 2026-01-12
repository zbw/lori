package de.zbw.persistence.lori.server

import StatisticsService
import com.google.gson.Gson
import de.zbw.api.lori.server.config.LoriConfiguration
import de.zbw.business.lori.server.utils.TimezoneUtil
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types
import java.time.OffsetDateTime
import java.util.function.BiFunction

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    val connectionPool: ConnectionPool,
    private val tracer: Tracer,
    internal val bookmarkDB: BookmarkDB = BookmarkDB(connectionPool, tracer),
    internal val groupDB: GroupDB =
        GroupDB(
            connectionPool,
            tracer,
            Gson().newBuilder().create(),
        ),
    internal val itemDB: ItemDB =
        ItemDB(
            connectionPool,
            tracer,
        ),
    internal val metadataDB: MetadataDB =
        MetadataDB(
            connectionPool,
            tracer,
        ),
    internal val rightDB: RightDB =
        RightDB(
            connectionPool,
            tracer,
            groupDB,
        ),
    internal val bookmarkTemplateDB: BookmarkTemplateDB = BookmarkTemplateDB(connectionPool, tracer),
    internal val userDB: UserDB = UserDB(connectionPool, tracer),
    internal val rightErrorDB: RightErrorDB = RightErrorDB(connectionPool, tracer),
    internal val exportJobDB: ExportJobDB = ExportJobDB(connectionPool, tracer),
    internal val genericJobDB: GenericJobDB = GenericJobDB(connectionPool, tracer),
    val statisticsService: StatisticsService = StatisticsService(connectionPool, tracer),
    internal val searchDB: SearchDB = SearchDB(connectionPool, tracer, statisticsService),
) {
    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        ConnectionPool(config),
        tracer,
    )

    init {
        runBlocking {
            connectionPool.useConnection { connection ->
                connection
                    .prepareStatement("create EXTENSION IF NOT EXISTS \"pg_trgm\"")
                    .execute()
                connection.commit()
            }
        }
    }

    companion object {
        const val TABLE_NAME_BOOKMARK = "bookmark"
        const val TABLE_NAME_EXPORT_JOBS = "export_jobs"
        const val TABLE_NAME_ITEM = "item"
        const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        const val TABLE_NAME_ITEM_RIGHT = "item_right"
        const val TABLE_NAME_JOBS = "generic_jobs"
        const val TABLE_NAME_SESSIONS = "sessions"
        const val TABLE_NAME_RIGHT_ERROR = "right_error"

        const val COLUMN_RIGHT_ACCESS_STATE = "access_state"
        const val COLUMN_RIGHT_END_DATE = "end_date"
        const val COLUMN_RIGHT_LICENCE_CONTRACT = "licence_contract"
        const val COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE = "restricted_open_content_licence"
        const val COLUMN_RIGHT_ID = "right_id"
        const val COLUMN_RIGHT_START_DATE = "start_date"
        const val COLUMN_RIGHT_IS_TEMPLATE = "is_template"
        const val COLUMN_RIGHT_TEMPLATE_NAME = "template_name"
        const val COLUMN_RIGHT_ZBW_USER_AGREEMENT = "zbw_user_agreement"

        fun Timestamp.toOffsetDateTime(): OffsetDateTime =
            OffsetDateTime.ofInstant(
                this.toInstant(),
                TimezoneUtil.TIME_ZONE_UTC,
            )

        suspend fun <T> runInTransaction(
            connection: Connection,
            tracer: Tracer,
            spanName: String,
            sql: String? = null,
            isReadOnly: Boolean,
            block: suspend () -> T,
        ): T {
            val span = tracer.spanBuilder(spanName).startSpan()
            span.setAttribute("db.system", "postgresql")
            span.setAttribute("db.operation", isReadOnly.takeIf { it }?.let { "SELECT" } ?: "UPDATE")
            span.setAttribute("db.user", connection.metaData.userName)
            span.setAttribute("db.statement", sql)

            return try {
                val result = block()
                if (!connection.autoCommit && !isReadOnly) connection.commit()
                result
            } catch (e: Exception) {
                if (!connection.autoCommit && !isReadOnly) connection.rollback()
                span.recordException(e)
                throw e
            } finally {
                span.end()
            }
        }

        suspend fun executeUpdate(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
        ): Int =
            connectionPool.useConnection("executeUpdate") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = false,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql).use { stmt ->
                            params(stmt)
                            stmt.executeUpdate()
                        }
                    }
                }
            }

        suspend fun <T> insertReturningKeys(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
            fetchGenerated: (ResultSet) -> T,
        ): List<T> =
            connectionPool.useConnection("insertReturningKeys") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = false,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                            params(stmt)
                            // Execute update
                            stmt.executeUpdate()
                            // Fetch generated keys safely
                            stmt.generatedKeys.use { rs ->
                                val results = mutableListOf<T>()
                                while (rs.next()) {
                                    results += fetchGenerated(rs)
                                }
                                results
                            }
                        }
                    }
                }
            }

        suspend fun insertBatch(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
        ): IntArray =
            connectionPool.useConnection("insertBatch") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = false,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql).use { stmt ->
                            params(stmt)
                            // Execute batch insert
                            stmt.executeBatch()
                        }
                    }
                }
            }

        suspend fun <T> insertBatchReturningKeys(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
            fetchGenerated: (ResultSet) -> T,
        ): List<T> =
            connectionPool.useConnection("insertBatchReturningKeys") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = false,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                            params(stmt)
                            // Execute batch insert
                            stmt.executeBatch()
                            // Fetch generated keys safely
                            stmt.generatedKeys.use { rs ->
                                val results = mutableListOf<T>()
                                while (rs.next()) {
                                    results += fetchGenerated(rs)
                                }
                                results
                            }
                        }
                    }
                }
            }

        suspend fun <T> select(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            fetchSize: Int = 500,
            mapper: (ResultSet) -> T,
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
        ): List<T> =
            connectionPool.useConnection("select") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = true,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql).use { stmt ->
                            stmt.fetchSize = fetchSize
                            // bind parameters (if any)
                            params(stmt)
                            stmt.executeQuery().use { rs ->
                                val result = ArrayList<T>()
                                while (rs.next()) {
                                    result += mapper(rs)
                                }
                                result
                            }
                        }
                    }
                }
            }

        suspend fun <K, V> selectToMap(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            fetchSize: Int = 500,
            mapper: (ResultSet) -> Pair<K, V>,
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
        ): Map<K, V> =
            connectionPool.useConnection("select") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = true,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql).use { stmt ->
                            stmt.fetchSize = fetchSize
                            // bind parameters (if any)
                            params(stmt)
                            stmt.executeQuery().use { rs ->
                                val result = mutableMapOf<K, V>()
                                while (rs.next()) {
                                    result += mapper(rs)
                                }
                                result
                            }
                        }
                    }
                }
            }

        suspend fun count(
            sql: String,
            params: (PreparedStatement) -> Unit = {},
            connectionPool: ConnectionPool,
            tracer: Tracer,
            spanName: String,
        ): Int =
            connectionPool.useConnection("count") { conn ->
                runInTransaction(
                    connection = conn,
                    tracer = tracer,
                    spanName = spanName,
                    sql = sql,
                    isReadOnly = true,
                ) {
                    withContext(Dispatchers.IO) {
                        conn.prepareStatement(sql).use { stmt ->
                            // bind parameters (if any)
                            params(stmt)
                            stmt.executeQuery().use { rs ->
                                if (rs.next()) {
                                    rs.getInt(1)
                                } else {
                                    throw IllegalStateException("No count found.")
                                }
                            }
                        }
                    }
                }
            }

        internal fun <K, V : Any> addDefaultEntriesToMap(
            givenMap: Map<K, V>,
            keys: Set<K>,
            defaultValue: V,
            remappingFunction: BiFunction<V, V, V>,
        ): Map<K, V> {
            val mMap = givenMap.toMutableMap()
            val defaultEntries = keys.toList().map { Pair(it, defaultValue) }
            defaultEntries.fold(mMap) { acc, elem ->
                acc.merge(elem.first, elem.second, remappingFunction)
                acc
            }
            return mMap.toMap()
        }

        /**
         * Helper function which adds a parameter to a prepared query.
         * Inserts NULL if the given parameter is null.
         */
        fun <T> PreparedStatement.setIfNotNull(
            idx: Int,
            element: T?,
            setter: (T, Int, PreparedStatement) -> Unit,
        ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)
    }
}
