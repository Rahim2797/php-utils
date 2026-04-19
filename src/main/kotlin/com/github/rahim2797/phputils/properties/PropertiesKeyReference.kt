package com.github.rahim2797.phputils.properties

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesKeyReference(
    literal: StringLiteralExpression,
    private val key: String,
    private val targetFqns: Set<String>
) : PsiPolyVariantReferenceBase<StringLiteralExpression>(
    literal,
    keyRangeInLiteral(literal),
    true
) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return PropertiesFieldCatalog.findFields(element.project, targetFqns, key)
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }

    override fun resolve(): Field? {
        return multiResolve(false).firstNotNullOfOrNull { it.element as? Field }
    }

    override fun getVariants(): Array<Any> {
        return PropertiesLookupElements.buildForLiteral(element, targetFqns)
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
