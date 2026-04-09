package com.github.rahim2797.phputils.properties

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object DebugUtil {
    private val log = Logger.getInstance(DebugUtil::class.java)

    fun logArrayKeyContext(literal: StringLiteralExpression) {
        warn("---- ARRAY KEY LITERAL ----")
        warn("literal text     = ${literal.text}")
        warn("literal contents = ${literal.contents}")
        warn("literal class    = ${literal.javaClass.name}")
        warn("parent class     = ${literal.parent?.javaClass?.name}")

        val arrayAccess = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false) ?: return
        val receiver = arrayAccess.value as? PhpExpression ?: return
        val receiverType: PhpType = receiver.type

        warn("receiver text    = ${receiver.text}")
        warn("receiver class   = ${receiver.javaClass.name}")
        warn("receiver type    = $receiverType")

        for (raw in receiverType.types) {
            warn("receiver raw type = $raw")
        }
    }

    fun warn(message: String) {
        log.warn(message)
    }
}