package com.github.rahim2797.phputils.magictypes

import com.github.rahim2797.phputils.properties.PropertiesMagicTypeFeature

object MagicTypeRegistry {
    val handlers: List<MagicTypeFeatureHandler> = listOf(
        PropertiesMagicTypeFeature,
    )

    val features = handlers.map { it.descriptor }
}
