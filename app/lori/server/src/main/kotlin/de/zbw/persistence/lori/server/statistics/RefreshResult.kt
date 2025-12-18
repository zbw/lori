package de.zbw.persistence.lori.server.statistics

data class RefreshResult(
    val success: Boolean,
    val durationMs: Long,
    val error: String? = null,
)
