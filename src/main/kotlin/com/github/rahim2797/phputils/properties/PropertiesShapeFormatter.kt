package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex

object PropertiesShapeFormatter {
    fun formatShape(project: Project, targetFqn: String): String? {
        val phpIndex = PhpIndex.getInstance(project)
        val phpClass = phpIndex.getClassesByFQN(targetFqn).firstOrNull() ?: return null

        val entries = phpClass.fields
            .asSequence()
            .filter { it.name.isNotBlank() }
            .map { field ->
                val renderedType = renderFieldType(field.type.toString())
                "${field.name}: $renderedType"
            }
            .distinct()
            .sorted()
            .toList()

        if (entries.isEmpty()) {
            return "array{}"
        }

        return buildString {
            append("array{")
            append(entries.joinToString(", "))
            append("}")
        }
    }

    fun formatPopupHtml(project: Project, targetFqn: String, variableName: String? = null): String? {
        val phpIndex = PhpIndex.getInstance(project)
        val phpClass = phpIndex.getClassesByFQN(targetFqn).firstOrNull() ?: return null
        val shortClass = targetFqn.substringAfterLast('\\')

        val fields = phpClass.fields
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
                    append(escape(renderFieldType(field.type.toString())))

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
            append(link("class:$targetFqn", "Properties<$shortClass>"))
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

    private fun renderFieldType(raw: String): String {
        if (raw.isBlank()) return "mixed"
        return raw.removePrefix("\\")
    }

    private fun escape(s: String): String {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}