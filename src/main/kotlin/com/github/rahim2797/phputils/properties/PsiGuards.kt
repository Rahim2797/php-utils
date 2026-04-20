package com.github.rahim2797.phputils.properties

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.*


object PsiGuards {
    const val DUMMY_IDENTIFIER: String = "IntellijIdeaRulezzz "

    fun getArrayAccessReceiver(literal: StringLiteralExpression): PhpExpression? {
        val parent = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false)
            ?: return null

        val index = parent.index ?: return null
        if (index.value !== literal) return null

        return parent.value as? PhpExpression
    }

    fun getDirectArrayHashElement(literal: StringLiteralExpression): PhpPsiElement? {
        if (literal.contents.isBlank() || literal.contents == DUMMY_IDENTIFIER) {
            return literal.parent as PhpPsiElement
        }
        val hash = PsiTreeUtil.getParentOfType(literal, ArrayHashElement::class.java, false) ?: return null
        if (hash.key !== literal) return null
        return hash
    }

    fun getOwningArrayCreation(literal: StringLiteralExpression): ArrayCreationExpression? {
        val hash = getDirectArrayHashElement(literal) ?: return null
        return PsiTreeUtil.getParentOfType(hash, ArrayCreationExpression::class.java, false)
    }

    fun getContextualizedArrayExpression(arrayCreation: ArrayCreationExpression): PhpExpression {
        var current: PhpExpression = arrayCreation

        while (true) {
            val parent = current.parent
            current = when {
                parent is ParenthesizedExpression && parent.argument === current -> parent
                parent is TernaryExpression &&
                    (parent.trueVariant === current || parent.falseVariant === current) -> parent
                parent is BinaryExpression &&
                    parent.operation?.text == "??" &&
                    (parent.leftOperand === current || parent.rightOperand === current) -> parent
                else -> return current
            }
        }
    }

    fun isArrayLiteralKeyContext(literal: StringLiteralExpression): Boolean {
        return getOwningArrayCreation(literal) != null
    }

    fun isArrayKeyLiteral(literal: StringLiteralExpression): Boolean {
        return getArrayAccessReceiver(literal) != null
    }
}
