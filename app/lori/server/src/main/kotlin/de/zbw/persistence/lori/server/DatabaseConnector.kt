package de.zbw.persistence.lori.server

import com.google.gson.Gson
import de.zbw.api.lori.server.config.LoriConfiguration
import io.opentelemetry.api.trace.Tracer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.function.BiFunction

/**
 * Connector for interacting with the postgres database.
 *
 * Created on 09-21-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class DatabaseConnector(
    val connection: Connection,
    private val tracer: Tracer,
    internal val bookmarkDB: BookmarkDB = BookmarkDB(connection, tracer),
    internal val groupDB: GroupDB = GroupDB(
        connection,
        tracer,
        Gson().newBuilder().create(),
    ),
    internal val itemDB: ItemDB = ItemDB(
        connection,
        tracer,
    ),
    internal val metadataDB: MetadataDB = MetadataDB(
        connection,
        tracer,
    ),
    internal val rightDB: RightDB = RightDB(
        connection,
        tracer,
        groupDB,
    ),
    internal val searchDB: SearchDB = SearchDB(connection, tracer),
    internal val userDB: UserDB = UserDB(connection, tracer),
) {
    constructor(
        config: LoriConfiguration,
        tracer: Tracer,
    ) : this(
        DriverManager.getConnection(config.sqlUrl, config.sqlUser, config.sqlPassword),
        tracer,
    )

    init {
        connection.autoCommit = false
        connection
            .prepareStatement("create EXTENSION IF NOT EXISTS \"pg_trgm\"")
            .execute()
        connection.commit()
    }

    companion object {
        const val TABLE_NAME_ITEM = "item"
        const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        const val TABLE_NAME_ITEM_RIGHT = "item_right"
        const val TABLE_NAME_USERS = "users"

        const val COLUMN_METADATA_COLLECTION_NAME = "collection_name"
        const val COLUMN_METADATA_COMMUNITY_NAME = "community_name"
        const val COLUMN_METADATA_PAKET_SIGEL = "paket_sigel"
        const val COLUMN_METADATA_PUBLICATION_DATE = "publication_date"
        const val COLUMN_METADATA_PUBLICATION_TYPE = "publication_type"
        const val COLUMN_METADATA_TITLE = "title"
        const val COLUMN_METADATA_ZDB_ID = "zdb_id"

        const val COLUMN_RIGHT_ACCESS_STATE = "access_state"
        const val COLUMN_RIGHT_END_DATE = "end_date"
        const val COLUMN_RIGHT_LICENCE_CONTRACT = "licence_contract"
        const val COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE = "non_standard_open_content_licence"
        const val COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL = "non_standard_open_content_licence_url"
        const val COLUMN_RIGHT_OPEN_CONTENT_LICENCE = "open_content_licence"
        const val COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE = "restricted_open_content_licence"
        const val COLUMN_RIGHT_ID = "right_id"
        const val COLUMN_RIGHT_START_DATE = "start_date"
        const val COLUMN_RIGHT_ZBW_USER_AGREEMENT = "zbw_user_agreement"

        fun Timestamp.toOffsetDateTime(): OffsetDateTime =
            OffsetDateTime.ofInstant(
                this.toInstant(),
                ZoneId.of("UTC+00:00"),
            )

        fun <T> runInTransaction(connection: Connection, block: () -> T): T =
            try {
                block().also { connection.commit() }
            } catch (e: Exception) {
                connection.rollback()
                throw e
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
         * Inserts NULL if the given if the parameter is null.
         */
        fun <T> PreparedStatement.setIfNotNull(
            idx: Int,
            element: T?,
            setter: (T, Int, PreparedStatement) -> Unit,
        ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)
    }
}
