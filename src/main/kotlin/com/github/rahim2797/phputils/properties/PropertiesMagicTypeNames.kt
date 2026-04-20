package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpUse
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesMagicTypeNames {
    const val SHORT_NAME = "Properties"
    const val QUALIFIED_NAME = "Rahim2797\\MagicTypes\\Properties"
    const val CANONICAL_FQN = "\\$QUALIFIED_NAME"

    fun isCanonicalFqn(raw: String): Boolean {
        return normalizeAbsoluteFqn(raw) == CANONICAL_FQN
    }

    fun resolveClassLikeName(raw: String, context: PsiElement?): String? {
        val normalized = raw.trim().removePrefix("?")
        if (normalized.isBlank()) return null

        return when (normalized.lowercase()) {
            "self", "static", "parent" -> normalized.lowercase()
            else -> resolveNamedType(normalized, context)
        }
    }

    fun resolveMagicTypeReference(raw: String, context: PsiElement?): String? {
        return resolveNamedType(raw.trim().removePrefix("?"), context)
            ?.takeIf(::isCanonicalFqn)
    }

    fun preferredTypeReference(context: PsiElement?): String {
        val alias = canonicalImportAlias(context)
        return alias ?: CANONICAL_FQN
    }

    fun render(targetFqn: String, context: PsiElement?): String {
        return "${preferredTypeReference(context)}<$targetFqn>"
    }

    fun canonicalImportAlias(context: PsiElement?): String? {
        val imports = classLikeImports(context)
        return imports.entries
            .firstOrNull { (_, fqn) -> fqn == CANONICAL_FQN }
            ?.key
    }

    private fun resolveNamedType(raw: String, context: PsiElement?): String? {
        if (raw.isBlank() || PhpType.isPrimitiveType(raw)) return null
        if (raw == "\\mixed") return null

        if (raw.startsWith("\\")) {
            return normalizeAbsoluteFqn(raw)
        }

        val imports = classLikeImports(context)
        val root = raw.substringBefore('\\')
        val suffix = raw.substringAfter('\\', "")
        val imported = imports[root]
        if (imported != null) {
            return if (suffix.isBlank()) imported else "$imported\\$suffix"
        }

        val namespace = (context?.containingFile as? PhpFile)
            ?.mainNamespaceName
            ?.trim('\\')
            .orEmpty()

        return if (namespace.isBlank()) {
            "\\$raw"
        } else {
            "\\$namespace\\$raw"
        }
    }

    private fun classLikeImports(context: PsiElement?): Map<String, String> {
        val file = context?.containingFile as? PhpFile ?: return emptyMap()
        val imports = linkedMapOf<String, String>()

        for (phpUse in PsiTreeUtil.findChildrenOfType(file, PhpUse::class.java)) {
            if (phpUse.isOfConst || phpUse.isOfFunction || phpUse.isTraitImport) continue

            val rawTarget = phpUse.targetReference?.text
                ?.takeIf { it.isNotBlank() }
                ?: phpUse.text
                    .removePrefix("use")
                    .substringBefore(';')
                    .substringBefore(" as ")
                    .trim()
            val fqn = rawTarget
                .takeIf { it.isNotBlank() }
                ?.let(::normalizeAbsoluteFqn)
                ?: continue

            val alias = (phpUse.aliasName ?: phpUse.name).trimStart('\\')
            if (alias.isNotBlank()) {
                imports[alias] = fqn
            }
        }

        return imports
    }

    private fun normalizeAbsoluteFqn(raw: String): String {
        return "\\" + raw.trim().trimStart('\\')
    }
}
