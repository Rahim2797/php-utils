package com.github.rahim2797.phputils.properties

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PsiGuards {
    fun getArrayAccessReceiver(literal: StringLiteralExpression): PhpExpression? {
        val parent = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false)
            ?: return null

        val index = parent.index ?: return null
        if (index.value !== literal) return null

        return parent.value as? PhpExpression
    }

    fun isArrayKeyLiteral(literal: StringLiteralExpression): Boolean {
        return getArrayAccessReceiver(literal) != null
    }
}