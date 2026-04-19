package com.github.rahim2797.phputils.properties

import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesTypeInspector {
    fun extractTargetFqns(type: PhpType): Set<String> {
        val targets = linkedSetOf<String>()

        for (raw in type.typesWithParametrisedParts) {
            targets += extractTargetFqns(raw)
        }

        return targets
    }

    fun extractTargetFqn(type: PhpType): String? {
        return extractTargetFqns(type).firstOrNull()
    }

    fun extractTargetFqns(raw: String): Set<String> {
        val normalized = stripOuterWrappers(stripPluralSuffix(raw.trim()))
        if (normalized.isBlank()) return emptySet()

        val topLevelParts = splitTopLevel(normalized, '|', '&')
        if (topLevelParts.size > 1) {
            return topLevelParts
                .asSequence()
                .flatMap { extractTargetFqns(it).asSequence() }
                .toCollection(linkedSetOf())
        }

        val base = removeParametrisedType(normalized)
        val parameters = PhpType.getParametrizedParts(normalized)

        if (isPropertiesBase(base)) {
            val firstParameter = parameters.firstOrNull() ?: return emptySet()
            return extractClassLikeTargets(firstParameter)
        }

        if (parameters.isEmpty()) return emptySet()

        return parameters
            .asSequence()
            .flatMap { extractTargetFqns(it).asSequence() }
            .toCollection(linkedSetOf())
    }

    fun extractTargetFqn(raw: String): String? {
        return extractTargetFqns(raw).firstOrNull()
    }

    fun containsPropertiesType(type: PhpType): Boolean {
        return extractTargetFqns(type).isNotEmpty()
    }

    private fun extractClassLikeTargets(raw: String): Set<String> {
        val normalized = stripOuterWrappers(stripPluralSuffix(raw.trim()))
        if (normalized.isBlank()) return emptySet()

        val topLevelParts = splitTopLevel(normalized, '|', '&', ',')
        if (topLevelParts.size > 1) {
            return topLevelParts
                .asSequence()
                .flatMap { extractClassLikeTargets(it).asSequence() }
                .toCollection(linkedSetOf())
        }

        val base = normalizePotentialClassToken(removeParametrisedType(normalized)) ?: return emptySet()
        return linkedSetOf(base)
    }

    private fun isPropertiesBase(base: String): Boolean {
        val normalized = normalizeSignatureToken(base).substringAfterLast('\\')
        return normalized == "Properties"
    }

    private fun normalizePotentialClassToken(raw: String): String? {
        val normalized = normalizeSignatureToken(raw)
        if (normalized.isBlank()) return null

        return when (normalized.lowercase()) {
            "self", "static", "parent" -> normalized.lowercase()
            else -> normalized.takeIf { !PhpType.isPrimitiveType(it) && it != "\\mixed" }
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
                if (part.isNotEmpty()) {
                    parts += part
                }
                current.clear()
                continue
            }

            current.append(char)
        }

        val tail = current.toString().trim()
        if (tail.isNotEmpty()) {
            parts += tail
        }

        return if (parts.isEmpty()) listOf(raw) else parts
    }
}
