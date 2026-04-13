// file: C:/Users/benme/IdeaProjects/php-utils/src/main/kotlin/com/github/rahim2797/phputils/properties/PropertiesLookupElements.kt
package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PropertiesLookupElements {

    fun buildForLiteral(literal: StringLiteralExpression, targetFqn: String): Array<Any> {
        return PropertiesClassFields.getFields(literal.project, targetFqn)
            .asSequence()
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name }
            .sortedBy { it.name }
            .map { field ->
                LookupElementBuilder.create(field.name)
                    .withPresentableText(field.name)
                    .withTypeText(renderFieldType(field), true)
                    .withIcon(field.getIcon(0))
                    .withInsertHandler(PropertiesArrayKeyInsertHandler(literal))
            }
            .toList()
            .toTypedArray()
    }

    private fun renderFieldType(field: Field): String {
        val raw = field.type.toString().removePrefix("\\")
        return if (raw.isBlank()) "mixed" else raw
    }
}

class PropertiesArrayKeyInsertHandler(
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

            val hasArrow = probe + 1 < chars.length &&
                    chars[probe] == '=' &&
                    chars[probe + 1] == '>'

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
