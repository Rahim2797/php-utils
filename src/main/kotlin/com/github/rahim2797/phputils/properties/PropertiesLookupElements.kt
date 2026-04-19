// file: C:/Users/benme/IdeaProjects/php-utils/src/main/kotlin/com/github/rahim2797/phputils/properties/PropertiesLookupElements.kt
package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PropertiesLookupElements {

    fun buildForLiteral(literal: StringLiteralExpression, targetFqns: Collection<String>): Array<Any> {
        return PropertiesFieldCatalog.getFields(literal.project, targetFqns)
            .asSequence()
            .filter { it.name.isNotBlank() }
            .groupBy { it.name }
            .toSortedMap()
            .map { (name, fields) ->
                val displayField = fields.first()
                LookupElementBuilder.create(name)
                    .withPresentableText(name)
                    .withTypeText(PropertiesFieldCatalog.renderFieldType(fields), true)
                    .withIcon(displayField.getIcon(0))
                    .withInsertHandler(PropertiesArrayKeyInsertHandler(literal))
            }
            .toList()
            .toTypedArray()
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
