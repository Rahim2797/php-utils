package com.github.rahim2797.phputils.properties

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.ResolveResult
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesDumbModeGuards {
    private val log = Logger.getInstance(PropertiesDumbModeGuards::class.java)

    data class TypedElementTypes(
        val localType: PhpType,
        val declaredType: PhpType,
        val docType: PhpType,
    )

    fun isDumb(project: Project): Boolean = DumbService.isDumb(project)

    inline fun <T> runSmart(project: Project, action: () -> T): T? {
        return if (isDumb(project)) null else action()
    }

    fun globalTypeOrNull(type: PhpType, project: Project): PhpType? {
        return safeIdeLookup(project, "global PHP type expansion") { type.global(project) }
    }

    fun safeExpressionType(
        expression: PhpExpression,
        operation: String = "expression type lookup",
    ): PhpType? {
        return safeType(expression.project, operation) { expression.type }
    }

    fun safeTypedElementTypes(
        element: PhpTypedElement,
        operation: String = "typed element type lookup",
    ): TypedElementTypes? {
        return safeTypedElementTypes(element.project, operation) {
            TypedElementTypes(
                localType = element.type,
                declaredType = element.declaredType,
                docType = element.docType,
            )
        }
    }

    fun safeType(project: Project, operation: String, action: () -> PhpType): PhpType? {
        return safeIdeLookup(project, operation, action)
    }

    fun safeTypedElementTypes(
        project: Project,
        operation: String,
        action: () -> TypedElementTypes,
    ): TypedElementTypes? {
        return safeIdeLookup(project, operation, action)
    }

    fun <T> safeIdeLookup(project: Project, operation: String, action: () -> T): T? {
        if (isDumb(project)) {
            debugSkip(operation, "project is in dumb mode")
            return null
        }

        return try {
            action()
        } catch (exception: Throwable) {
            when {
                exception is ProcessCanceledException -> throw exception
                isRecoverableResolutionFailure(exception) -> {
                    log.warn("Skipping $operation because IDE indexes are unstable: ${exception.javaClass.name}: ${exception.message}")
                    null
                }
                else -> throw exception
            }
        }
    }

    fun safeMultiResolve(project: Project, operation: String, action: () -> Array<ResolveResult>): List<ResolveResult> {
        return safeIdeLookup(project, operation, action)?.toList().orEmpty()
    }

    fun <T> safeResolve(project: Project, operation: String, action: () -> T): T? {
        return safeIdeLookup(project, operation, action)
    }

    internal fun isRecoverableResolutionFailure(exception: Throwable): Boolean {
        var current: Throwable? = exception
        while (current != null) {
            if (current is IndexNotReadyException) return true

            val className = current.javaClass.name
            if (className == "com.intellij.psi.stubs.StubTextInconsistencyException") return true
            if (className == "com.intellij.psi.stubs.StubTreeLoader\$StubTreeAndIndexUnmatchCoarseException") return true
            if (className == "com.intellij.openapi.util.RecursionManager\$CachingPreventedException") return true
            if (className == "com.intellij.openapi.util.StackOverflowPreventedException") return true

            val message = current.message.orEmpty()
            if (message.contains("Outdated stub in index", ignoreCase = true)) return true
            if (message.contains("stub and index do not match", ignoreCase = true)) return true
            if (message.contains("stub text inconsistency", ignoreCase = true)) return true
            if (message.contains("Caching disabled due to recursion prevention", ignoreCase = true)) return true
            if (message.contains("cyclic dependencies", ignoreCase = true)) return true

            current = current.cause
        }

        return false
    }

    private fun debugSkip(operation: String, reason: String) {
        if (log.isDebugEnabled) {
            log.debug("Skipping $operation: $reason")
        }
    }
}
