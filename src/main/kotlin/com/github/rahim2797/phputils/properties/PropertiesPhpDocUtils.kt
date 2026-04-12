package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.Function

object PropertiesPhpDocUtils {
    fun findReturnTag(doc: PhpDocComment): PhpDocReturnTag? {
        return PsiTreeUtil.findChildrenOfType(doc, PhpDocTag::class.java)
            .firstOrNull { it as? PhpDocReturnTag !== null } as PhpDocReturnTag?
    }

    fun previousPhpDoc(function: Function): PhpDocComment? {
        var current: PsiElement? = function.prevSibling
        while (current != null) {
            when {
                current is PhpDocComment -> return current
                current.text.isBlank() -> current = current.prevSibling
                else -> return null
            }
        }
        return null
    }
}
