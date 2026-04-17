package com.github.rahim2797.phputils.properties

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.PhpFile

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

        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }
}
