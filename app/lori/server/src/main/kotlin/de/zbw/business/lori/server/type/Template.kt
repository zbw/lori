package de.zbw.business.lori.server.type

/**
 * Business representation of [TemplateRest].
 *
 * Created on 04-19-2023.
 * @author Christian Bay (c.bay@zbw.eu)
 */
data class Template(
    val templateName: String,
    val templateId: Int? = null,
    val description: String? = null,
    val right: ItemRight,
)
