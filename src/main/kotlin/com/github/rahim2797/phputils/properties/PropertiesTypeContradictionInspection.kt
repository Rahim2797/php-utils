package com.github.rahim2797.phputils.properties

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

class PropertiesTypeContradictionInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFunction(function: Function) {
                val returnTag = PropertiesPhpDocUtils.findReturnTag(
                    function.docComment ?: PropertiesPhpDocUtils.previousPhpDoc(function) ?: return
                ) ?: return
                val docType = returnTag.declaredType
                if (!PropertiesTypeInspector.containsPropertiesType(docType)) return

                val declaredReturnType = function.declaredType
                if (PropertiesTypeCompatibility.isArrayCompatible(declaredReturnType)) return

                val targetFqn = PropertiesTypeInspector.extractTargetFqn(docType)
                val declaredTypeText = PropertiesTypeCompatibility.describe(declaredReturnType)
                val highlightedElement =
                    PsiTreeUtil.findChildOfType(returnTag, PhpDocType::class.java) ?: returnTag
                holder.registerProblem(
                    highlightedElement,
                    "Properties<$targetFqn> is array-like, but declared return type `$declaredTypeText` is not array-compatible."
                )
            }
        }
    }
}
