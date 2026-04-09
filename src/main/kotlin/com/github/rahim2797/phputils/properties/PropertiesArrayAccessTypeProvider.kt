package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

class PropertiesArrayAccessTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = 'Q'

    override fun getType(element: PsiElement): PhpType? {
        val arrayAccess = element as? ArrayAccessExpression ?: return null

        val receiver = arrayAccess.value as? PhpExpression ?: return null
        val targetFqn = PropertiesTypeInspector.extractTargetFqn(receiver.type) ?: return null

        val index = arrayAccess.index ?: return null
        val keyLiteral = index.value as? StringLiteralExpression ?: return null
        val key = keyLiteral.contents
        if (key.isBlank()) return null

        val field = PropertiesFieldResolver.findField(element.project, targetFqn, key) ?: return null
        val fieldType = field.type
        if (fieldType.isEmpty) return null

        DebugUtil.warn(
            "Resolved array access type receiver=${receiver.text} target=$targetFqn key=$key field=${field.name} fieldType=$fieldType"
        )

        return fieldType
    }

    override fun complete(expression: String, project: Project): PhpType? = null

    override fun getBySignature(
        expression: String,
        visited: MutableSet<String>,
        depth: Int,
        project: Project
    ) = mutableListOf<PhpNamedElement>()
}