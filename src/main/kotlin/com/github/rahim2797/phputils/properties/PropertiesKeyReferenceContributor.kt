package com.github.rahim2797.phputils.properties

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesKeyReferenceContributor: PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    val literal = element as StringLiteralExpression

                    val receiver = PsiGuards.getArrayAccessReceiver(literal) ?: return PsiReference.EMPTY_ARRAY
                    val targetFqn = PropertiesTypeInspector.extractTargetFqn(receiver.type) ?: return PsiReference.EMPTY_ARRAY

                    DebugUtil.warn("Matched Properties<T> receiver=${receiver.text} target=$targetFqn key=${literal.contents}")

                    return PsiReference.EMPTY_ARRAY
                }
            }
        )
    }
}