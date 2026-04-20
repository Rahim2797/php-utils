package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class MagicTypeKeyReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val literal = element as StringLiteralExpression
                    val key = literal.contents
                    val matches = MagicTypeTargetResolver.resolveLiteralMatches(literal)
                    if (matches.isEmpty()) return PsiReference.EMPTY_ARRAY
                    return arrayOf(MagicTypeKeyReference(literal, key, matches))
                }
            }
        )
    }
}
