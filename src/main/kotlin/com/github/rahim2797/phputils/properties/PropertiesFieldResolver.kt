package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.elements.Field

object PropertiesFieldResolver {
    fun findField(project: Project, targetFqn: String, key: String): Field? {
        return PropertiesFieldCatalog.findField(project, targetFqn, key)
    }

    fun getFieldNames(project: Project, targetFqn: String): List<String> {
        return PropertiesFieldCatalog.getFieldNames(project, targetFqn)
    }
}
