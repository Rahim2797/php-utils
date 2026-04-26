package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.parser.MagicTypeParser
import com.github.rahim2797.phputils.magictypes.shapes.MagicTypeShapeService
import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.github.rahim2797.phputils.properties.PropertiesArrayAccessTypeCodec
import com.github.rahim2797.phputils.properties.PropertiesDumbModeGuards
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4

class MagicTypeArrayAccessTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = 'Q'

    override fun getType(element: PsiElement): PhpType? {
        val arrayAccess = element as? ArrayAccessExpression ?: return null
        val receiver = arrayAccess.value as? PhpExpression ?: return null
        val index = arrayAccess.index ?: return null
        val keyLiteral = index.value as? StringLiteralExpression ?: return null
        val key = keyLiteral.contents
        if (key.isBlank()) return null

        val localMatches = MagicTypeTargetResolver.resolveArrayAccessMatches(receiver)
        if (localMatches.isNotEmpty()) {
            val fieldType = PropertiesDumbModeGuards.runSmart(element.project) {
                MagicTypeShapeService.fieldType(element.project, localMatches, key)
            }
            if (fieldType != null && fieldType.types.isNotEmpty()) {
                return fieldType
            }
        }

        val receiverType = PropertiesDumbModeGuards.safeExpressionType(
            receiver,
            "array access receiver type lookup"
        ) ?: return null
        if (receiverType.types.isEmpty()) return null

        val result = PhpType()
        for (receiverRaw in receiverType.types) {
            if (receiverRaw.isBlank()) continue
            val encoded = PropertiesArrayAccessTypeCodec.encode(receiverRaw, key)
            result.add("#${getKey()}$encoded")
        }
        return result.takeIf { it.types.isNotEmpty() }
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (PropertiesDumbModeGuards.isDumb(project)) return null

        val prefix = "#${getKey()}"
        if (!expression.startsWith(prefix)) return null

        val payload = PropertiesArrayAccessTypeCodec.decode(expression.substring(prefix.length)) ?: return null
        val matches = mutableListOf<com.github.rahim2797.phputils.magictypes.MagicTypeMatch>()
        matches += MagicTypeParser.extractMatches(payload.receiverRawType)
        PropertiesDumbModeGuards.globalTypeOrNull(PhpType().add(payload.receiverRawType), project)
            ?.let { matches += MagicTypeParser.extractMatches(it) }
        if (matches.isEmpty()) return null

        return MagicTypeShapeService.fieldType(project, matches, payload.key)
    }

    override fun getBySignature(
        expression: String,
        visited: MutableSet<String>,
        depth: Int,
        project: Project
    ) = mutableListOf<PhpNamedElement>()
}
