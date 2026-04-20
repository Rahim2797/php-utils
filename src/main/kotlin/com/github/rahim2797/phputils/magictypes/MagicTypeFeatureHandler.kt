package com.github.rahim2797.phputils.magictypes

import com.github.rahim2797.phputils.core.MagicTypeFeatureDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpType

interface MagicTypeFeatureHandler {
    val descriptor: MagicTypeFeatureDescriptor

    val shortName: String

    fun matchesBase(base: String, context: PsiElement?): Boolean

    fun extractTargetFqns(
        parameters: List<String>,
        context: PsiElement?,
        classLikeTargetExtractor: (String, PsiElement?) -> Set<String>
    ): Set<String>

    fun preferredTypeReference(context: PsiElement?): String

    fun buildShape(project: Project, targetFqns: Set<String>): MagicTypeShape

    fun buildContradictionMessage(
        function: Function,
        docType: PhpType,
        targets: Set<String>
    ): String? = null

    fun shouldSuppress(toolId: String, phpDocType: PhpDocType): Boolean = false
}
