package com.github.rahim2797.phputils.properties

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocReturnTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.ParenthesizedExpression
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.PhpReturn
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesTargetResolver {

    fun resolveTargetFqnLocally(receiver: PhpExpression): String? {
        return resolveTargetFqnsLocally(receiver).firstOrNull()
    }

    fun resolveTargetFqnsLocally(receiver: PhpExpression): Set<String> {
        return resolveTargetFqnsLocally(receiver, mutableSetOf(), allowIndexAccess = true)
    }

    fun resolveTargetFqnLocallyWithoutIndexes(receiver: PhpExpression): String? {
        return resolveTargetFqnsLocallyWithoutIndexes(receiver).firstOrNull()
    }

    fun resolveTargetFqnsLocallyWithoutIndexes(receiver: PhpExpression): Set<String> {
        return resolveTargetFqnsLocally(receiver, mutableSetOf(), allowIndexAccess = false)
    }

    fun resolveTargetFqnForArrayLiteral(arrayCreation: ArrayCreationExpression): String? {
        return resolveTargetFqnsForArrayLiteral(arrayCreation).firstOrNull()
    }

    fun resolveTargetFqnsForArrayLiteral(arrayCreation: ArrayCreationExpression): Set<String> {
        val targets = linkedSetOf<String>()
        targets += resolveFromAssignment(arrayCreation)
        targets += resolveFromReturn(arrayCreation)
        targets += resolveFromArgument(arrayCreation)
        return targets
    }

    private fun resolveFromAssignment(arrayCreation: ArrayCreationExpression): Set<String> {
        val assignment = arrayCreation.parent as? AssignmentExpression ?: return emptySet()
        if (assignment.value !== arrayCreation) return emptySet()

        val target = assignment.variable as? PhpExpression ?: return emptySet()
        return resolveTargetFqnsLocally(target)
    }

    private fun resolveFromReturn(arrayCreation: ArrayCreationExpression): Set<String> {
        val phpReturn = arrayCreation.parent as? PhpReturn ?: return emptySet()
        if (phpReturn.argument !== arrayCreation) return emptySet()

        val function = PsiTreeUtil.getParentOfType(
            phpReturn,
            Function::class.java,
            false,
        ) ?: return emptySet()

        return extractReturnTargetFqns(function, allowIndexAccess = true)
    }

    private fun resolveFromArgument(arrayCreation: ArrayCreationExpression): Set<String> {
        val paramList = arrayCreation.parent as? ParameterList ?: return emptySet()
        val parameters = paramList.parameters
        val argIndex = parameters.indexOfFirst { it === arrayCreation }
        if (argIndex < 0) return emptySet()

        val call = paramList.parent ?: return emptySet()

        return when (call) {
            is FunctionReference -> resolveCallArgumentTargets(call, argIndex)
            is MethodReference -> resolveMethodArgumentTargets(call, argIndex)
            else -> emptySet()
        }
    }

    private fun resolveCallArgumentTargets(call: FunctionReference, argIndex: Int): Set<String> {
        val resolvedFunctions = call.multiResolve(false)
            .mapNotNull { it.element as? Function }
        if (resolvedFunctions.isNotEmpty()) {
            return resolvedFunctions
                .asSequence()
                .flatMap { extractParamTargetFqns(it, argIndex, allowIndexAccess = true).asSequence() }
                .toCollection(linkedSetOf())
        }

        val functionName = call.name ?: return emptySet()
        val scope = findLocalScope(call) ?: return emptySet()

        return PsiTreeUtil.collectElementsOfType(scope, Function::class.java)
            .asSequence()
            .filter { it.name == functionName }
            .sortedByDescending { it.textOffset }
            .flatMap { extractParamTargetFqns(it, argIndex, allowIndexAccess = true).asSequence() }
            .toCollection(linkedSetOf())
    }

    private fun resolveMethodArgumentTargets(call: MethodReference, argIndex: Int): Set<String> {
        return call.multiResolve(false)
            .asSequence()
            .mapNotNull(ResolveResult::getElement)
            .mapNotNull { it as? Method }
            .flatMap { extractParamTargetFqns(it, argIndex, allowIndexAccess = true).asSequence() }
            .toCollection(linkedSetOf())
    }

    private fun extractReturnTargetFqns(function: Function, allowIndexAccess: Boolean): Set<String> {
        val contextClass = contextClassOf(function)
        val targets = linkedSetOf<String>()

        targets += extractTargetFqnsFromTypedElement(function, function.project, allowIndexAccess, contextClass)
        targets += extractTargetsFromReturnDocs(function, contextClass)

        return targets
    }

    private fun extractParamTargetFqns(
        function: Function,
        argIndex: Int,
        allowIndexAccess: Boolean
    ): Set<String> {
        val parameter = function.parameters.getOrNull(argIndex) ?: return emptySet()
        val contextClass = contextClassOf(function)
        val targets = linkedSetOf<String>()

        targets += extractTargetFqnsFromTypedElement(parameter, function.project, allowIndexAccess, contextClass)
        targets += extractTargetsFromParamDocs(function, argIndex, contextClass)

        return targets
    }

    private fun resolveTargetFqnsLocally(
        expression: PhpExpression?,
        visited: MutableSet<PsiElement>,
        allowIndexAccess: Boolean
    ): Set<String> {
        if (expression == null) return emptySet()
        if (!visited.add(expression)) return emptySet()

        val directTargets = extractFromExpression(expression, visited, allowIndexAccess)
        if (directTargets.isNotEmpty()) return directTargets

        if (expression is Variable) {
            val variableDocTargets = extractFromVariableDoc(expression)
            if (variableDocTargets.isNotEmpty()) {
                return variableDocTargets
            }

            val assignment = findLatestAssignment(expression) ?: return emptySet()

            val assignmentDocTargets = extractFromAssignmentDoc(assignment, expression)
            if (assignmentDocTargets.isNotEmpty()) {
                return assignmentDocTargets
            }

            val assignedValue = assignment.value as? PhpExpression ?: return emptySet()

            val assignedValueTargets = extractFromExpression(assignedValue, visited, allowIndexAccess)
            if (assignedValueTargets.isNotEmpty()) {
                return assignedValueTargets
            }

            return resolveTargetFqnsLocally(assignedValue, visited, allowIndexAccess)
        }

        return emptySet()
    }

    private fun extractFromExpression(
        expression: PhpExpression?,
        visited: MutableSet<PsiElement>,
        allowIndexAccess: Boolean
    ): Set<String> {
        if (expression == null) return emptySet()

        return when (expression) {
            is ParenthesizedExpression -> {
                val nested = expression.argument as? PhpExpression
                resolveTargetFqnsLocally(nested, visited, allowIndexAccess)
            }

            is FunctionReference -> {
                expression.multiResolve(false)
                    .asSequence()
                    .mapNotNull(ResolveResult::getElement)
                    .mapNotNull { it as? Function }
                    .flatMap { extractReturnTargetFqns(it, allowIndexAccess).asSequence() }
                    .toCollection(linkedSetOf())
            }

            is MethodReference -> {
                expression.multiResolve(false)
                    .asSequence()
                    .mapNotNull(ResolveResult::getElement)
                    .mapNotNull { it as? Method }
                    .flatMap { extractReturnTargetFqns(it, allowIndexAccess).asSequence() }
                    .toCollection(linkedSetOf())
            }

            is FieldReference -> {
                val field = expression.resolve() as? Field ?: return emptySet()
                extractTargetFqnsFromTypedElement(
                    field,
                    expression.project,
                    allowIndexAccess,
                    contextClassOf(field)
                )
            }

            else -> extractTargetFqnsFromType(
                expression.type,
                expression.project,
                allowIndexAccess,
                contextClassOf(expression)
            )
        }
    }

    private fun extractTargetFqnsFromTypedElement(
        element: com.jetbrains.php.lang.psi.elements.PhpTypedElement,
        project: com.intellij.openapi.project.Project,
        allowIndexAccess: Boolean,
        contextClass: PhpClass?
    ): Set<String> {
        val targets = linkedSetOf<String>()
        targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(element.type), contextClass)
        targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(element.declaredType), contextClass)
        targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(element.docType), contextClass)

        if (targets.isNotEmpty() || !allowIndexAccess) {
            return targets
        }

        val globalType = PropertiesDumbModeGuards.globalTypeOrNull(element.type, project) ?: return emptySet()
        return normalizeTargets(PropertiesTypeInspector.extractTargetFqns(globalType), contextClass)
    }

    private fun extractTargetFqnsFromType(
        type: PhpType,
        project: com.intellij.openapi.project.Project,
        allowIndexAccess: Boolean,
        contextClass: PhpClass?
    ): Set<String> {
        val targets = normalizeTargets(PropertiesTypeInspector.extractTargetFqns(type), contextClass)
        if (targets.isNotEmpty() || !allowIndexAccess) {
            return targets
        }

        val globalType = PropertiesDumbModeGuards.globalTypeOrNull(type, project) ?: return emptySet()
        return normalizeTargets(PropertiesTypeInspector.extractTargetFqns(globalType), contextClass)
    }

    private fun extractTargetsFromReturnDocs(function: Function, contextClass: PhpClass?): Set<String> {
        val targets = linkedSetOf<String>()

        for (candidate in collectFunctionHierarchy(function)) {
            val candidateContextClass = contextClassOf(candidate) ?: contextClass
            for (doc in directDocs(candidate)) {
                val returnTag = PropertiesPhpDocUtils.findReturnTag(doc)
                if (returnTag != null) {
                    targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(returnTag.declaredType), candidateContextClass)
                    targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(returnTag.type), candidateContextClass)
                }
            }
        }

        return targets
    }

    private fun extractTargetsFromParamDocs(
        function: Function,
        argIndex: Int,
        contextClass: PhpClass?
    ): Set<String> {
        val targets = linkedSetOf<String>()

        for (candidate in collectFunctionHierarchy(function)) {
            val parameter = candidate.parameters.getOrNull(argIndex) ?: continue
            val parameterName = parameter.name
            val candidateContextClass = contextClassOf(candidate) ?: contextClass

            for (doc in directDocs(candidate)) {
                val paramTags = PsiTreeUtil.findChildrenOfType(doc, PhpDocTag::class.java)
                    .filterIsInstance<PhpDocParamTag>()

                for (tag in paramTags) {
                    if (!tag.text.contains("\$$parameterName")) continue
                    targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(tag.declaredType), candidateContextClass)
                    targets += normalizeTargets(PropertiesTypeInspector.extractTargetFqns(tag.type), candidateContextClass)
                }
            }
        }

        return targets
    }

    private fun extractFromVariableDoc(variable: Variable): Set<String> {
        val doc = previousDocComment(variable) ?: return emptySet()
        return extractTargetFqnsFromVarDoc(doc, variable.name, contextClassOf(variable))
    }

    private fun extractFromAssignmentDoc(
        assignment: AssignmentExpression,
        variable: Variable
    ): Set<String> {
        val doc = previousDocComment(assignment) ?: return emptySet()
        return extractTargetFqnsFromVarDoc(doc, variable.name, contextClassOf(variable))
    }

    private fun extractTargetFqnsFromVarDoc(
        doc: PsiComment,
        variableName: String,
        contextClass: PhpClass?
    ): Set<String> {
        val phpDoc = doc as? PhpDocComment
        if (phpDoc != null) {
            val tagTargets = PsiTreeUtil.findChildrenOfType(phpDoc, PhpDocTag::class.java)
                .asSequence()
                .filter { it.text.contains("@var") }
                .filter { it.text.contains("\$$variableName") || !it.text.contains("$") }
                .flatMap { tag ->
                    sequenceOf(tag.type, tag.declaredType).flatMap { type ->
                        normalizeTargets(PropertiesTypeInspector.extractTargetFqns(type), contextClass).asSequence()
                    }
                }
                .toCollection(linkedSetOf())

            if (tagTargets.isNotEmpty()) {
                return tagTargets
            }
        }

        return extractTargetFqnsFromDocText(doc.text, variableName, contextClass)
    }

    private fun extractTargetFqnsFromDocText(
        docText: String,
        variableName: String,
        contextClass: PhpClass?
    ): Set<String> {
        val normalizedVar = "\\$$variableName"

        val exactVarPattern = Regex("""@var\s+([^\s*]+)\s+$normalizedVar\b""")
        val exactTargets = exactVarPattern.find(docText)?.groupValues?.getOrNull(1)
            ?.let { normalizeTargets(PropertiesTypeInspector.extractTargetFqns(it), contextClass) }
            .orEmpty()
        if (exactTargets.isNotEmpty()) {
            return exactTargets
        }

        val loosePattern = Regex("""@var\s+([^\s*]+)""")
        return loosePattern.find(docText)?.groupValues?.getOrNull(1)
            ?.let { normalizeTargets(PropertiesTypeInspector.extractTargetFqns(it), contextClass) }
            .orEmpty()
    }

    private fun normalizeTargets(rawTargets: Set<String>, contextClass: PhpClass?): Set<String> {
        val targets = linkedSetOf<String>()

        for (rawTarget in rawTargets) {
            when (rawTarget.lowercase()) {
                "self", "static" -> {
                    contextClass?.fqn?.takeIf { it.isNotBlank() }?.let(targets::add)
                }

                "parent" -> {
                    contextClass?.superClasses
                        ?.asSequence()
                        ?.map(PhpClass::getFQN)
                        ?.filter { it.isNotBlank() }
                        ?.forEach(targets::add)
                }

                else -> targets += rawTarget
            }
        }

        return targets
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
            Function::class.java,
            Method::class.java,
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

    private fun directDocs(function: Function): List<PhpDocComment> {
        val docs = linkedSetOf<PhpDocComment>()
        function.docComment?.let(docs::add)
        PropertiesPhpDocUtils.previousPhpDoc(function)?.let(docs::add)
        return docs.toList()
    }

    private fun collectFunctionHierarchy(function: Function): Set<Function> {
        val functions = linkedSetOf<Function>()
        functions += function

        val method = function as? Method ?: return functions
        PhpClassHierarchyUtils.processSuperMethods(method) { superMethod, _, _ ->
            functions += superMethod
            true
        }
        return functions
    }

    private fun contextClassOf(element: PsiElement): PhpClass? {
        return when (element) {
            is Method -> element.containingClass
            is Field -> element.containingClass
            else -> PsiTreeUtil.getParentOfType(element, PhpClass::class.java, false)
        }
    }
}
