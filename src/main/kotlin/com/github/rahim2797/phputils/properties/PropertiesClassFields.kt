package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocPropertyTag
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass

object PropertiesClassFields {
    private const val IDE_HELPER_MODELS_FILE = "_ide_helper_models.php"
    private const val LARAVEL_MODEL_FQN = "Illuminate\\Database\\Eloquent\\Model"

    fun getFields(project: Project, targetFqn: String): List<Field> {
        val phpIndex = PhpIndex.getInstance(project)
        val classes = findClassesByFqn(project, phpIndex, targetFqn)
        if (classes.isEmpty()) return emptyList()

        val helperAttributes = classes
            .asSequence()
            .flatMap { getHelperModelProperties(it).asSequence() }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name }
            .toList()

        if (helperAttributes.isNotEmpty()) {
            return helperAttributes
        }

        return classes
            .asSequence()
            .flatMap { phpClass ->
                val fields = if (isLaravelModelDescendant(phpClass)) {
                    phpClass.ownFields.asSequence()
                } else {
                    phpClass.fields.asSequence()
                }
                fields
            }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name }
            .toList()
    }

    fun findField(project: Project, targetFqn: String, fieldName: String): Field? {
        return getFields(project, targetFqn).firstOrNull { it.name == fieldName }
    }

    private fun findClassesByFqn(
        project: Project,
        phpIndex: PhpIndex,
        targetFqn: String
    ): Collection<PhpClass> {
        val candidates = linkedSetOf(targetFqn, targetFqn.removePrefix("\\"))

        for (candidate in candidates) {
            if (candidate.isBlank()) continue

            val classes = phpIndex.getClassesByFQN(candidate)
            if (classes.isNotEmpty()) {
                return classes
            }
        }

        return findClassesByPsiFallback(project, candidates)
    }

    private fun findClassesByPsiFallback(
        project: Project,
        candidates: Set<String>
    ): Collection<PhpClass> {
        val normalizedFqns = candidates
            .asSequence()
            .filter { it.isNotBlank() }
            .flatMap { candidate ->
                sequenceOf(candidate, candidate.removePrefix("\\"))
            }
            .toSet()
        if (normalizedFqns.isEmpty()) return emptyList()

        val shortNames = normalizedFqns
            .map { it.substringAfterLast('\\') }
            .filter { it.isNotBlank() }
            .toSet()
        val psiManager = PsiManager.getInstance(project)
        val matches = linkedSetOf<PhpClass>()

        for (root in ProjectRootManager.getInstance(project).contentRoots) {
            collectMatchingClasses(root, shortNames, normalizedFqns, psiManager, matches)
        }

        return matches
    }

    private fun collectMatchingClasses(
        file: VirtualFile,
        shortNames: Set<String>,
        normalizedFqns: Set<String>,
        psiManager: PsiManager,
        matches: MutableSet<PhpClass>
    ) {
        if (file.isDirectory) {
            file.children.forEach { child ->
                collectMatchingClasses(child, shortNames, normalizedFqns, psiManager, matches)
            }
            return
        }

        if (!file.name.endsWith(".php", ignoreCase = true)) return
        if (shortNames.isNotEmpty() && file.nameWithoutExtension !in shortNames) return

        val psiFile = psiManager.findFile(file) ?: return
        val phpClasses = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)

        for (phpClass in phpClasses) {
            val fqn = phpClass.fqn
            if (fqn.isBlank()) continue

            if (fqn in normalizedFqns || fqn.removePrefix("\\") in normalizedFqns) {
                matches += phpClass
            }
        }
    }

    private fun getHelperModelProperties(phpClass: PhpClass): List<PhpDocProperty> {
        val docComment = phpClass.docComment ?: return emptyList()
        if (docComment.containingFile?.name != IDE_HELPER_MODELS_FILE) return emptyList()

        return docComment.propertyTags
            .asSequence()
            .mapNotNull(PhpDocPropertyTag::getProperty)
            .filter { it.containingFile?.name == IDE_HELPER_MODELS_FILE }
            .toList()
    }

    private fun isLaravelModelDescendant(phpClass: PhpClass): Boolean {
        return isLaravelModelDescendant(phpClass, mutableSetOf())
    }

    private fun isLaravelModelDescendant(
        phpClass: PhpClass,
        visited: MutableSet<String>
    ): Boolean {
        val classFqn = phpClass.fqn
        if (!visited.add(classFqn)) return false
        if (classFqn == LARAVEL_MODEL_FQN) return true

        val superClass = phpClass.superClass ?: return false
        return isLaravelModelDescendant(superClass, visited)
    }
}
