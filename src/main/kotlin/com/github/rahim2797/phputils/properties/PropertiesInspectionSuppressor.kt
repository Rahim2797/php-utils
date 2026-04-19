package com.github.rahim2797.phputils.properties

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType

class PropertiesInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (element as? PhpFile != null) {
            return false
        }

        if (toolId == "PhpDocSignatureInspection") {
            val phpDocType = PsiTreeUtil.getParentOfType(element, PhpDocType::class.java, false)
            if (phpDocType != null && PropertiesTypeInspector.extractTargetFqns(phpDocType.text, phpDocType).isNotEmpty()) {
                return true
            }
        }

        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }
}
