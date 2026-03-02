package de.zbw.persistence.lori.server.statistics

data class StatisticResult(
    val metric: String,
    val value: String,
    val count: Long,
)
