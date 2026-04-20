package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.PhpFile

class MagicTypeInspectionSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (element as? PhpFile != null) return false
        if (toolId != "PhpDocSignatureInspection") return false

        val phpDocType = MagicTypeDocContextResolver.resolve(element) ?: return false
        val matches = MagicTypeTargetResolver.resolveDocMatches(phpDocType, phpDocType)
        return matches.any { it.handler.shouldSuppress(toolId, phpDocType) }
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }
}
