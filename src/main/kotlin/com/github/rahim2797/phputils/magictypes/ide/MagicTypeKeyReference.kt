package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.MagicTypeMatch
import com.github.rahim2797.phputils.magictypes.shapes.MagicTypeShapeService
import com.github.rahim2797.phputils.properties.PsiGuards
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class MagicTypeKeyReference(
    literal: StringLiteralExpression,
    private val key: String,
    private val matches: List<MagicTypeMatch>,
) : PsiPolyVariantReferenceBase<StringLiteralExpression>(
    literal,
    keyRangeInLiteral(literal),
    true
) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return MagicTypeShapeService.fields(element.project, matches, key)
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }

    override fun resolve(): Field? {
        return multiResolve(false).firstNotNullOfOrNull { it.element as? Field }
    }

    override fun getVariants(): Array<Any> {
        return MagicTypeShapeService.keys(element.project, matches)
            .map { keyShape ->
                val displayField = keyShape.fields.firstOrNull()
                LookupElementBuilder.create(keyShape.name)
                    .withPresentableText(keyShape.name)
                    .withTypeText(keyShape.mergedType?.types?.joinToString("|") { it.removePrefix("\\") } ?: "mixed", true)
                    .withIcon(displayField?.getIcon(0))
                    .withInsertHandler(MagicTypeArrayKeyInsertHandler(element))
            }
            .toTypedArray()
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

private class MagicTypeArrayKeyInsertHandler(
    private val literal: StringLiteralExpression
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val key = item.lookupString
        val document = context.document
        val startOffset = literal.textRange.startOffset

        var caretOffset = startOffset + key.length + 2

        if (PsiGuards.isArrayLiteralKeyContext(literal)) {
            val chars = document.charsSequence
            var probe = caretOffset

            while (probe < chars.length && chars[probe].isWhitespace()) {
                probe++
            }

            val hasArrow = probe + 1 < chars.length && chars[probe] == '=' && chars[probe + 1] == '>'
            if (hasArrow) {
                probe += 2
                while (probe < chars.length && chars[probe].isWhitespace()) {
                    probe++
                }
                caretOffset = probe
            } else {
                document.insertString(caretOffset, " => ")
                caretOffset += 4
            }
        }

        context.editor.caretModel.moveToOffset(caretOffset)
    }
}
