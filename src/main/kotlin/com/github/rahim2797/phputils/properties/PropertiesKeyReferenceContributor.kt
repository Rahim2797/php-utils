package com.github.rahim2797.phputils.properties

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesKeyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val literal = element as StringLiteralExpression
                    val key = literal.contents

                    val targetFqn =
                        resolveArrayAccessTarget(literal)
                            ?: resolveArrayLiteralTarget(literal)
                        ?: return PsiReference.EMPTY_ARRAY

                    return arrayOf(PropertiesKeyReference(literal, key, targetFqn))
                    }

                private fun resolveArrayAccessTarget(literal: StringLiteralExpression): String? {
                    val receiver = PsiGuards.getArrayAccessReceiver(literal) ?: return null

                    PropertiesTargetResolver.resolveTargetFqnLocally(receiver)?.let { return it }

                    val resolvedReceiverType = receiver.type.global(literal.project)
                    return PropertiesTypeInspector.extractTargetFqn(resolvedReceiverType)
                }

                private fun resolveArrayLiteralTarget(literal: StringLiteralExpression): String? {
                    val arrayCreation = PsiGuards.getOwningArrayCreation(literal) ?: return null
                    return PropertiesTargetResolver.resolveTargetFqnForArrayLiteral(arrayCreation)
                }
            }
        )
    }
}