package com.github.rahim2797.phputils.magictypes

import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.resolve.types.PhpType

data class MagicTypeShape(
    val handler: MagicTypeFeatureHandler,
    val targetFqns: Set<String>,
    val keys: List<MagicTypeShapeKey>,
) {
    fun key(name: String): MagicTypeShapeKey? = keys.firstOrNull { it.name == name }
}

data class MagicTypeShapeKey(
    val name: String,
    val fields: List<Field>,
    val mergedType: PhpType?,
)
