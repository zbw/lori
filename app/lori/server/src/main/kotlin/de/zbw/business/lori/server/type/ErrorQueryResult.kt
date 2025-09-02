package de.zbw.business.lori.server.type

data class ErrorQueryResult(
    val totalNumberOfResults: Int,
    val contextNames: List<String>,
    val conflictTypes: Set<ConflictType>,
    val results: List<RightError>,
)
