package com.github.rahim2797.phputils.magictypes.ide

import com.github.rahim2797.phputils.magictypes.parser.MagicTypeParser
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import java.util.ArrayDeque

object MagicTypeDocContextResolver {
    fun resolve(element: PsiElement?, originalElement: PsiElement? = null): PhpDocType? {
        val seeds = linkedSetOf<PsiElement>()
        collectSeeds(element, seeds)
        collectSeeds(originalElement, seeds)

        for (seed in seeds) {
            resolveFromAncestors(seed)?.let { return it }
        }

        for (seed in seeds) {
            resolveFromEnclosingDoc(seed)?.let { return it }
        }

        return null
    }

    private fun collectSeeds(element: PsiElement?, sink: MutableSet<PsiElement>) {
        if (element == null) return

        val queue = ArrayDeque<PsiElement>()
        queue += element

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!sink.add(current)) continue

            current.navigationElement
                ?.takeIf { it !== current }
                ?.let(queue::addLast)

            current.references
                .asSequence()
                .map(PsiReference::getElement)
                .filter { it !== current }
                .forEach(queue::addLast)
        }
    }

    private fun resolveFromAncestors(element: PsiElement): PhpDocType? {
        var current: PsiElement? = element
        repeat(12) {
            when (current) {
                is PhpDocType -> {
                    if (MagicTypeParser.extractMatches(current.text, current).isNotEmpty()) {
                        return current
                    }
                }
                null -> return null
            }
            current = current.parent
        }
        return null
    }

    private fun resolveFromEnclosingDoc(element: PsiElement): PhpDocType? {
        val docComment = PsiTreeUtil.getParentOfType(element, PhpDocComment::class.java, false)
            ?: return null
        val candidates = PsiTreeUtil.findChildrenOfType(docComment, PhpDocType::class.java)
            .filter { MagicTypeParser.extractMatches(it.text, it).isNotEmpty() }
        if (candidates.isEmpty()) return null

        val matchingByRange = candidates.firstOrNull { candidate ->
            candidate.textRange.intersectsStrict(element.textRange)
        }
        return matchingByRange ?: candidates.singleOrNull() ?: candidates.firstOrNull()
    }
}
