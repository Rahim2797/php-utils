package com.github.rahim2797.phputils.properties

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType

class PropertiesDocumentationProvider : AbstractDocumentationProvider() {

    data class PropertiesDocTarget(
        val targetFqn: String,
        val variableName: String? = null
    )

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val target = resolveDocTarget(element, originalElement) ?: return null
        val variableName = target.variableName?.let { "\$$it" }
        return PropertiesShapeFormatter.formatPopupHtml(
            (element ?: originalElement)?.project ?: return null,
            target.targetFqn,
            variableName
        )
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        return contextElement
    }

    override fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement
    ): PsiElement? {
        val phpIndex = PhpIndex.getInstance(psiManager.project)

        return when {
            link.startsWith("class:") -> {
                val fqn = link.removePrefix("class:")
                phpIndex.getClassesByFQN(fqn).firstOrNull()
            }

            link.startsWith("field:") -> {
                val payload = link.removePrefix("field:")
                val separator = payload.lastIndexOf('#')
                if (separator <= 0 || separator >= payload.lastIndex) {
                    return null
                }

                val classFqn = payload.substring(0, separator)
                val fieldName = payload.substring(separator + 1)

                phpIndex.getClassesByFQN(classFqn)
                    .asSequence()
                    .mapNotNull { it.findFieldByName(fieldName, false) }
                    .firstOrNull()
            }

            else -> null
        }
    }

    private fun resolveDocTarget(
        element: PsiElement?,
        originalElement: PsiElement?
    ): PropertiesDocTarget? {
        val target = element ?: originalElement ?: return null

        var current: PsiElement? = target
        repeat(8) {
            if (current == null) return null

            val phpDocType = current as? PhpDocType
            phpDocType?.let { return resolveFromPhpDoc(it) }

            current = current.parent
        }

        return null
    }

    private fun resolveFromPhpDoc(element: PhpDocType): PropertiesDocTarget? {
        val declaredType = element.declaredType
        val targetFqn = PropertiesTypeInspector.extractTargetFqn(declaredType)
        return targetFqn?.let { PropertiesDocTarget(targetFqn = it) }
    }
}