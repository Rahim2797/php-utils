package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.github.rahim2797.phputils.magictypes.shapes.MagicTypeShapeService
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.php.PhpIndex

class MagicTypeDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        val matches = MagicTypeTargetResolver.resolveDocMatches(element, originalElement)
        val shape = MagicTypeShapeService.resolveShapes((element ?: originalElement)?.project ?: return null, matches).firstOrNull()
            ?: return null
        return MagicTypeShapeFormatter.formatPopupHtml((element ?: originalElement)!!.project, shape, element ?: originalElement)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? = contextElement

    override fun getDocumentationElementForLink(psiManager: PsiManager, link: String, context: PsiElement): PsiElement? {
        val phpIndex = PhpIndex.getInstance(psiManager.project)
        return when {
            link.startsWith("class:") -> phpIndex.getClassesByFQN(link.removePrefix("class:")).firstOrNull()
            link.startsWith("field:") -> {
                val payload = link.removePrefix("field:")
                val separator = payload.lastIndexOf('#')
                if (separator <= 0 || separator >= payload.lastIndex) return null

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

private object MagicTypeShapeFormatter {
    fun formatPopupHtml(
        project: Project,
        shape: com.github.rahim2797.phputils.magictypes.MagicTypeShape,
        context: PsiElement?
    ): String? {
        val targetFqn = shape.targetFqns.firstOrNull() ?: return null
        val shortClass = targetFqn.substringAfterLast('\\')
        val keys = shape.keys.sortedBy { it.name }

        return buildString {
            append("<div class='definition'><pre>")
            if (keys.isEmpty()) {
                append("array{}")
            } else {
                append("array{\n")
                keys.forEachIndexed { index, key ->
                    append("  ")
                    append(link("field:$targetFqn#${key.name}", key.name))
                    append(": ")
                    append(escape(key.mergedType?.types?.joinToString("|") { it.removePrefix("\\") } ?: "mixed"))
                    if (index < keys.lastIndex) append(",")
                    append("\n")
                }
                append("}")
            }
            append("</pre></div>")
            append("<div class='content'>")
            append("<p><b>Derived from</b> ")
            append(link("class:$targetFqn", "${shape.handler.preferredTypeReference(context)}<$shortClass>"))
            append("</p>")
            append("<p><b>Source class</b> ")
            append(link("class:$targetFqn", targetFqn))
            append("</p>")
            append("</div>")
        }
    }

    private fun link(ref: String, label: String): String {
        val sb = StringBuilder()
        com.intellij.codeInsight.documentation.DocumentationManagerUtil.createHyperlink(sb, ref, escape(label), false)
        return sb.toString()
    }

    private fun escape(s: String): String {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
