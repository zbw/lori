package de.zbw.persistence.lori.server

import io.opentelemetry.api.trace.Tracer

abstract class AbstractDB(
    val connectionPool: ConnectionPool,
    val batchConnectionPool: ConnectionPool,
    val tracer: Tracer,
    val tableName: String,
) {
    internal suspend fun cleanTable(isBatchJob: Boolean = false): Int =
        DatabaseConnector.executeUpdate(
            connectionPool =
                if (isBatchJob) {
                    batchConnectionPool
                } else {
                    connectionPool
                },
            sql =
                "DELETE" +
                    " FROM $tableName;",
            tracer = tracer,
            spanName = "cleanTable$tableName",
        )
}
