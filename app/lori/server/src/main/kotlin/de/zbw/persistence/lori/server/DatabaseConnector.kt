package de.zbw.persistence.lori.server

import com.google.gson.Gson
import de.zbw.api.lori.server.config.LoriConfiguration
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.runBlocking
import java.sql.Connection
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
    internal val searchDB: SearchDB = SearchDB(connectionPool, tracer),
    internal val bookmarkTemplateDB: BookmarkTemplateDB = BookmarkTemplateDB(connectionPool, tracer),
    internal val userDB: UserDB = UserDB(connectionPool, tracer),
    internal val rightErrorDB: RightErrorDB = RightErrorDB(connectionPool, tracer),
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
        const val TABLE_NAME_ITEM = "item"
        const val TABLE_NAME_ITEM_METADATA = "item_metadata"
        const val TABLE_NAME_ITEM_RIGHT = "item_right"
        const val TABLE_NAME_SESSIONS = "sessions"
        const val TABLE_NAME_RIGHT_ERROR = "right_error"

        const val COLUMN_METADATA_AUTHOR = "author"
        const val COLUMN_METADATA_BAND = "band"
        const val COLUMN_METADATA_COLLECTION_HANDLE = "collection_handle"
        const val COLUMN_METADATA_COMMUNITY_HANDLE = "community_handle"
        const val COLUMN_METADATA_COMMUNITY_NAME = "community_name"
        const val COLUMN_METADATA_COLLECTION_NAME = "collection_name"
        const val COLUMN_METADATA_CREATED_BY = "created_by"
        const val COLUMN_METADATA_CREATED_ON = "created_on"
        const val COLUMN_METADATA_DOI = "doi"
        const val COLUMN_METADATA_ISBN = "isbn"
        const val COLUMN_METADATA_ISSN = "issn"
        const val COLUMN_METADATA_IS_PART_OF_SERIES = "is_part_of_series"
        const val COLUMN_METADATA_HANDLE = "handle"
        const val COLUMN_METADATA_LAST_UPDATED_BY = "last_updated_by"
        const val COLUMN_METADATA_LAST_UPDATED_ON = "last_updated_on"
        const val COLUMN_METADATA_LICENCE_URL = "licence_url"
        const val COLUMN_METADATA_LICENCE_URL_FILTER = "licence_url_filter"
        const val COLUMN_METADATA_PAKET_SIGEL = "paket_sigel"
        const val COLUMN_METADATA_PPN = "ppn"
        const val COLUMN_METADATA_PUBLICATION_DATE = "publication_date"
        const val COLUMN_METADATA_PUBLICATION_TYPE = "publication_type"
        const val COLUMN_METADATA_RIGHTS_K10PLUS = "rights_k10plus"
        const val COLUMN_METADATA_STORAGE_DATE = "storage_date"
        const val COLUMN_METADATA_SUBCOMMUNITY_HANDLE = "sub_community_handle"
        const val COLUMN_METADATA_SUBCOMMUNITY_NAME = "sub_community_name"
        const val COLUMN_METADATA_TITLE = "title"
        const val COLUMN_METADATA_TITLE_JOURNAL = "title_journal"
        const val COLUMN_METADATA_TITLE_SERIES = "title_series"
        const val COLUMN_METADATA_ZDB_ID_JOURNAL = "zdb_id_journal"
        const val COLUMN_METADATA_ZDB_ID_SERIES = "zdb_id_series"

        const val COLUMN_RIGHT_ACCESS_STATE = "access_state"
        const val COLUMN_RIGHT_END_DATE = "end_date"
        const val COLUMN_RIGHT_LICENCE_CONTRACT = "licence_contract"
        const val COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE = "non_standard_open_content_licence"
        const val COLUMN_RIGHT_NON_STANDARD_OPEN_CONTENT_LICENCE_URL = "non_standard_open_content_licence_url"
        const val COLUMN_RIGHT_OPEN_CONTENT_LICENCE = "open_content_licence"
        const val COLUMN_RIGHT_RESTRICTED_OPEN_CONTENT_LICENCE = "restricted_open_content_licence"
        const val COLUMN_RIGHT_ID = "right_id"
        const val COLUMN_RIGHT_START_DATE = "start_date"
        const val COLUMN_RIGHT_IS_TEMPLATE = "is_template"
        const val COLUMN_RIGHT_TEMPLATE_NAME = "template_name"
        const val COLUMN_RIGHT_ZBW_USER_AGREEMENT = "zbw_user_agreement"

        fun Timestamp.toOffsetDateTime(): OffsetDateTime =
            OffsetDateTime.ofInstant(
                this.toInstant(),
                ZoneId.of("UTC+00:00"),
            )

        fun <T> runInTransaction(
            connection: Connection,
            block: () -> T,
        ): T =
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
         * Inserts NULL if the given parameter is null.
         */
        fun <T> PreparedStatement.setIfNotNull(
            idx: Int,
            element: T?,
            setter: (T, Int, PreparedStatement) -> Unit,
        ) = element?.let { setter(element, idx, this) } ?: this.setNull(idx, Types.NULL)
    }
}
