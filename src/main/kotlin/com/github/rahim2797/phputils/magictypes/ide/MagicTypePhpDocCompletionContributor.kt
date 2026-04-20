package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.MagicTypeRegistry
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.WeighingContext
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType

class MagicTypePhpDocCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val phpDocType = PsiTreeUtil.getParentOfType(parameters.position, PhpDocType::class.java, false) ?: return
        if (!isAtTopLevelTypeIdentifier(phpDocType, parameters.offset)) return

        val matchesPrefix = MagicTypeRegistry.handlers.any { result.prefixMatcher.prefixMatches(it.shortName) }
        if (!matchesPrefix) return

        val sorter = CompletionSorter.defaultSorter(parameters, result.prefixMatcher)
            .weighBefore("priority", MagicTypePhpDocWeigher())
        val sorted = result.withRelevanceSorter(sorter)

        MagicTypeRegistry.handlers.forEach {
            val typeText = it.preferredTypeReference(null).removePrefix("\\")
            sorted.addElement(MagicTypeLookup.build(it.shortName, typeText, it))
        }
        result.runRemainingContributors(parameters, { completionResult -> sorted.addElement(completionResult.lookupElement) }, true, sorter)
        result.stopHere()
    }

    private fun isAtTopLevelTypeIdentifier(phpDocType: PhpDocType, offset: Int): Boolean {
        val relativeOffset = offset - phpDocType.textRange.startOffset
        if (relativeOffset < 0) return false
        val genericStart = phpDocType.text.indexOf('<').let { if (it >= 0) it else Int.MAX_VALUE }
        return relativeOffset <= genericStart
    }
}

private class MagicTypePhpDocWeigher : LookupElementWeigher("magicTypeLookup") {
    override fun weigh(element: LookupElement, context: WeighingContext): Comparable<*> {
        return when (element.`object`) {
            is MagicTypeLookup.Marker -> 0
            else -> 1
        }
    }
}

private object MagicTypeLookup {
    data class Marker(val handlerId: String)

    fun build(name: String, typeText: String, handler: com.github.rahim2797.phputils.magictypes.MagicTypeFeatureHandler): LookupElement {
        var builder = LookupElementBuilder.create(Marker(handler.descriptor.id), name)
            .withPresentableText(name)
            .withTailText("<T>", true)
            .withTypeText(typeText, true)
            .withInsertHandler(MagicTypeInsertHandler(handler))

        for (length in 1 until name.length) {
            builder = builder.withLookupString(name.substring(0, length))
        }
        return builder
    }
}

private class MagicTypeInsertHandler(
    private val handler: com.github.rahim2797.phputils.magictypes.MagicTypeFeatureHandler
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val document = context.document
        val phpDocType = PsiTreeUtil.getParentOfType(context.file.findElementAt(context.startOffset), PhpDocType::class.java, false)
        val replacement = handler.preferredTypeReference(phpDocType ?: context.file)
        val existingText = phpDocType?.text.orEmpty()
        val hasOpeningAngle = existingText.indexOf('<').takeIf { it >= 0 }?.let { phpDocType!!.textRange.startOffset + it }
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
