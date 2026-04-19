package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.core.FeatureCapability
import com.github.rahim2797.phputils.core.MagicTypeFeatureDescriptor

object PropertiesMagicTypeFeature {
    val descriptor = MagicTypeFeatureDescriptor(
        id = "properties",
        displayName = "Rahim2797\\MagicTypes\\Properties<T>",
        summary = "Treats Rahim2797\\MagicTypes\\Properties<T> as an array-like shape keyed by fields on class T.",
        capabilities = setOf(
            FeatureCapability.TYPE_INFERENCE,
            FeatureCapability.PSI_REFERENCES,
            FeatureCapability.COMPLETION,
            FeatureCapability.DOCUMENTATION,
            FeatureCapability.INSPECTIONS,
        ),
        supportedContexts = listOf(
            "PHPDoc @var annotations on local variables",
            "PHPDoc @return annotations paired with array-compatible native return types",
            "PHPDoc @param annotations paired with array parameters",
            "Array access keys such as \$props['name']",
            "Array literals assigned to Rahim2797\\MagicTypes\\Properties<T>-annotated locals",
            "Array literals returned from Rahim2797\\MagicTypes\\Properties<T>-annotated functions",
            "Array literals passed to Rahim2797\\MagicTypes\\Properties<T>-annotated parameters",
        ),
        unsupportedContexts = listOf(
            "Nested utility types such as Pick<Properties<T>, ...>",
            "Object property access semantics on Rahim2797\\MagicTypes\\Properties<T>",
            "Automatic contradiction fixes for every built-in PhpStorm inspection",
        ),
        safetyNotes = listOf(
            "Augments PhpStorm behavior conservatively and should not replace built-in behavior wholesale.",
            "Inspection suppression is intentionally narrow and only used where the plugin already provides explicit handling.",
            "The plugin provides an IDE-only stub for Rahim2797\\MagicTypes\\Properties, so runtime installation is not required for PHPDoc-only usage.",
        ),
    )
}
