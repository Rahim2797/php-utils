package com.github.rahim2797.phputils.properties

import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesTypeInspector {
    fun extractTargetFqn(type: PhpType): String? {
        for (raw in type.typesWithParametrisedParts) {
            val parsed = extractTargetFqn(raw)
            if (parsed != null) return parsed
        }
        return null
    }

    fun extractTargetFqn(raw: String): String? {
        // Example input:
        // #C\Properties<\User>
        // or maybe union/intermediate variations depending on context

        val marker = "\\Properties<"
        val start = raw.indexOf(marker)
        if (start < 0) return null

        val genericStart = start + marker.length
        val genericEnd = raw.indexOf('>', genericStart)
        if (genericEnd < 0) return null

        val inner = raw.substring(genericStart, genericEnd).trim()
        return inner.takeIf { it.isNotEmpty() }
    }

    fun containsPropertiesType(type: PhpType): Boolean {
        return extractTargetFqn(type) != null
    }
}