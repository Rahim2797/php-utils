package com.github.rahim2797.phputils.properties

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesKeyReference(
    literal: StringLiteralExpression,
    private val key: String,
    private val targetFqn: String
) : PsiPolyVariantReferenceBase<StringLiteralExpression>(
    literal,
    keyRangeInLiteral(literal),
    true
) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val phpIndex = PhpIndex.getInstance(project)

        val results = mutableListOf<ResolveResult>()

        for (phpClass in phpIndex.getClassesByFQN(targetFqn)) {
            val field = findMatchingField(phpClass, key)
            if (field != null) {
                results += PsiElementResolveResult(field)
            }
        }

        return results.toTypedArray()
    }

    override fun resolve(): Field? {
        return multiResolve(false).firstNotNullOfOrNull { it.element as? Field }
    }

    override fun getVariants(): Array<Any> {
        val project = element.project
        return PropertiesFieldResolver.getFieldNames(project, targetFqn).toTypedArray()
    }

    private fun findMatchingField(phpClass: PhpClass, fieldName: String): Field? {
        phpClass.findFieldByName(fieldName, false)?.let { return it }

        return phpClass.fields.firstOrNull { it.name == fieldName }
    }

    companion object {
        private fun keyRangeInLiteral(literal: StringLiteralExpression): TextRange {
            val text = literal.text
            if (text.length >= 2 && (
                    (text.startsWith("'") && text.endsWith("'")) ||
                    (text.startsWith("\"") && text.endsWith("\""))
                )
            ) {
                return TextRange(1, text.length - 1)
            }
            return TextRange(0, text.length)
        }
    }
}