package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesResolver {
    fun resolveFields(type: PhpType, project: Project): Collection<Field> {
        val fqn = PropertiesTypeInspector.extractTargetFqn(type) ?: return emptyList()
        val phpIndex = PhpIndex.getInstance(project)
        val phpClass = phpIndex.getClassesByFQN(fqn).firstOrNull() ?: return emptyList()
        return phpClass.fields
    }
}