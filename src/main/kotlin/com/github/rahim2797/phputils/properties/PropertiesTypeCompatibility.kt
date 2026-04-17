package com.github.rahim2797.phputils.properties

import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesTypeCompatibility {
    fun isArrayCompatible(type: PhpType): Boolean {
        if (type.types.isEmpty()) return true
        return type.containsAll(PhpType.ARRAY)
    }

    fun describe(type: PhpType): String {
        if (type.types.isEmpty()) return "unknown"
        return type.types.joinToString("|") { it.removePrefix("\\") }
    }
}
