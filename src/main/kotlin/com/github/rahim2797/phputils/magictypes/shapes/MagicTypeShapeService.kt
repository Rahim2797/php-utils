package com.github.rahim2797.phputils.magictypes.shapes

import com.github.rahim2797.phputils.magictypes.MagicTypeMatch
import com.github.rahim2797.phputils.magictypes.MagicTypeShape
import com.github.rahim2797.phputils.magictypes.MagicTypeShapeKey
import com.intellij.openapi.project.Project
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object MagicTypeShapeService {
    fun resolveShapes(project: Project, matches: Collection<MagicTypeMatch>): List<MagicTypeShape> {
        return MagicTypeMatch.merge(matches)
            .map { it.handler.buildShape(project, it.targetFqns) }
    }

    fun fieldType(project: Project, matches: Collection<MagicTypeMatch>, key: String): PhpType? {
        val result = PhpType()
        for (shape in resolveShapes(project, matches)) {
            shape.key(key)?.mergedType?.let(result::add)
        }
        return result.takeIf { it.types.isNotEmpty() }
    }

    fun fields(project: Project, matches: Collection<MagicTypeMatch>, key: String): List<Field> {
        return resolveShapes(project, matches)
            .flatMap { it.key(key)?.fields.orEmpty() }
            .distinctBy(::fieldIdentity)
    }

    fun keys(project: Project, matches: Collection<MagicTypeMatch>): List<MagicTypeShapeKey> {
        return resolveShapes(project, matches)
            .flatMap { it.keys }
            .groupBy { it.name }
            .toSortedMap()
            .map { (name, groupedKeys) ->
                val fields = groupedKeys.flatMap { it.fields }.distinctBy(::fieldIdentity)
                val mergedType = PhpType()
                groupedKeys.mapNotNull { it.mergedType }.forEach(mergedType::add)
                MagicTypeShapeKey(name, fields, mergedType.takeIf { it.types.isNotEmpty() })
            }
    }

    private fun fieldIdentity(field: Field): String {
        val owner = field.containingClass?.fqn ?: field.containingFile?.virtualFile?.path ?: "unknown"
        return "$owner#${field.name}"
    }
}
