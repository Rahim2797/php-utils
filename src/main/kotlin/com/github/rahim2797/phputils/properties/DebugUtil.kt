package com.github.rahim2797.phputils.properties

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object DebugUtil {
    private val log = Logger.getInstance(DebugUtil::class.java)

    fun logArrayKeyContext(literal: StringLiteralExpression) {
        log.warn("---- ARRAY KEY LITERAL ----")
        log.warn("literal text     = ${literal.text}")
        log.warn("literal contents = ${literal.contents}")
        log.warn("literal class    = ${literal.javaClass.name}")
        log.warn("parent class     = ${literal.parent?.javaClass?.name}")

        val arrayAccess = literal.parent as? ArrayAccessExpression ?: return
        val receiver = arrayAccess.value as? PhpExpression ?: return
        val receiverType: PhpType = receiver.type

        log.warn("receiver text    = ${receiver.text}")
        log.warn("receiver class   = ${receiver.javaClass.name}")
        log.warn("receiver type    = $receiverType")
    }
}