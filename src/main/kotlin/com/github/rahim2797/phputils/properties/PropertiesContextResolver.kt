package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PropertiesContextResolver {
    fun resolveArrayAccessTargetFqn(receiver: PhpExpression): String? {
        val project = receiver.project

        val localTarget =
            if (PropertiesDumbModeGuards.isDumb(project)) {
                PropertiesTargetResolver.resolveTargetFqnLocallyWithoutIndexes(receiver)
            } else {
                PropertiesTargetResolver.resolveTargetFqnLocally(receiver)
            }
        localTarget?.let { return it }

        val resolvedReceiverType = PropertiesDumbModeGuards.globalTypeOrNull(receiver.type, project) ?: return null
        return PropertiesTypeInspector.extractTargetFqn(resolvedReceiverType)
    }

    fun resolveLiteralTargetFqn(literal: StringLiteralExpression): String? {
        PsiGuards.getArrayAccessReceiver(literal)?.let { receiver ->
            resolveArrayAccessTargetFqn(receiver)?.let { return it }
        }

        val arrayCreation = PsiGuards.getOwningArrayCreation(literal) ?: return null
        return PropertiesTargetResolver.resolveTargetFqnForArrayLiteral(arrayCreation)
    }

    fun resolveDocTargetFqn(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = element ?: originalElement ?: return null

        var current: PsiElement? = target
        repeat(8) {
            if (current == null) return null

            val phpDocType = current as? PhpDocType
            if (phpDocType != null) {
                return PropertiesTypeInspector.extractTargetFqn(phpDocType.declaredType)
            }

            current = current.parent
        }

        return null
    }
}
