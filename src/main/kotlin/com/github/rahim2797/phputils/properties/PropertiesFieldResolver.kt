package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Field

object PropertiesFieldResolver {
    fun findField(project: Project, targetFqn: String, key: String): Field? {
        val phpIndex = PhpIndex.getInstance(project)

        for (phpClass in phpIndex.getClassesByFQN(targetFqn)) {
            phpClass.findFieldByName(key, false)?.let { return it }
            phpClass.fields.firstOrNull { it.name == key }?.let { return it }
        }

        return null
    }

    fun getFieldNames(project: Project, targetFqn: String): List<String> {
        val phpIndex = PhpIndex.getInstance(project)

        return phpIndex.getClassesByFQN(targetFqn)
            .asSequence()
            .flatMap { it.fields.asSequence() }
            .map { it.name }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }
}