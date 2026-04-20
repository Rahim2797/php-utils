package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.core.FeatureCapability
import com.github.rahim2797.phputils.core.MagicTypeFeatureDescriptor
import com.github.rahim2797.phputils.magictypes.MagicTypeFeatureHandler
import com.github.rahim2797.phputils.magictypes.MagicTypeShape
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesMagicTypeFeature : MagicTypeFeatureHandler {
    override val descriptor = MagicTypeFeatureDescriptor(
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

    override val shortName: String = PropertiesMagicTypeNames.SHORT_NAME

    override fun matchesBase(base: String, context: PsiElement?): Boolean {
        return PropertiesMagicTypeNames.resolveMagicTypeReference(base, context) != null
    }

    override fun extractTargetFqns(
        parameters: List<String>,
        context: PsiElement?,
        classLikeTargetExtractor: (String, PsiElement?) -> Set<String>
    ): Set<String> {
        val firstParameter = parameters.firstOrNull() ?: return emptySet()
        return classLikeTargetExtractor(firstParameter, context)
    }

    override fun preferredTypeReference(context: PsiElement?): String {
        return PropertiesMagicTypeNames.preferredTypeReference(context)
    }

    override fun buildShape(project: Project, targetFqns: Set<String>): MagicTypeShape {
        return PropertiesShapeBuilder.build(project, this, targetFqns)
    }

    override fun buildContradictionMessage(function: Function, docType: PhpType, targets: Set<String>): String? {
        val declaredReturnType = function.declaredType
        if (PropertiesTypeCompatibility.isArrayCompatible(declaredReturnType)) return null

        val targetFqn = targets.firstOrNull() ?: "mixed"
        val declaredTypeText = PropertiesTypeCompatibility.describe(declaredReturnType)
        return "${PropertiesMagicTypeNames.render(targetFqn, function)} is array-like, but declared return type `$declaredTypeText` is not array-compatible."
    }

    override fun shouldSuppress(toolId: String, phpDocType: PhpDocType): Boolean {
        return toolId == "PhpDocSignatureInspection"
    }
}
