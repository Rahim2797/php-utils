package com.github.rahim2797.phputils.properties

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
        val shape = formatShape(project, targetFqn) ?: return null
        val shortClass = targetFqn.substringAfterLast('\\')

        return buildString {
            append("<div class='definition'><pre>")
            if (!variableName.isNullOrBlank()) {
                append(escape(variableName))
                append(": ")
            }
            append(escape(shape))
            append("</pre></div>")

            append("<div class='content'>")
            append("<p><b>Derived from</b> ")
            append(escape("Properties<$shortClass>"))
            append("</p>")
            append("<p><b>Source class</b> ")
            append(escape(targetFqn))
            append("</p>")
            append("</div>")
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