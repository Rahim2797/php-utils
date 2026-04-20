package com.github.rahim2797.phputils.magictypes

data class MagicTypeMatch(
    val handler: MagicTypeFeatureHandler,
    val targetFqns: Set<String>
) {
    companion object {
        fun merge(matches: Collection<MagicTypeMatch>): List<MagicTypeMatch> {
            return matches
                .groupBy { it.handler.descriptor.id }
                .values
                .map { grouped ->
                    MagicTypeMatch(
                        grouped.first().handler,
                        grouped.flatMapTo(linkedSetOf()) { it.targetFqns }
                    )
                }
        }
    }
}
