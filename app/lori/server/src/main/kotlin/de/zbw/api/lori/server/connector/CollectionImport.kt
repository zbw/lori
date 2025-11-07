package de.zbw.api.lori.server.connector

data class CollectionImport(
    val collectionId: Int,
    val importsExpected: Int,
    val importsReceived: Int,
)
