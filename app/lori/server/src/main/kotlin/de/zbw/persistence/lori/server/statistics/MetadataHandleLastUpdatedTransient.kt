package de.zbw.persistence.lori.server.statistics

import java.time.Instant

data class MetadataHandleLastUpdatedTransient(
    val handle: String,
    val lastUpdatedOn: Instant,
    val createdOn: Instant,
    val isDeleted: Boolean,
)
