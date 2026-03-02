package de.zbw.persistence.lori.server.statistics

data class StatisticsResponse(
    val metadataStats: List<StatisticResult>,
    val rightsStats: List<StatisticResult>,
)
