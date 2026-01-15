package de.zbw.persistence.lori.server

import io.opentelemetry.api.trace.Tracer

abstract class AbstractDB(
    val connectionPool: ConnectionPool,
    val tracer: Tracer,
    val tableName: String,
) {
    internal suspend fun cleanTable(): Int =
        DatabaseConnector.executeUpdate(
            connectionPool = connectionPool,
            sql =
                "DELETE" +
                    " FROM $tableName;",
            tracer = tracer,
            spanName = "cleanTable$tableName",
        )
}
