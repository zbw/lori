import de.zbw.business.lori.server.MetadataSearchFilter
import de.zbw.business.lori.server.NoRightInformationFilter
import de.zbw.business.lori.server.RightSearchFilter
import de.zbw.business.lori.server.type.SearchExpression
import de.zbw.business.lori.server.utils.SearchExpressionResolution
import de.zbw.persistence.lori.server.ConnectionPool
import de.zbw.persistence.lori.server.MetadataDB.Companion.COLUMN_METADATA_PAKET_SIGEL
import de.zbw.persistence.lori.server.SearchDB.Companion.buildWhereClause
import de.zbw.persistence.lori.server.statistics.RefreshResult
import de.zbw.persistence.lori.server.statistics.StatisticResult
import de.zbw.persistence.lori.server.statistics.StatisticsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet

class StatisticsService(
    private val connectionPool: ConnectionPool,
) {
    /**
     * CASE 1: No filters - baseline statistics
     */
    suspend fun getStatisticsNoFilter(): StatisticsResponse =
        withContext(Dispatchers.IO) {
            connectionPool.useConnection { conn ->
                conn.autoCommit = false
                try {
                    val metadataStats = mutableListOf<StatisticResult>()

                    // Execute metadata queries sequentially
                    metadataStats.addAll(getMetadataStatFromMV(conn, "paket_sigel"))
                    metadataStats.addAll(getMetadataStatFromMV(conn, "is_part_of_series"))
                    metadataStats.addAll(getMetadataStatFromMV(conn, "publication_type"))
                    metadataStats.addAll(getMetadataStatFromMV(conn, "zdb_ids"))
                    metadataStats.addAll(getMetadataStatFromMV(conn, "licence_url_filter"))

                    val rightsStats = mutableListOf<StatisticResult>()

                    // Execute rights queries sequentially
                    rightsStats.addAll(getRightsStatFromMV(conn, "access_state"))
                    rightsStats.addAll(getRightsStatFromMV(conn, "template_name"))
                    rightsStats.addAll(getRightsStatFromMV(conn, "licence_contract"))
                    rightsStats.addAll(getRightsStatFromMV(conn, "zbw_user_agreement"))
                    rightsStats.addAll(getRightsStatFromMV(conn, "has_legal_risk"))

                    conn.commit()
                    StatisticsResponse(metadataStats, rightsStats)
                } catch (t: Throwable) {
                    conn.rollback()
                    throw t
                }
            }
        }

    /**
     * CASE 2: Filter on metadata only (e.g., publication_type = ?)
     */
    suspend fun getStatisticsWithMetadataFilter(
        searchExpression: SearchExpression?,
        metadataSearchFilters: List<MetadataSearchFilter>,
    ): StatisticsResponse =
        withContext(Dispatchers.IO) {
            connectionPool.useConnection { conn ->
                conn.autoCommit = false
                try {
                    val whereClause =
                        buildWhereClause(
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilters,
                            rightSearchFilter = emptyList(),
                            noRightInformationFilter = null,
                            isStatistics = true,
                        )
                    // Create filtered metadata temp table
                    val sql =
                        """
                        CREATE TEMP TABLE temp_metadata_filtered 
                        ON COMMIT DROP
                        AS
                        SELECT 
                            handle,
                            paket_sigel,
                            is_part_of_series,
                            publication_type,
                            zdb_ids,
                            licence_url_filter
                        FROM item_metadata im
                        WHERE $whereClause
                        """.trimIndent()

                    conn.prepareStatement(sql).use { stmt ->
                        var counter = 1
                        val searchPairs =
                            searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                                ?: emptyList()
                        searchPairs.forEach { f ->
                            counter =
                                f.setSQLParameter(
                                    counter = counter,
                                    preparedStatement = stmt,
                                )
                        }
                        metadataSearchFilters.forEach { f ->
                            counter =
                                f.setSQLParameter(
                                    counter = counter,
                                    preparedStatement = stmt,
                                )
                        }
                        stmt.execute()
                    }

                    conn.createStatement().execute("ANALYZE temp_metadata_filtered")

                    // Create temp table with filtered handles for rights stats
                    conn
                        .prepareStatement(
                            """
                            CREATE TEMP TABLE temp_filtered_handles 
                            ON COMMIT DROP
                            AS
                            SELECT handle
                            FROM item_metadata im
                            WHERE $whereClause
                            """.trimIndent(),
                        ).use { stmt ->
                            var counter = 1
                            val searchPairs =
                                searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                                    ?: emptyList()
                            searchPairs.forEach { f ->
                                counter =
                                    f.setSQLParameter(
                                        counter = counter,
                                        preparedStatement = stmt,
                                    )
                            }
                            metadataSearchFilters.forEach { f ->
                                counter =
                                    f.setSQLParameter(
                                        counter = counter,
                                        preparedStatement = stmt,
                                    )
                            }
                            stmt.execute()
                        }

                    conn.createStatement().execute("ANALYZE temp_filtered_handles")

                    // Execute queries in parallel
                    val metadataStats =
                        getMetadataStatsFromTable(conn, "temp_metadata_filtered")
                    val rightsStats =
                        getRightsStatsWithHandleFilter(conn)

                    conn.commit()

                    StatisticsResponse(
                        metadataStats = metadataStats,
                        rightsStats = rightsStats,
                    )
                } catch (t: Throwable) {
                    conn.rollback()
                    throw t
                }
            }
        }

    /**
     * CASE 3: Filter on item_right only (e.g., licence_contract <> '')
     */
    suspend fun getStatisticsWithRightsFilter(
        searchExpression: SearchExpression?,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): StatisticsResponse =
        withContext(Dispatchers.IO) {
            connectionPool.useConnection { conn ->
                conn.autoCommit = false
                try {
                    createFilteredMetadataTableByRights(
                        conn,
                        searchExpression,
                        rightSearchFilters,
                        noRightInformationFilter,
                    )
                    conn.createStatement().execute("ANALYZE temp_filtered_metadata")
                    val metadataStats = getMetadataStatsFromTable(conn, "temp_filtered_metadata")
                    val rightsStats =
                        getRightsStatsWithDirectFilter(
                            conn,
                            searchExpression,
                            rightSearchFilters,
                            noRightInformationFilter,
                        )

                    conn.commit()
                    StatisticsResponse(metadataStats, rightsStats)
                } catch (t: Throwable) {
                    conn.rollback()
                    throw t
                }
            }
        }

    /**
     * CASE 4: Filter on both metadata and item_right
     */
    suspend fun getStatisticsWithBothFilters(
        searchExpression: SearchExpression?,
        metadataSearchFilters: List<MetadataSearchFilter>,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): StatisticsResponse =
        withContext(Dispatchers.IO) {
            connectionPool.useConnection { conn ->
                conn.autoCommit = false

                try {
                    val whereClause =
                        buildWhereClause(
                            searchExpression = searchExpression,
                            metadataSearchFilter = metadataSearchFilters,
                            rightSearchFilter = rightSearchFilters,
                            noRightInformationFilter = noRightInformationFilter,
                            isStatistics = true,
                        )
                    val sql =
                        """
                        CREATE TEMP TABLE temp_combined_filtered 
                        ON COMMIT DROP
                        AS
                        SELECT DISTINCT
                            im.handle,
                            im.paket_sigel,
                            im.is_part_of_series,
                            im.publication_type,
                            im.zdb_ids,
                            im.licence_url_filter
                        FROM item_metadata im
                        JOIN item i ON i.handle = im.handle
                        JOIN item_right ir ON i.right_id = ir.right_id
                        WHERE $whereClause
                        """.trimIndent()

                    conn.prepareStatement(sql).use { stmt ->
                        var counter = 1
                        val searchPairs =
                            searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                                ?: emptyList()
                        searchPairs.forEach { f ->
                            counter =
                                f.setSQLParameter(
                                    counter = counter,
                                    preparedStatement = stmt,
                                )
                        }
                        metadataSearchFilters.forEach { f ->
                            counter =
                                f.setSQLParameter(
                                    counter = counter,
                                    preparedStatement = stmt,
                                )
                        }
                        rightSearchFilters.forEach { f ->
                            counter =
                                f.setSQLParameter(
                                    counter = counter,
                                    preparedStatement = stmt,
                                )
                        }
                        stmt.execute()
                    }

                    conn.createStatement().execute("ANALYZE temp_combined_filtered")

                    val metadataStats = getMetadataStatsFromTable(conn, "temp_combined_filtered")
                    val rightsStats = getRightsStatsWithCombinedFilter(conn, "temp_combined_filtered")

                    conn.commit()

                    StatisticsResponse(metadataStats, rightsStats)
                } catch (t: Throwable) {
                    conn.rollback()
                    throw t
                }
            }
        }

    // Helper methods

    private fun createFilteredMetadataTableByRights(
        conn: Connection,
        searchExpression: SearchExpression?,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ) {
        val whereClause =
            buildWhereClause(
                searchExpression = searchExpression,
                metadataSearchFilter = emptyList(),
                rightSearchFilter = rightSearchFilters,
                noRightInformationFilter = noRightInformationFilter,
                isStatistics = true,
            )
        val sql =
            """
            CREATE TEMP TABLE temp_filtered_metadata 
            ON COMMIT DROP
            AS
            SELECT 
                im.handle,
                im.paket_sigel,
                im.is_part_of_series,
                im.publication_type,
                im.zdb_ids,
                im.licence_url_filter
            FROM (
                SELECT DISTINCT i.handle
                FROM item_right ir
                JOIN item i ON i.right_id = ir.right_id
                WHERE $whereClause
            ) fh
            JOIN item_metadata im ON im.handle = fh.handle
            """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            var counter = 1
            val searchPairs =
                searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                    ?: emptyList()
            searchPairs.forEach { f ->
                counter =
                    f.setSQLParameter(
                        counter = counter,
                        preparedStatement = stmt,
                    )
            }
            rightSearchFilters.forEach { f ->
                counter =
                    f.setSQLParameter(
                        counter = counter,
                        preparedStatement = stmt,
                    )
            }
            stmt.execute()
        }
    }

    private fun getMetadataStatsFromTable(
        conn: Connection,
        tableName: String,
    ): List<StatisticResult> {
        val results = mutableListOf<StatisticResult>()

        // paket_sigel
        results.addAll(
            executeQuery(
                conn,
                """
                SELECT 
                    '${COLUMN_METADATA_PAKET_SIGEL}' as metric,
                    value,
                    COUNT(DISTINCT handle) as count
                FROM (
                    SELECT handle, unnest(${COLUMN_METADATA_PAKET_SIGEL}) as value
                    FROM $tableName
                    WHERE paket_sigel IS NOT NULL
                ) t
                GROUP BY value
                """.trimIndent(),
            ),
        )

        // is_part_of_series
        results.addAll(
            executeQuery(
                conn,
                """
                SELECT 
                    'is_part_of_series' as metric,
                    unnest(is_part_of_series) as value,
                    COUNT(DISTINCT handle) as count
                FROM $tableName
                WHERE is_part_of_series IS NOT NULL
                GROUP BY unnest(is_part_of_series)
                """.trimIndent(),
            ),
        )

        // publication_type
        results.addAll(
            executeQuery(
                conn,
                """
                SELECT 
                    'publication_type' as metric,
                    publication_type as value,
                    COUNT(DISTINCT handle) as count
                FROM $tableName
                WHERE publication_type IS NOT NULL
                GROUP BY publication_type
                """.trimIndent(),
            ),
        )

        // zdb_ids
        results.addAll(
            executeQuery(
                conn,
                """
                SELECT 
                    'zdb_ids' as metric,
                    unnest(zdb_ids) as value,
                    COUNT(DISTINCT handle) as count
                FROM $tableName
                WHERE zdb_ids IS NOT NULL
                GROUP BY unnest(zdb_ids)
                """.trimIndent(),
            ),
        )

        // licence_url_filter
        results.addAll(
            executeQuery(
                conn,
                """
                SELECT 
                    'licence_url_filter' as metric,
                    licence_url_filter as value,
                    COUNT(DISTINCT handle) as count
                FROM $tableName
                WHERE licence_url_filter IS NOT NULL
                GROUP BY licence_url_filter
                """.trimIndent(),
            ),
        )

        return results
    }

    private fun getRightsStatsNoFilter(conn: Connection): List<StatisticResult> {
        val results = mutableListOf<StatisticResult>()
        val metrics =
            listOf(
                "access_state" to false,
                "template_name" to false,
                "licence_contract" to false,
                "zbw_user_agreement" to true,
                "has_legal_risk" to true,
            )

        metrics.forEach { (metric, needsCast) ->
            val column = "ir.$metric"
            val valueColumn = if (needsCast) "$column::text" else column

            val sql =
                """
                SELECT
                    '$metric' as metric,
                    $valueColumn as value,
                    COUNT(DISTINCT i.handle) as count
                FROM item i
                JOIN item_right ir ON i.right_id = ir.right_id
                WHERE $column IS NOT NULL
                GROUP BY $column
                """.trimIndent()

            results.addAll(executeQuery(conn, sql))
        }

        return results
    }

    private fun getRightsStatsWithHandleFilter(conn: Connection): List<StatisticResult> {
        val results = mutableListOf<StatisticResult>()
        val metrics =
            listOf(
                "access_state" to false,
                "template_name" to false,
                "licence_contract" to false,
                "zbw_user_agreement" to true,
                "has_legal_risk" to true,
            )

        metrics.forEach { (metric, needsCast) ->
            val column = "ir.$metric"
            val valueColumn = if (needsCast) "$column::text" else column

            val sql =
                """
                SELECT
                    '$metric' as metric,
                    $valueColumn as value,
                    COUNT(DISTINCT i.handle) as count
                FROM temp_filtered_handles fh
                JOIN item i ON i.handle = fh.handle
                JOIN item_right ir ON i.right_id = ir.right_id
                WHERE $column IS NOT NULL
                GROUP BY $column
                """.trimIndent()

            results.addAll(executeQuery(conn, sql))
        }

        return results
    }

    private fun getRightsStatSingle(
        conn: Connection,
        metricType: String,
        filterValue: String,
        needsCast: Boolean,
    ): List<StatisticResult> {
        val column = "ir.$metricType"
        val valueColumn = if (needsCast) "$column::text" else column

        val sql =
            """
            SELECT
                ? as metric,
                $valueColumn as value,
                COUNT(DISTINCT i.handle) as count
            FROM item i
            JOIN item_right ir ON i.right_id = ir.right_id
            WHERE $column IS NOT NULL
            AND ir.licence_contract <> ?
            GROUP BY $column
            """.trimIndent()

        return conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, metricType)
            stmt.setString(2, filterValue)
            stmt.executeQuery().toStatisticResults()
        }
    }

    private fun getRightsStatCombined(
        conn: Connection,
        metricType: String,
        rightsFilterValue: String,
        needsCast: Boolean,
    ): List<StatisticResult> {
        val column = "ir.$metricType"
        val valueColumn = if (needsCast) "$column::text" else column

        val sql =
            """
            SELECT
                ? as metric,
                $valueColumn as value,
                COUNT(DISTINCT i.handle) as count
            FROM temp_combined_filtered tcf
            JOIN item i ON i.handle = tcf.handle
            JOIN item_right ir ON i.right_id = ir.right_id
            WHERE $column IS NOT NULL
            AND ir.licence_contract <> ?
            GROUP BY $column
            """.trimIndent()

        return conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, metricType)
            stmt.setString(2, rightsFilterValue)
            stmt.executeQuery().toStatisticResults()
        }
    }

    private fun getRightsStatsWithDirectFilter(
        conn: Connection,
        searchExpression: SearchExpression?,
        rightSearchFilters: List<RightSearchFilter>,
        noRightInformationFilter: NoRightInformationFilter?,
    ): List<StatisticResult> {
        val results = mutableListOf<StatisticResult>()
        val metrics =
            listOf(
                "access_state" to false,
                "template_name" to false,
                "licence_contract" to false,
                "zbw_user_agreement" to true,
                "has_legal_risk" to true,
            )

        metrics.forEach { (metric, needsCast) ->
            val column = "ir.$metric"
            val valueColumn = if (needsCast) "$column::text" else column

            val whereClause =
                buildWhereClause(
                    searchExpression = searchExpression,
                    metadataSearchFilter = emptyList(),
                    rightSearchFilter = rightSearchFilters,
                    noRightInformationFilter = noRightInformationFilter,
                    isStatistics = true,
                )

            val sql =
                """
                SELECT
                    ? as metric,
                    $valueColumn as value,
                    COUNT(DISTINCT i.handle) as count
                FROM item i
                JOIN item_right ir ON i.right_id = ir.right_id
                WHERE $column IS NOT NULL
                AND $whereClause
                GROUP BY $column
                """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                var counter = 1
                stmt.setString(counter++, metric)
                val searchPairs =
                    searchExpression?.let { SearchExpressionResolution.getSearchPairs(it) }
                        ?: emptyList()
                searchPairs.forEach { f ->
                    counter =
                        f.setSQLParameter(
                            counter = counter,
                            preparedStatement = stmt,
                        )
                }
                rightSearchFilters.forEach { f ->
                    counter =
                        f.setSQLParameter(
                            counter = counter,
                            preparedStatement = stmt,
                        )
                }
                results.addAll(stmt.executeQuery().toStatisticResults())
            }
        }

        return results
    }

    private fun getRightsStatsWithCombinedFilter(
        conn: Connection,
        tableName: String,
    ): List<StatisticResult> {
        val results = mutableListOf<StatisticResult>()
        val metrics =
            listOf(
                "access_state" to false,
                "template_name" to false,
                "licence_contract" to false,
                "zbw_user_agreement" to true,
                "has_legal_risk" to true,
            )

        metrics.forEach { (metric, needsCast) ->
            val column = "ir.$metric"
            val valueColumn = if (needsCast) "$column::text" else column

            val sql =
                """
                SELECT
                    ? as metric,
                    $valueColumn as value,
                    COUNT(DISTINCT i.handle) as count
                FROM $tableName tcf
                JOIN item i ON i.handle = tcf.handle
                JOIN item_right ir ON i.right_id = ir.right_id
                WHERE $column IS NOT NULL
                GROUP BY $column
                """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, metric)
                results.addAll(stmt.executeQuery().toStatisticResults())
            }
        }

        return results
    }

    private fun executeQuery(
        conn: Connection,
        sql: String,
    ): List<StatisticResult> =
        conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).toStatisticResults()
        }

    private fun ResultSet.toStatisticResults(): List<StatisticResult> {
        val results = mutableListOf<StatisticResult>()
        while (next()) {
            results.add(
                StatisticResult(
                    metric = getString("metric"),
                    value = getString("value") ?: "null",
                    count = getLong("count"),
                ),
            )
        }
        return results
    }

    // Helper methods for querying materialized views
    private fun getMetadataStatFromMV(
        conn: Connection,
        metricType: String,
    ): List<StatisticResult> {
        val sql =
            """
            SELECT 
                metric,
                value,
                count
            FROM mv_metadata_$metricType
            """.trimIndent()

        return executeQuery(conn, sql)
    }

    private fun getRightsStatFromMV(
        conn: Connection,
        metricType: String,
    ): List<StatisticResult> {
        val sql =
            """
            SELECT
                metric,
                value,
                count
            FROM mv_rights_$metricType
            """.trimIndent()

        return executeQuery(conn, sql)
    }

    /**
     * Refresh materialized views (call this periodically, e.g., hourly or daily)
     */
    suspend fun refreshStatisticsMaterializedViews(): RefreshResult =
        withContext(Dispatchers.IO) {
            connectionPool.useConnection { conn ->
                val startTime = System.currentTimeMillis()

                try {
                    conn.createStatement().use { stmt ->
                        // Use CONCURRENTLY to allow reads during refresh
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_metadata_paket_sigel")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_metadata_is_part_of_series")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_metadata_publication_type")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_metadata_zdb_ids")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_metadata_licence_url_filter")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rights_access_state")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rights_template_name")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rights_licence_contract")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rights_zbw_user_agreement")
                        stmt.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_rights_has_legal_risk")
                    }

                    val duration = System.currentTimeMillis() - startTime
                    RefreshResult(success = true, durationMs = duration)
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    RefreshResult(success = false, durationMs = duration, error = e.message)
                }
            }
        }

    /**
     * Get last refresh time for materialized views
     */
    suspend fun getLastRefreshTime(): Map<String, String?> =
        withContext(Dispatchers.IO) {
            connectionPool.useConnection { conn ->
                val sql =
                    """
                    SELECT 
                        schemaname,
                        matviewname,
                        last_refresh
                    FROM pg_matviews
                    WHERE schemaname = 'public'
                    AND matviewname IN ('mv_metadata_statistics', 'mv_rights_statistics')
                    """.trimIndent()

                val result = mutableMapOf<String, String?>()

                conn.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        while (rs.next()) {
                            val viewName = rs.getString("matviewname")
                            val lastRefresh = rs.getTimestamp("last_refresh")
                            result[viewName] = lastRefresh?.toString()
                        }
                    }
                }

                result
            }
        }
}
