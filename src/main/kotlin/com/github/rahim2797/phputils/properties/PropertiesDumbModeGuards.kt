package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesDumbModeGuards {
    fun isDumb(project: Project): Boolean = DumbService.isDumb(project)

    inline fun <T> runSmart(project: Project, action: () -> T): T? {
        return if (isDumb(project)) null else action()
    }

    fun globalTypeOrNull(type: PhpType, project: Project): PhpType? {
        return runSmart(project) { type.global(project) }
    }
}
