package de.zbw.api.lori.server.type

/**
 *
 * Created on 08-17-2022.
 * @author Christian Bay (c.bay@zbw.eu)
 */

enum class UserRole(val dbRepresentation: String){
    READ_ONLY("READONLY"),
    READ_WRITE("READWRITE"),
    ADMIN("ADMIN"),
}

data class User(
    val name: String,
    val passwordHash: String,
    val role: UserRole,
)
