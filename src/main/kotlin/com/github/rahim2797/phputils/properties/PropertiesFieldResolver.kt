package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.elements.Field

object PropertiesFieldResolver {
    fun findField(project: Project, targetFqn: String, key: String): Field? {
        return PropertiesClassFields.findField(project, targetFqn, key)
    }

    fun getFieldNames(project: Project, targetFqn: String): List<String> {
        return PropertiesClassFields.getFields(project, targetFqn)
            .asSequence()
            .map { it.name }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
}
