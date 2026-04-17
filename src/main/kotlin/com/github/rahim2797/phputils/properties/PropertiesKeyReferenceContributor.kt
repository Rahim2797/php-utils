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
                    val targetFqn = PropertiesContextResolver.resolveLiteralTargetFqn(literal)
                        ?: return PsiReference.EMPTY_ARRAY

                    return arrayOf(PropertiesKeyReference(literal, key, targetFqn))
                }
            }
        )
    }
}
