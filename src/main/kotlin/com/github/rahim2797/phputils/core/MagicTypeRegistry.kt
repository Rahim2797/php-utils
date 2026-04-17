package com.github.rahim2797.phputils.core

import com.github.rahim2797.phputils.properties.PropertiesMagicTypeFeature

object MagicTypeRegistry {
    val features: List<MagicTypeFeatureDescriptor> = listOf(
        PropertiesMagicTypeFeature.descriptor,
    )
}
