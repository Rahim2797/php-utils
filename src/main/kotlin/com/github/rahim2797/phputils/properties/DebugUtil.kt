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
        this.warn("---- ARRAY KEY LITERAL ----")
        this.warn("literal text     = ${literal.text}")
        this.warn("literal contents = ${literal.contents}")

        val arrayAccess = PsiTreeUtil.getParentOfType(literal, ArrayAccessExpression::class.java, false) ?: return
        val receiver = arrayAccess.value as? PhpExpression ?: return
        val receiverType: PhpType = receiver.type

        this.warn("receiver text    = ${receiver.text}")
        this.warn("receiver type    = $receiverType")

        for (raw in receiverType.types) {
            this.warn("receiver raw type = $raw")
        }

        val targetFqn = PropertiesTypeInspector.extractTargetFqn(receiverType)
        this.warn("properties target = $targetFqn")
    }

    fun warn(message: String) {
        log.warn(message)
    }
}