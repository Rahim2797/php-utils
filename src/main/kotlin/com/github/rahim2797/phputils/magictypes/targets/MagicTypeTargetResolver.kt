package com.github.rahim2797.phputils.magictypes.targets

import com.github.rahim2797.phputils.magictypes.MagicTypeMatch
import com.github.rahim2797.phputils.magictypes.parser.MagicTypeParser
import com.github.rahim2797.phputils.properties.PropertiesDumbModeGuards
import com.github.rahim2797.phputils.properties.PropertiesPhpDocUtils
import com.github.rahim2797.phputils.properties.PsiGuards
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpClassHierarchyUtils
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object MagicTypeTargetResolver {
    fun resolveArrayAccessMatches(receiver: PhpExpression): List<MagicTypeMatch> {
        val localMatches =
            if (PropertiesDumbModeGuards.isDumb(receiver.project)) {
                resolveLocally(receiver, mutableSetOf(), allowIndexAccess = false)
            } else {
                resolveLocally(receiver, mutableSetOf(), allowIndexAccess = true)
            }
        if (localMatches.isNotEmpty()) return localMatches

        val receiverType = PropertiesDumbModeGuards.safeExpressionType(
            receiver,
            "array access receiver type lookup"
        ) ?: return emptyList()
        val globalType = PropertiesDumbModeGuards.globalTypeOrNull(receiverType, receiver.project) ?: return emptyList()
        return MagicTypeParser.extractMatches(globalType, receiver)
    }

    fun resolveLiteralMatches(literal: StringLiteralExpression): List<MagicTypeMatch> {
        PsiGuards.getArrayAccessReceiver(literal)?.let { receiver ->
            val accessMatches = resolveArrayAccessMatches(receiver)
            if (accessMatches.isNotEmpty()) {
                return accessMatches
            }
        }

        val arrayCreation = PsiGuards.getOwningArrayCreation(literal) ?: return emptyList()
        return resolveArrayLiteralMatches(arrayCreation)
    }

    fun resolveArrayLiteralMatches(arrayCreation: ArrayCreationExpression): List<MagicTypeMatch> {
        val matches = mutableListOf<MagicTypeMatch>()
        matches += resolveFromAssignment(arrayCreation)
        matches += resolveFromReturn(arrayCreation)
        matches += resolveFromArgument(arrayCreation)
        return MagicTypeMatch.merge(matches)
    }

    fun resolveDocMatches(element: PsiElement?, originalElement: PsiElement?): List<MagicTypeMatch> {
        val target = element ?: originalElement ?: return emptyList()
        var current: PsiElement? = target
        repeat(8) {
            if (current == null) return emptyList()
            val phpDocType = current as? PhpDocType
            if (phpDocType != null) {
                val matches = MagicTypeParser.extractMatches(phpDocType.text, phpDocType)
                if (matches.isNotEmpty()) {
                    return matches
                }
            }
            current = current.parent
        }
        return emptyList()
    }

    private fun resolveFromAssignment(arrayCreation: ArrayCreationExpression): List<MagicTypeMatch> {
        val contextualExpression = PsiGuards.getContextualizedArrayExpression(arrayCreation)
        val assignment = contextualExpression.parent as? AssignmentExpression ?: return emptyList()
        if (assignment.value !== contextualExpression) return emptyList()
        val target = assignment.variable as? PhpExpression ?: return emptyList()
        return resolveLocally(target, mutableSetOf(), allowIndexAccess = true)
    }

    private fun resolveFromReturn(arrayCreation: ArrayCreationExpression): List<MagicTypeMatch> {
        val contextualExpression = PsiGuards.getContextualizedArrayExpression(arrayCreation)
        val phpReturn = contextualExpression.parent as? PhpReturn ?: return emptyList()
        if (phpReturn.argument !== contextualExpression) return emptyList()
        val function = PsiTreeUtil.getParentOfType(phpReturn, Function::class.java, false) ?: return emptyList()
        return extractReturnMatches(function, allowIndexAccess = true)
    }

    private fun resolveFromArgument(arrayCreation: ArrayCreationExpression): List<MagicTypeMatch> {
        val contextualExpression = PsiGuards.getContextualizedArrayExpression(arrayCreation)
        val paramList = contextualExpression.parent as? ParameterList ?: return emptyList()
        val parameters = paramList.parameters
        val argIndex = parameters.indexOfFirst { it === contextualExpression }
        if (argIndex < 0) return emptyList()
        val call = paramList.parent ?: return emptyList()

        return when (call) {
            is FunctionReference -> resolveCallArgumentMatches(call, argIndex)
            is MethodReference -> resolveMethodArgumentMatches(call, argIndex)
            else -> emptyList()
        }
    }

    private fun resolveCallArgumentMatches(call: FunctionReference, argIndex: Int): List<MagicTypeMatch> {
        val resolvedFunctions = PropertiesDumbModeGuards.safeMultiResolve(
            call.project,
            "function argument resolution"
        ) { call.multiResolve(false) }
            .mapNotNull { it.element as? Function }
        if (resolvedFunctions.isNotEmpty()) {
            return MagicTypeMatch.merge(
                resolvedFunctions.flatMap { extractParamMatches(it, argIndex, allowIndexAccess = true) }
            )
        }

        val functionName = call.name ?: return emptyList()
        val scope = findLocalScope(call) ?: return emptyList()
        return MagicTypeMatch.merge(
            PsiTreeUtil.collectElementsOfType(scope, Function::class.java)
                .asSequence()
                .filter { it.name == functionName }
                .sortedByDescending { it.textOffset }
                .flatMap { extractParamMatches(it, argIndex, allowIndexAccess = true).asSequence() }
                .toList()
        )
    }

    private fun resolveMethodArgumentMatches(call: MethodReference, argIndex: Int): List<MagicTypeMatch> {
        return MagicTypeMatch.merge(
            PropertiesDumbModeGuards.safeMultiResolve(call.project, "method argument resolution") { call.multiResolve(false) }
                .asSequence()
                .mapNotNull(ResolveResult::getElement)
                .mapNotNull { it as? Method }
                .flatMap { extractParamMatches(it, argIndex, allowIndexAccess = true).asSequence() }
                .toList()
        )
    }

    private fun extractReturnMatches(function: Function, allowIndexAccess: Boolean): List<MagicTypeMatch> {
        val contextClass = contextClassOf(function)
        return MagicTypeMatch.merge(
            extractMatchesFromTypedElement(function, function.project, allowIndexAccess, contextClass) +
                extractMatchesFromReturnDocs(function, contextClass)
        )
    }

    private fun extractParamMatches(function: Function, argIndex: Int, allowIndexAccess: Boolean): List<MagicTypeMatch> {
        val parameter = function.parameters.getOrNull(argIndex) ?: return emptyList()
        val contextClass = contextClassOf(function)
        return MagicTypeMatch.merge(
            extractMatchesFromTypedElement(parameter, function.project, allowIndexAccess, contextClass) +
                extractMatchesFromParamDocs(function, argIndex, contextClass)
        )
    }

    private fun resolveLocally(
        expression: PhpExpression?,
        visited: MutableSet<PsiElement>,
        allowIndexAccess: Boolean
    ): List<MagicTypeMatch> {
        if (expression == null) return emptyList()
        if (!visited.add(expression)) return emptyList()

        val directMatches = extractMatchesFromExpression(expression, visited, allowIndexAccess)
        if (directMatches.isNotEmpty()) return directMatches

        if (expression is Variable) {
            val variableDocMatches = extractMatchesFromVariableDoc(expression)
            if (variableDocMatches.isNotEmpty()) return variableDocMatches

            val assignment = findLatestAssignment(expression) ?: return emptyList()
            val assignmentDocMatches = extractMatchesFromAssignmentDoc(assignment, expression)
            if (assignmentDocMatches.isNotEmpty()) return assignmentDocMatches

            val assignedValue = assignment.value as? PhpExpression ?: return emptyList()
            val assignedMatches = extractMatchesFromExpression(assignedValue, visited, allowIndexAccess)
            if (assignedMatches.isNotEmpty()) return assignedMatches

            return resolveLocally(assignedValue, visited, allowIndexAccess)
        }

        return emptyList()
    }

    private fun extractMatchesFromExpression(
        expression: PhpExpression?,
        visited: MutableSet<PsiElement>,
        allowIndexAccess: Boolean
    ): List<MagicTypeMatch> {
        if (expression == null) return emptyList()

        return when (expression) {
            is ParenthesizedExpression -> resolveLocally(expression.argument as? PhpExpression, visited, allowIndexAccess)
            is FunctionReference -> MagicTypeMatch.merge(
                PropertiesDumbModeGuards.safeMultiResolve(
                    expression.project,
                    "function return resolution"
                ) { expression.multiResolve(false) }
                    .asSequence()
                    .mapNotNull(ResolveResult::getElement)
                    .mapNotNull { it as? Function }
                    .flatMap { extractReturnMatches(it, allowIndexAccess).asSequence() }
                    .toList()
            )
            is MethodReference -> MagicTypeMatch.merge(
                PropertiesDumbModeGuards.safeMultiResolve(
                    expression.project,
                    "method return resolution"
                ) { expression.multiResolve(false) }
                    .asSequence()
                    .mapNotNull(ResolveResult::getElement)
                    .mapNotNull { it as? Method }
                    .flatMap { extractReturnMatches(it, allowIndexAccess).asSequence() }
                    .toList()
            )
            is FieldReference -> {
                val field = PropertiesDumbModeGuards.safeResolve(
                    expression.project,
                    "field resolution"
                ) { expression.resolve() } as? Field ?: return emptyList()
                extractMatchesFromTypedElement(field, expression.project, allowIndexAccess, contextClassOf(field))
            }
            else -> {
                val expressionType = PropertiesDumbModeGuards.safeExpressionType(
                    expression,
                    "expression type lookup"
                ) ?: return emptyList()
                extractMatchesFromType(expressionType, expression.project, allowIndexAccess, contextClassOf(expression), expression)
            }
        }
    }

    private fun extractMatchesFromTypedElement(
        element: PhpTypedElement,
        project: com.intellij.openapi.project.Project,
        allowIndexAccess: Boolean,
        contextClass: PhpClass?
    ): List<MagicTypeMatch> {
        val elementTypes = PropertiesDumbModeGuards.safeTypedElementTypes(
            element,
            "typed element type lookup"
        ) ?: return emptyList()
        val matches = MagicTypeMatch.merge(
            MagicTypeParser.extractMatches(elementTypes.localType, element) +
                MagicTypeParser.extractMatches(elementTypes.declaredType, element) +
                MagicTypeParser.extractMatches(elementTypes.docType, element)
        )
        if (matches.isNotEmpty() || !allowIndexAccess) return normalizeMatches(matches, contextClass)

        val globalType = PropertiesDumbModeGuards.globalTypeOrNull(elementTypes.localType, project) ?: return emptyList()
        return normalizeMatches(MagicTypeParser.extractMatches(globalType, element), contextClass)
    }

    private fun extractMatchesFromType(
        type: PhpType,
        project: com.intellij.openapi.project.Project,
        allowIndexAccess: Boolean,
        contextClass: PhpClass?,
        contextElement: PsiElement?
    ): List<MagicTypeMatch> {
        val matches = normalizeMatches(MagicTypeParser.extractMatches(type, contextElement), contextClass)
        if (matches.isNotEmpty() || !allowIndexAccess) return matches

        val globalType = PropertiesDumbModeGuards.globalTypeOrNull(type, project) ?: return emptyList()
        return normalizeMatches(MagicTypeParser.extractMatches(globalType, contextElement), contextClass)
    }

    private fun extractMatchesFromReturnDocs(function: Function, contextClass: PhpClass?): List<MagicTypeMatch> {
        val matches = mutableListOf<MagicTypeMatch>()
        for (candidate in collectFunctionHierarchy(function)) {
            val candidateContextClass = contextClassOf(candidate) ?: contextClass
            for (doc in directDocs(candidate)) {
                PropertiesPhpDocUtils.findReturnTag(doc)?.let { returnTag ->
                    matches += normalizeMatches(MagicTypeParser.extractMatches(returnTag.declaredType, returnTag), candidateContextClass)
                    matches += normalizeMatches(MagicTypeParser.extractMatches(returnTag.type, returnTag), candidateContextClass)
                }
            }
        }
        return MagicTypeMatch.merge(matches)
    }

    private fun extractMatchesFromParamDocs(function: Function, argIndex: Int, contextClass: PhpClass?): List<MagicTypeMatch> {
        val matches = mutableListOf<MagicTypeMatch>()
        for (candidate in collectFunctionHierarchy(function)) {
            val parameter = candidate.parameters.getOrNull(argIndex) ?: continue
            val parameterName = parameter.name
            val candidateContextClass = contextClassOf(candidate) ?: contextClass

            for (doc in directDocs(candidate)) {
                val paramTags = PsiTreeUtil.findChildrenOfType(doc, PhpDocTag::class.java).filterIsInstance<PhpDocParamTag>()
                for (tag in paramTags) {
                    if (!tag.text.contains("\$$parameterName")) continue
                    matches += normalizeMatches(MagicTypeParser.extractMatches(tag.declaredType, tag), candidateContextClass)
                    matches += normalizeMatches(MagicTypeParser.extractMatches(tag.type, tag), candidateContextClass)
                }
            }
        }
        return MagicTypeMatch.merge(matches)
    }

    private fun extractMatchesFromVariableDoc(variable: Variable): List<MagicTypeMatch> {
        val doc = previousDocComment(variable) ?: return emptyList()
        return extractMatchesFromVarDoc(doc, variable.name, contextClassOf(variable))
    }

    private fun extractMatchesFromAssignmentDoc(assignment: AssignmentExpression, variable: Variable): List<MagicTypeMatch> {
        val doc = previousDocComment(assignment) ?: return emptyList()
        return extractMatchesFromVarDoc(doc, variable.name, contextClassOf(variable))
    }

    private fun extractMatchesFromVarDoc(doc: PsiComment, variableName: String, contextClass: PhpClass?): List<MagicTypeMatch> {
        val phpDoc = doc as? PhpDocComment
        if (phpDoc != null) {
            val matches = PsiTreeUtil.findChildrenOfType(phpDoc, PhpDocTag::class.java)
                .asSequence()
                .filter { it.text.contains("@var") }
                .filter { it.text.contains("\$$variableName") || !it.text.contains("$") }
                .flatMap { tag ->
                    sequenceOf(tag.type, tag.declaredType).flatMap { type ->
                        normalizeMatches(MagicTypeParser.extractMatches(type, tag), contextClass).asSequence()
                    }
                }
                .toList()
            if (matches.isNotEmpty()) return MagicTypeMatch.merge(matches)
        }

        return extractMatchesFromDocText(doc.text, doc, contextClass)
    }

    private fun extractMatchesFromDocText(docText: String, contextClass: PhpClass?): List<MagicTypeMatch> {
        return extractMatchesFromDocText(docText, contextClass, contextClass)
    }

    private fun extractMatchesFromDocText(
        docText: String,
        contextElement: PsiElement?,
        contextClass: PhpClass?
    ): List<MagicTypeMatch> {
        val varPattern = Regex("""@var\s+([^\s*]+)""")
        val rawType = varPattern.find(docText)?.groupValues?.getOrNull(1) ?: return emptyList()
        return normalizeMatches(MagicTypeParser.extractMatches(rawType, contextElement), contextClass)
    }

    private fun normalizeMatches(matches: List<MagicTypeMatch>, contextClass: PhpClass?): List<MagicTypeMatch> {
        return MagicTypeMatch.merge(
            matches.map { match ->
                MagicTypeMatch(match.handler, normalizeTargets(match.targetFqns, contextClass))
            }
        )
    }

    private fun normalizeTargets(rawTargets: Set<String>, contextClass: PhpClass?): Set<String> {
        val targets = linkedSetOf<String>()
        for (rawTarget in rawTargets) {
            when (rawTarget.lowercase()) {
                "self", "static" -> contextClass?.fqn?.takeIf { it.isNotBlank() }?.let(targets::add)
                "parent" -> contextClass?.superClasses?.asSequence()?.map(PhpClass::getFQN)?.filter { it.isNotBlank() }?.forEach(targets::add)
                else -> targets += rawTarget
            }
        }
        return targets
    }

    private fun findLatestAssignment(variable: Variable): AssignmentExpression? {
        val variableName = variable.name
        val scope = findLocalScope(variable) ?: return null

        return PsiTreeUtil.collectElementsOfType(scope, AssignmentExpression::class.java)
            .asSequence()
            .filter { it.textOffset < variable.textOffset }
            .filter { (it.variable as? Variable)?.name == variableName }
            .maxByOrNull { it.textOffset }
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
