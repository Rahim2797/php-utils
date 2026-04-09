package com.github.rahim2797.phputils.properties

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesKeyReference(
    literal: StringLiteralExpression,
    private val key: String,
    private val targetFqn: String
) : PsiPolyVariantReferenceBase<StringLiteralExpression>(
    literal,
    TextRange(1, literal.textLength - 1), // inside quotes
    true
) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val phpIndex = PhpIndex.getInstance(project)

        val results = mutableListOf<ResolveResult>()

        for (phpClass in phpIndex.getClassesByFQN(targetFqn)) {
            val field = phpClass.findFieldByName(key, false)
            if (field != null) {
                results += PsiElementResolveResult(field)
            }
        }

        return results.toTypedArray()
    }

    override fun resolve(): Field? {
        return multiResolve(false)
            .mapNotNull { it.element as? Field }
            .firstOrNull()
    }

    override fun getVariants(): Array<Any> {
        val project = element.project
        val phpIndex = PhpIndex.getInstance(project)

        return phpIndex.getClassesByFQN(targetFqn)
            .asSequence()
            .flatMap { it.fields.asSequence() }
            .map { it.name }
            .distinct()
            .toList()
            .toTypedArray()
    }
}