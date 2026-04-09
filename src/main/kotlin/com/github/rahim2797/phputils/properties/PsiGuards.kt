package com.github.rahim2797.phputils.properties

import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

object PsiGuards {
    fun isArrayKeyLiteral(literal: StringLiteralExpression): Boolean {
        val parent = literal.parent as? ArrayAccessExpression ?: return false
        return parent.index === literal
    }
}