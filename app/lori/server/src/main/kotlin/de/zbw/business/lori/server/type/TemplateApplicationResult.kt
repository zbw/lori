package de.zbw.business.lori.server.type

data class TemplateApplicationResult(
    val rightId: String,
    val templateName: String,
    val testId: String?,
    val appliedMetadataHandles: List<String>,
    val errors: List<RightError>,
    val numberOfErrors: Int,
    val exceptionTemplateApplicationResult: TemplateApplicationResult?,
    val skippedApplication: Boolean = false,
) {
    // Monoid operation.
    fun mAppend(other: TemplateApplicationResult): TemplateApplicationResult =
        other.copy(
            appliedMetadataHandles = this.appliedMetadataHandles + other.appliedMetadataHandles,
            errors = this.errors + other.errors,
            numberOfErrors = this.numberOfErrors + other.numberOfErrors,
            exceptionTemplateApplicationResult = this.exceptionTemplateApplicationResult ?: other.exceptionTemplateApplicationResult,
        )
}
