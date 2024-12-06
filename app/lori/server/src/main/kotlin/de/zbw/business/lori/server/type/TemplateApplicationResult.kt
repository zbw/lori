package de.zbw.business.lori.server.type

data class TemplateApplicationResult(
    val rightId: String,
    val templateName: String,
    val testId: String?,
    val appliedMetadataHandles: List<String>,
    val errors: List<RightError>,
    val numberOfErrors: Int,
    val exceptionTemplateApplicationResult: List<TemplateApplicationResult>,
)
