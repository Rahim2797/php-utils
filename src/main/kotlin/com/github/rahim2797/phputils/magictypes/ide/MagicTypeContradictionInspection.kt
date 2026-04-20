package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.parser.MagicTypeParser
import com.github.rahim2797.phputils.properties.PropertiesPhpDocUtils
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

class MagicTypeContradictionInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpFunction(function: Function) {
                val returnTag = PropertiesPhpDocUtils.findReturnTag(
                    function.docComment ?: PropertiesPhpDocUtils.previousPhpDoc(function) ?: return
                ) ?: return
                val matches = MagicTypeParser.extractMatches(returnTag.declaredType, returnTag)
                if (matches.isEmpty()) return

                val highlightedElement = PsiTreeUtil.findChildOfType(returnTag, PhpDocType::class.java) ?: returnTag
                for (match in matches) {
                    val message = match.handler.buildContradictionMessage(function, returnTag.declaredType, match.targetFqns)
                        ?: continue
                    holder.registerProblem(highlightedElement, message)
                }
            }
        }
    }
}
