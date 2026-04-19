package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesFieldCatalog {
    fun getFields(project: Project, targetFqn: String): List<Field> {
        return PropertiesClassFields.getFields(project, targetFqn)
    }

    fun getFields(project: Project, targetFqns: Collection<String>): List<Field> {
        return targetFqns
            .asSequence()
            .flatMap { getFields(project, it).asSequence() }
            .distinctBy(::fieldIdentity)
            .toList()
    }

    fun findField(project: Project, targetFqn: String, fieldName: String): Field? {
        return getFields(project, targetFqn).firstOrNull { it.name == fieldName }
    }

    fun findFields(project: Project, targetFqns: Collection<String>, fieldName: String): List<Field> {
        return getFields(project, targetFqns)
            .asSequence()
            .filter { it.name == fieldName }
            .distinctBy(::fieldIdentity)
            .toList()
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

    fun getFieldNames(project: Project, targetFqns: Collection<String>): List<String> {
        return getFields(project, targetFqns)
            .asSequence()
            .map { it.name }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    fun getFieldType(project: Project, targetFqns: Collection<String>, fieldName: String): PhpType? {
        val result = PhpType()

        for (field in findFields(project, targetFqns, fieldName)) {
            result.add(field.type)
        }

        return result.takeIf { it.types.isNotEmpty() }
    }

    fun renderFieldType(field: Field): String {
        return renderFieldType(field.type.toString())
    }

    fun renderFieldType(fields: Collection<Field>): String {
        val renderedTypes = fields
            .asSequence()
            .map(::renderFieldType)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()

        return if (renderedTypes.isEmpty()) "mixed" else renderedTypes.joinToString("|")
    }

    fun renderFieldType(raw: String): String {
        val normalized = raw.removePrefix("\\")
        return if (normalized.isBlank()) "mixed" else normalized
    }

    private fun fieldIdentity(field: Field): String {
        val owner = field.containingClass?.fqn ?: field.containingFile?.virtualFile?.path ?: "unknown"
        return "$owner#${field.name}"
    }
}
