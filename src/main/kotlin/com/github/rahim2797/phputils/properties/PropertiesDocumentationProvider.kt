package com.github.rahim2797.phputils.properties

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.php.PhpIndex

class PropertiesDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val targetFqn = PropertiesContextResolver.resolveDocTargetFqn(element, originalElement) ?: return null
        return PropertiesShapeFormatter.formatPopupHtml(
            (element ?: originalElement)?.project ?: return null,
            targetFqn,
            null
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
}
