package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex

object PropertiesShapeFormatter {
    fun formatPopupHtml(project: Project, targetFqn: String, variableName: String? = null): String? {
        val phpIndex = PhpIndex.getInstance(project)
        val phpClass = phpIndex.getClassesByFQN(targetFqn).firstOrNull() ?: return null
        val shortClass = targetFqn.substringAfterLast('\\')

        val fields = PropertiesFieldCatalog.getFields(project, targetFqn)
            .asSequence()
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name }
            .toList()

        return buildString {
            append("<div class='definition'><pre>")
            if (!variableName.isNullOrBlank()) {
                append(escape(variableName))
                append(": ")
            }

            if (fields.isEmpty()) {
                append("array{}")
            } else {
                append("array{")
                append("\n")

                fields.forEachIndexed { index, field ->
                    append("  ")
                    append(link("field:$targetFqn#${field.name}", field.name))
                    append(": ")
                    append(escape(PropertiesFieldCatalog.renderFieldType(field.type.toString())))

                    if (index < fields.lastIndex) {
                        append(",")
                    }
                    append("\n")
                }

                append("}")
            }

            append("</pre></div>")

            append("<div class='content'>")
            append("<p><b>Derived from</b> ")
            append(link("class:$targetFqn", "${PropertiesMagicTypeNames.preferredTypeReference(phpClass)}<$shortClass>"))
            append("</p>")
            append("<p><b>Source class</b> ")
            append(link("class:$targetFqn", targetFqn))
            append("</p>")
            append("</div>")
        }
    }

    private fun link(ref: String, label: String): String {
        return buildString {
            DocumentationManagerUtil.createHyperlink(this, ref, escape(label), false)
        }
    }
    private fun escape(s: String): String {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
