package com.github.rahim2797.phputils.properties

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PsiGuards {
    fun isArrayKeyLiteral(literal: StringLiteralExpression): Boolean {
        val parent = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false) ?: return false
        val index = parent.index ?: return false
        return index.value === literal
    }
}