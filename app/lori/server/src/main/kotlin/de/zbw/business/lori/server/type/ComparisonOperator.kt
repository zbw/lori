package de.zbw.business.lori.server.type

enum class ComparisonOperator {
    LESS,
    LESS_OR_EQUAL,
    EQUAL,
    GREATER_OR_EQUAL,
    GREATER,
    ;

    fun toSQL(): String =
        when (this) {
            LESS -> "<"
            LESS_OR_EQUAL -> "<="
            EQUAL -> "="
            GREATER_OR_EQUAL -> ">="
            GREATER -> ">"
        }
}
