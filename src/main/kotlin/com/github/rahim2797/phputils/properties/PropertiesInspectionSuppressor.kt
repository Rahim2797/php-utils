package com.github.rahim2797.phputils.properties

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpType

class PropertiesInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (element as? PhpFile != null) {
            return false
        }

        if (toolId == "PhpUndefinedClassInspection" && element.text == "Properties") {
            return true
        }

        if (toolId == "PhpDocSignatureInspection" && element.text.contains("Properties<")) {
            return true
        }

        val function = PsiTreeUtil.getParentOfType(
            element,
            Function::class.java,
            false
        ) ?: return false

        if (!isPropertiesArrayContract(function)) {
            return false
        }

        return isReturnMismatchInspection(toolId)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }

    private fun isPropertiesArrayContract(function: Function): Boolean {
        val declaredReturnType = function.type
        if (!declaredReturnType.containsAll(PhpType.ARRAY)) {
            return false
        }

        val doc = function.docComment ?: previousPhpDoc(function) ?: return false
        val returnTag = findReturnTag(doc) ?: return false

        return PropertiesTypeInspector.containsPropertiesType(returnTag.declaredType)
    }

    private fun findReturnTag(doc: PhpDocComment): PhpDocReturnTag? {
        return PsiTreeUtil.findChildrenOfType(doc, PhpDocTag::class.java)
            .firstOrNull { it as? PhpDocReturnTag !== null } as PhpDocReturnTag?
    }

    private fun previousPhpDoc(function: Function): PhpDocComment? {
        var current: PsiElement? = function.prevSibling
        while (current != null) {
            when {
                current is PhpDocComment -> return current
                current.text.isBlank() -> current = current.prevSibling
                else -> return null
            }
        }
        return null
    }

    private fun isReturnMismatchInspection(toolId: String): Boolean {
        // Start narrow, then tune from logs if PhpStorm uses a different ID in your build.
        val suppressedToolIds = setOf(
            // Add/remove based on what your log shows in your PhpStorm build.
            "PhpUnused",
        )
        if (toolId in suppressedToolIds
        ) return true

        val id = toolId.lowercase()
        return (
                "return" in id &&
                        ("type" in id || "phpdoc" in id || "doc" in id) &&
                        ("mismatch" in id || "incompatible" in id)
                )
    }
}

