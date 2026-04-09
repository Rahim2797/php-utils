package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

class PropertiesArrayAccessTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = 'Q'

    override fun getType(element: PsiElement): PhpType? {
        val arrayAccess = element as? ArrayAccessExpression ?: return null
        val receiver = arrayAccess.value as? PhpExpression ?: return null
        val index = arrayAccess.index ?: return null
        val keyLiteral = index.value as? StringLiteralExpression ?: return null
        val key = keyLiteral.contents
        if (key.isBlank()) return null

        val receiverType = receiver.type
        if (receiverType.types.isEmpty()) return null

        val result = PhpType()

        for (receiverRaw in receiverType.types) {
            val encoded = PropertiesArrayAccessTypeCodec.encode(receiverRaw, key)
            result.add("#${getKey()}$encoded")
        }

        DebugUtil.warn("Array access getType element=${element.text} receiverType=$receiverType key=$key result=$result")
        return result
    }

    override fun complete(expression: String, project: Project): PhpType? {
        val prefix = "#${getKey()}"
        if (!expression.startsWith(prefix)) return null

        val payloadText = expression.substring(prefix.length)
        val payload = PropertiesArrayAccessTypeCodec.decode(payloadText) ?: return null

        val resolvedReceiverType = PhpType().add(payload.receiverRawType).global(project)
        val targetFqn = PropertiesTypeInspector.extractTargetFqn(resolvedReceiverType) ?: return null

        val field = PropertiesFieldResolver.findField(project, targetFqn, payload.key) ?: return null
        val fieldType = field.type
        if (fieldType.types.isEmpty()) return null

        DebugUtil.warn(
            "Array access complete expression=$expression receiverRaw=${payload.receiverRawType} " +
                "resolvedReceiverType=$resolvedReceiverType targetFqn=$targetFqn key=${payload.key} fieldType=$fieldType"
        )

        return fieldType
    }

    override fun getBySignature(
        expression: String,
        visited: MutableSet<String>,
        depth: Int,
        project: Project
    ) = mutableListOf<PhpNamedElement>()
}