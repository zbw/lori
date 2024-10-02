package de.zbw.business.lori.server.type

data class ErrorQueryResult(
    val totalNumberOfResults: Int,
    val templateNames: Set<String>,
    val conflictTypes: Set<ConflictType>,
    val results: List<RightError>,
)
