package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function

object PropertiesTargetResolver {

    fun resolveTargetFqnLocally(receiver: PhpExpression): String? {
        return resolveTargetFqnLocally(receiver, mutableSetOf())
    }

    fun resolveTargetFqnForArrayLiteral(arrayCreation: ArrayCreationExpression): String? {
        resolveFromAssignment(arrayCreation)?.let { return it }
        resolveFromReturn(arrayCreation)?.let { return it }
        resolveFromArgument(arrayCreation)?.let { return it }
        return null
    }

    private fun resolveFromAssignment(arrayCreation: ArrayCreationExpression): String? {
        val assignment = arrayCreation.parent as? AssignmentExpression ?: return null
        if (assignment.value !== arrayCreation) return null

        val target = assignment.variable as? PhpExpression ?: return null
        return resolveTargetFqnLocally(target)
    }

    private fun resolveFromReturn(arrayCreation: ArrayCreationExpression): String? {
        val phpReturn = arrayCreation.parent as? PhpReturn ?: return null
        if (phpReturn.argument !== arrayCreation) return null

        val function = PsiTreeUtil.getParentOfType(
            phpReturn,
            Function::class.java,
            false,
        ) ?: PsiTreeUtil.getParentOfType(
            phpReturn,
            Method::class.java,
            false,
        ) ?: return null

        extractReturnTargetFqn(function)?.let { return it }

        val returnType = function.type
        PropertiesTypeInspector.extractTargetFqn(returnType)?.let { return it }
        PropertiesTypeInspector.extractTargetFqn(returnType.global(function.project))?.let { return it }

        return null
    }

    private fun resolveFromArgument(arrayCreation: ArrayCreationExpression): String? {
        val paramList = arrayCreation.parent as? ParameterList ?: return null
        val parameters = paramList.parameters
        val argIndex = parameters.indexOfFirst { it === arrayCreation }
        if (argIndex < 0) return null

        val call = paramList.parent ?: return null

        return when (call) {
            is FunctionReference -> resolveCallArgumentTarget(call, argIndex)
            is MethodReference -> resolveMethodArgumentTarget(call, argIndex)
            else -> null
        }
    }

    private fun resolveCallArgumentTarget(call: FunctionReference, argIndex: Int): String? {
        for (resolved in call.resolveLocal()) {
            val function = resolved as? Function ?: continue
            extractParamTargetFqn(function, argIndex)?.let { return it }
        }

        val functionName = call.name ?: return null
        val scope = findLocalScope(call) ?: return null

        return PsiTreeUtil.collectElementsOfType(scope, Function::class.java)
            .asSequence()
            .filter { it.name == functionName }
            .sortedByDescending { it.textOffset }
            .mapNotNull { extractParamTargetFqn(it, argIndex) }
            .firstOrNull()
    }
    private fun resolveMethodArgumentTarget(call: MethodReference, argIndex: Int): String? {
        for (resolved in call.multiResolve(false)) {
            val method = resolved.element as? Method ?: continue
            extractParamTargetFqn(method, argIndex)?.let { return it }
        }
        return null
    }

    private fun extractReturnTargetFqn(function: Function): String? {
        val doc = function.docComment ?: PropertiesPhpDocUtils.previousPhpDoc(function) ?: return null
        val returnTag = PsiTreeUtil.findChildrenOfType(doc, PhpDocTag::class.java)
            .firstOrNull { it is PhpDocReturnTag } as? PhpDocReturnTag ?: return null

        return PropertiesTypeInspector.extractTargetFqn(returnTag.declaredType)
    }

    private fun extractParamTargetFqn(function: Function, argIndex: Int): String? {
        val parameter = function.parameters.getOrNull(argIndex) ?: return null

        PropertiesTypeInspector.extractTargetFqn(parameter.type)?.let { return it }
        PropertiesTypeInspector.extractTargetFqn(parameter.type.global(function.project))?.let { return it }

        val doc = function.docComment ?: PropertiesPhpDocUtils.previousPhpDoc(function) ?: return null
        val paramTags = PsiTreeUtil.findChildrenOfType(doc, PhpDocTag::class.java)
            .filterIsInstance<PhpDocParamTag>()

        val paramName = parameter.name
        val matchingTag = paramTags.firstOrNull { tag ->
            tag.text.contains("\$$paramName")
        } ?: return null

        return PropertiesTypeInspector.extractTargetFqn(matchingTag.declaredType)
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