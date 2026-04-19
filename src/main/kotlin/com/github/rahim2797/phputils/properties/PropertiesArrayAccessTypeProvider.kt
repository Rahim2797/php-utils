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

        // 1) Best case: resolve locally and return the final type directly.
        val localTargetFqns = PropertiesContextResolver.resolveArrayAccessTargetFqns(receiver)
        if (localTargetFqns.isNotEmpty()) {
            val fieldType = PropertiesDumbModeGuards.runSmart(element.project) {
                PropertiesFieldCatalog.getFieldType(element.project, localTargetFqns, key)
            }
            if (fieldType != null && fieldType.types.isNotEmpty()) {
                return fieldType
            }
        }

        // 2) Fallback: defer using an opaque payload.
        val receiverType = receiver.type
        if (receiverType.types.isEmpty()) return null

        val result = PhpType()

        for (receiverRaw in receiverType.types) {
            if (receiverRaw.isBlank()) continue

            val encoded = PropertiesArrayAccessTypeCodec.encode(receiverRaw, key)
            result.add("#${getKey()}$encoded")
        }

        return if (result.types.isEmpty()) null else result
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (PropertiesDumbModeGuards.isDumb(project)) return null

        val prefix = "#${getKey()}"
        if (!expression.startsWith(prefix)) return null

        val payloadText = expression.substring(prefix.length)
        val payload = PropertiesArrayAccessTypeCodec.decode(payloadText) ?: return null

        val targetFqns = linkedSetOf<String>()
        targetFqns += PropertiesTypeInspector.extractTargetFqns(payload.receiverRawType)
        targetFqns += PropertiesDumbModeGuards
            .globalTypeOrNull(PhpType().add(payload.receiverRawType), project)
            ?.let { PropertiesTypeInspector.extractTargetFqns(it) }
            .orEmpty()
        if (targetFqns.isEmpty()) return null

        return PropertiesFieldCatalog.getFieldType(project, targetFqns, payload.key)
    }

    override fun getBySignature(
        expression: String,
        visited: MutableSet<String>,
        depth: Int,
        project: Project
    ) = mutableListOf<PhpNamedElement>()
}
