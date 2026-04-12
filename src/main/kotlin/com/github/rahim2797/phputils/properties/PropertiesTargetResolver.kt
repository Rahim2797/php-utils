package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.Variable

object PropertiesTargetResolver {

    fun resolveTargetFqnLocally(receiver: PhpExpression): String? {
        return resolveTargetFqnLocally(receiver, mutableSetOf())
    }

    private fun resolveTargetFqnLocally(
        expression: PhpExpression?,
        visited: MutableSet<PsiElement>
    ): String? {
        if (expression == null) return null
        if (!visited.add(expression)) return null

        extractFromExpression(expression)?.let { return it }

        if (expression is Variable) {
            extractFromVariableDoc(expression)?.let { return it }

            findLatestAssignment(expression)?.let { assignment ->
                extractFromAssignmentDoc(assignment, expression)?.let { return it }

                val assignedValue = assignment.value as PhpExpression?
                extractFromExpression(assignedValue)?.let { return it }

                resolveTargetFqnLocally(assignedValue, visited)?.let { return it }
            }
        }

        return null
    }

    private fun extractFromExpression(expression: PhpExpression?): String? {
        if (expression == null) return null

        PropertiesTypeInspector.extractTargetFqn(expression.type)?.let { return it }
        PropertiesTypeInspector.extractTargetFqn(expression.type.global(expression.project))?.let { return it }

        return null
    }

    private fun extractFromVariableDoc(variable: Variable): String? {
        val name = variable.name

        val doc = previousDocComment(variable) ?: return null
        val text = doc.text

        return extractTargetFqnFromDoc(text, name)
    }

    private fun extractFromAssignmentDoc(
        assignment: AssignmentExpression,
        variable: Variable
    ): String? {
        val name = variable.name

        val doc = previousDocComment(assignment) ?: return null
        val text = doc.text

        return extractTargetFqnFromDoc(text, name)
    }

    private fun extractTargetFqnFromDoc(docText: String, variableName: String): String? {
        val normalizedVar = "\\$$variableName"

        val exactVarPattern = Regex(
            """@var\s+([^\s*]+)\s+$normalizedVar\b"""
        )
        exactVarPattern.find(docText)?.groupValues?.getOrNull(1)?.let { raw ->
            PropertiesTypeInspector.extractTargetFqn(raw)?.let { return it }
        }

        val loosePattern = Regex(
            """@var\s+([^\s*]+)"""
        )
        loosePattern.find(docText)?.groupValues?.getOrNull(1)?.let { raw ->
            PropertiesTypeInspector.extractTargetFqn(raw)?.let { return it }
        }

        return null
    }

    private fun findLatestAssignment(variable: Variable): AssignmentExpression? {
        val variableName = variable.name
        val scope = findLocalScope(variable) ?: return null

        val assignments = PsiTreeUtil.collectElementsOfType(scope, AssignmentExpression::class.java)

        return assignments
            .asSequence()
            .filter { it.textOffset < variable.textOffset }
            .filter { isAssignmentToVariable(it, variableName) }
            .maxByOrNull { it.textOffset }
    }

    private fun isAssignmentToVariable(
        assignment: AssignmentExpression,
        variableName: String
    ): Boolean {
        val target = assignment.variable as? Variable ?: return false
        return target.name == variableName
    }

    private fun findLocalScope(element: PsiElement): PsiElement? {
        return PsiTreeUtil.getParentOfType(
            element,
            com.jetbrains.php.lang.psi.elements.Function::class.java,
            com.jetbrains.php.lang.psi.elements.Method::class.java,
            com.jetbrains.php.lang.psi.PhpFile::class.java,
        )
    }

    private fun previousDocComment(element: PsiElement): PsiComment? {
        var current = element.prevSibling
        while (current != null) {
            when {
                current is PsiComment && current.text.startsWith("/**") -> return current
                current.text.isBlank() -> current = current.prevSibling
                else -> return null
            }
        }
        return null
    }
}