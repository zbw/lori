package de.zbw.api.lori.server.connector

data class CommunityImport(
    val communityId: Int,
    val importsExpected: Int,
    val importsReceived: Int,
)
