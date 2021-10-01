package de.zbw.persistence.auth.server.transient

data class UserTableEntry(
    val id: Int,
    val name: String,
    val email: String,
    val hash: String,
)
