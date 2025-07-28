package de.zbw.business.lori.server.utils

import io.ktor.http.Parameters

inline fun <reified T : Enum<T>> Parameters.enumOrNull(name: String): T? {
    val value = this[name] ?: return null
    return enumValues<T>().find { it.name.equals(value, ignoreCase = true) }
}
