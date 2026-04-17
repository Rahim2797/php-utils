package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.elements.Field

object PropertiesFieldCatalog {
    fun getFields(project: Project, targetFqn: String): List<Field> {
        return PropertiesClassFields.getFields(project, targetFqn)
    }

    fun findField(project: Project, targetFqn: String, fieldName: String): Field? {
        return getFields(project, targetFqn).firstOrNull { it.name == fieldName }
    }

    fun getFieldNames(project: Project, targetFqn: String): List<String> {
        return getFields(project, targetFqn)
            .asSequence()
            .map { it.name }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    fun renderFieldType(field: Field): String {
        return renderFieldType(field.type.toString())
    }

    fun renderFieldType(raw: String): String {
        val normalized = raw.removePrefix("\\")
        return if (normalized.isBlank()) "mixed" else normalized
    }
}
