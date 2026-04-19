package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PropertiesContextResolver {
    fun resolveArrayAccessTargetFqns(receiver: PhpExpression): Set<String> {
        val localTargets =
            if (PropertiesDumbModeGuards.isDumb(receiver.project)) {
                PropertiesTargetResolver.resolveTargetFqnsLocallyWithoutIndexes(receiver)
            } else {
                PropertiesTargetResolver.resolveTargetFqnsLocally(receiver)
            }
        if (localTargets.isNotEmpty()) return localTargets

        val resolvedReceiverType =
            PropertiesDumbModeGuards.globalTypeOrNull(receiver.type, receiver.project) ?: return emptySet()
        return PropertiesTypeInspector.extractTargetFqns(resolvedReceiverType, receiver)
    }

    fun resolveArrayAccessTargetFqn(receiver: PhpExpression): String? {
        return resolveArrayAccessTargetFqns(receiver).firstOrNull()
    }

    fun resolveLiteralTargetFqns(literal: StringLiteralExpression): Set<String> {
        PsiGuards.getArrayAccessReceiver(literal)?.let { receiver ->
            val accessTargets = resolveArrayAccessTargetFqns(receiver)
            if (accessTargets.isNotEmpty()) {
                return accessTargets
            }
        }

        val arrayCreation = PsiGuards.getOwningArrayCreation(literal) ?: return emptySet()
        return PropertiesTargetResolver.resolveTargetFqnsForArrayLiteral(arrayCreation)
    }

    fun resolveLiteralTargetFqn(literal: StringLiteralExpression): String? {
        return resolveLiteralTargetFqns(literal).firstOrNull()
    }

    fun resolveDocTargetFqns(element: PsiElement?, originalElement: PsiElement?): Set<String> {
        val target = element ?: originalElement ?: return emptySet()

        var current: PsiElement? = target
        repeat(8) {
            if (current == null) return emptySet()

            val phpDocType = current as? PhpDocType
            if (phpDocType != null) {
                val directTargets = PropertiesTypeInspector.extractTargetFqns(phpDocType.text, phpDocType)
                if (directTargets.isNotEmpty()) {
                    return directTargets
                }
            }

            current = current.parent
        }

        return emptySet()
    }

    fun resolveDocTargetFqn(element: PsiElement?, originalElement: PsiElement?): String? {
        return resolveDocTargetFqns(element, originalElement).firstOrNull()
    }
}
