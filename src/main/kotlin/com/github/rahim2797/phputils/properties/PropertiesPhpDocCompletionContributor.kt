package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.WeighingContext
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType

class PropertiesPhpDocCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val phpDocType = PsiTreeUtil.getParentOfType(parameters.position, PhpDocType::class.java, false)
            ?: return
        if (!isAtTopLevelTypeIdentifier(phpDocType, parameters.offset)) return
        if (!result.prefixMatcher.prefixMatches(PropertiesPhpDocMagicTypeLookup.NAME)) return

        val sorter = CompletionSorter
            .defaultSorter(parameters, result.prefixMatcher)
            .weighBefore("priority", PropertiesPhpDocMagicTypeWeigher())
        val sortedResult = result.withRelevanceSorter(sorter)

        sortedResult.addElement(PropertiesPhpDocMagicTypeLookup.build())
        result.runRemainingContributors(
            parameters,
            { completionResult -> sortedResult.addElement(completionResult.lookupElement) },
            true,
            sorter
        )
        result.stopHere()
    }

    private fun isAtTopLevelTypeIdentifier(phpDocType: PhpDocType, offset: Int): Boolean {
        val relativeOffset = offset - phpDocType.textRange.startOffset
        if (relativeOffset < 0) return false

        val text = phpDocType.text
        val genericStart = text.indexOf('<').let { if (it >= 0) it else Int.MAX_VALUE }
        return relativeOffset <= genericStart
    }
}

private class PropertiesPhpDocMagicTypeWeigher : LookupElementWeigher("propertiesMagicType") {
    override fun weigh(element: LookupElement, context: WeighingContext): Comparable<*> {
        return when (element.`object`) {
            PropertiesPhpDocMagicTypeLookup.marker -> 0
            else -> 1
        }
    }
}

private object PropertiesPhpDocMagicTypeLookup {
    const val NAME = PropertiesMagicTypeNames.SHORT_NAME
    val marker = Any()

    fun build(): LookupElement {
        var builder = LookupElementBuilder.create(marker, NAME)
            .withPresentableText(NAME)
            .withTailText("<T>", true)
            .withTypeText(PropertiesMagicTypeNames.QUALIFIED_NAME, true)
            .withInsertHandler(PropertiesPhpDocTypeInsertHandler)

        for (length in 1 until NAME.length) {
            builder = builder.withLookupString(NAME.substring(0, length))
        }

        return builder
    }
}

private object PropertiesPhpDocTypeInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val phpDocType = PsiTreeUtil.getParentOfType(context.file.findElementAt(context.startOffset), PhpDocType::class.java, false)
        val preferredReference = PropertiesMagicTypeNames.preferredTypeReference(phpDocType ?: context.file)
        val replacement = preferredReference

        val existingText = phpDocType?.text.orEmpty()
        val hasOpeningAngle = existingText.indexOf('<').takeIf { it >= 0 }
            ?.let { phpDocType!!.textRange.startOffset + it }

        val endOffset = hasOpeningAngle ?: context.tailOffset
        document.replaceString(context.startOffset, endOffset, replacement)

        val insertTailOffset = context.startOffset + replacement.length
        val chars = document.charsSequence
        if (insertTailOffset < chars.length && chars[insertTailOffset] == '<') {
            context.editor.caretModel.moveToOffset(insertTailOffset + 1)
            return
        }

        document.insertString(insertTailOffset, "<>")
        context.editor.caretModel.moveToOffset(insertTailOffset + 1)
        context.tailOffset = insertTailOffset + 2
    }
}
