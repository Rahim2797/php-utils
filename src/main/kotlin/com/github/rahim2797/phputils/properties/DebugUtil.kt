package com.github.rahim2797.phputils.properties

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object DebugUtil {
    private val log = Logger.getInstance("com.github.rahim2797")

    fun logArrayKeyContext(literal: StringLiteralExpression) {
        this.debug("---- ARRAY KEY LITERAL ----")
        this.debug("literal text     = ${literal.text}")
        this.debug("literal contents = ${literal.contents}")
        this.debug("literal class    = ${literal.javaClass.name}")
        this.debug("parent class     = ${literal.parent?.javaClass?.name}")

        val arrayAccess = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false) ?: return
        val receiver = arrayAccess.value as? PhpExpression ?: return
        val receiverType: PhpType = receiver.type

        this.debug("receiver text    = ${receiver.text}")
        this.debug("receiver class   = ${receiver.javaClass.name}")
        this.debug("receiver type    = $receiverType")
    }

    fun debug(message: String) {
        log.debug(message)
    }
}