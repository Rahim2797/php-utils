package com.github.rahim2797.phputils.magictypes.parser

import com.github.rahim2797.phputils.magictypes.MagicTypeMatch
import com.github.rahim2797.phputils.magictypes.MagicTypeRegistry
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object MagicTypeParser {
    fun extractMatches(type: PhpType, context: PsiElement? = null): List<MagicTypeMatch> {
        val matches = linkedSetOf<MagicTypeMatch>()

        for (raw in type.typesWithParametrisedParts) {
            matches += extractMatches(raw, context)
        }

        return MagicTypeMatch.merge(matches)
    }

    fun extractMatches(raw: String, context: PsiElement? = null): List<MagicTypeMatch> {
        val normalized = stripOuterWrappers(stripPluralSuffix(raw.trim()))
        if (normalized.isBlank()) return emptyList()

        val topLevelParts = splitTopLevel(normalized, '|', '&')
        if (topLevelParts.size > 1) {
            return MagicTypeMatch.merge(
                topLevelParts.flatMap { extractMatches(it, context) }
            )
        }

        val base = removeParametrisedType(normalized)
        val normalizedBase = normalizeSignatureToken(base)
        val parameters = PhpType.getParametrizedParts(normalized)

        val directMatches = MagicTypeRegistry.handlers
            .asSequence()
            .filter { it.matchesBase(normalizedBase, context) }
            .mapNotNull { handler ->
                val targets = handler.extractTargetFqns(parameters, context, ::extractClassLikeTargets)
                targets.takeIf { it.isNotEmpty() }?.let { MagicTypeMatch(handler, it) }
            }
            .toList()
        if (directMatches.isNotEmpty()) {
            return MagicTypeMatch.merge(directMatches)
        }

        if (parameters.isEmpty()) return emptyList()
        return MagicTypeMatch.merge(parameters.flatMap { extractMatches(it, context) })
    }

    fun extractTargetFqns(type: PhpType, context: PsiElement? = null): Set<String> {
        return extractMatches(type, context).flatMapTo(linkedSetOf()) { it.targetFqns }
    }

    fun extractTargetFqns(raw: String, context: PsiElement? = null): Set<String> {
        return extractMatches(raw, context).flatMapTo(linkedSetOf()) { it.targetFqns }
    }

    fun extractFirstMatch(type: PhpType, context: PsiElement? = null): MagicTypeMatch? {
        return extractMatches(type, context).firstOrNull()
    }

    fun extractFirstMatch(raw: String, context: PsiElement? = null): MagicTypeMatch? {
        return extractMatches(raw, context).firstOrNull()
    }

    fun extractClassLikeTargets(raw: String, context: PsiElement?): Set<String> {
        val normalized = stripOuterWrappers(stripPluralSuffix(raw.trim()))
        if (normalized.isBlank()) return emptySet()

        val topLevelParts = splitTopLevel(normalized, '|', '&', ',')
        if (topLevelParts.size > 1) {
            return topLevelParts
                .flatMapTo(linkedSetOf()) { extractClassLikeTargets(it, context) }
        }

        val base = normalizePotentialClassToken(removeParametrisedType(normalized), context) ?: return emptySet()
        return linkedSetOf(base)
    }

    private fun normalizePotentialClassToken(raw: String, context: PsiElement?): String? {
        val normalized = normalizeSignatureToken(raw)
        if (normalized.isBlank()) return null

        return when (normalized.lowercase()) {
            "self", "static", "parent" -> normalized.lowercase()
            else -> com.github.rahim2797.phputils.properties.PropertiesMagicTypeNames.resolveClassLikeName(normalized, context)
        }
    }

    private fun normalizeSignatureToken(raw: String): String {
        var normalized = stripOuterWrappers(stripPluralSuffix(raw.trim()))

        while (normalized.startsWith("#") && normalized.length > 2 && normalized[1] != '-') {
            normalized = normalized.substring(2)
        }

        val memberSeparator = normalized.indexOf('.')
        if (memberSeparator > 0) {
            normalized = normalized.substring(0, memberSeparator)
        }

        return normalized
    }

    private fun removeParametrisedType(raw: String): String {
        return try {
            PhpType.removeParametrisedType(raw)
        } catch (_: Throwable) {
            raw.substringBefore('<')
        }
    }

    private fun stripOuterWrappers(raw: String): String {
        var current = raw.trim()
        while (
            current.length >= 2 &&
            ((current.startsWith("(") && current.endsWith(")")) ||
                (current.startsWith("[") && current.endsWith("]")))
        ) {
            current = current.substring(1, current.length - 1).trim()
        }
        return current
    }

    private fun stripPluralSuffix(raw: String): String {
        var current = raw
        while (current.endsWith("[]")) {
            current = current.dropLast(2)
        }
        return current
    }

    private fun splitTopLevel(raw: String, vararg separators: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var angleDepth = 0
        var braceDepth = 0
        var bracketDepth = 0
        var parenDepth = 0

        for (char in raw) {
            when (char) {
                '<' -> angleDepth++
                '>' -> angleDepth = (angleDepth - 1).coerceAtLeast(0)
                '{' -> braceDepth++
                '}' -> braceDepth = (braceDepth - 1).coerceAtLeast(0)
                '[' -> bracketDepth++
                ']' -> bracketDepth = (bracketDepth - 1).coerceAtLeast(0)
                '(' -> parenDepth++
                ')' -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
            }

            val isTopLevel = angleDepth == 0 && braceDepth == 0 && bracketDepth == 0 && parenDepth == 0
            if (isTopLevel && char in separators) {
                val part = current.toString().trim()
                if (part.isNotEmpty()) parts += part
                current.clear()
                continue
            }

            current.append(char)
        }

        val tail = current.toString().trim()
        if (tail.isNotEmpty()) parts += tail
        return if (parts.isEmpty()) listOf(raw) else parts
    }
}
