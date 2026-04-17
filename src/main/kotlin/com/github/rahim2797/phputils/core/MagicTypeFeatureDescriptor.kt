package com.github.rahim2797.phputils.core

data class MagicTypeFeatureDescriptor(
    val id: String,
    val displayName: String,
    val summary: String,
    val capabilities: Set<FeatureCapability>,
    val supportedContexts: List<String>,
    val unsupportedContexts: List<String>,
    val safetyNotes: List<String>,
)
