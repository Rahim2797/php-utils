package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.MagicTypeFeatureHandler
import com.github.rahim2797.phputils.magictypes.MagicTypeShape
import com.github.rahim2797.phputils.magictypes.MagicTypeShapeKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocPropertyTag
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.resolve.types.PhpType

object PropertiesShapeBuilder {
    private const val IDE_HELPER_MODELS_FILE = "_ide_helper_models.php"
    private const val LARAVEL_MODEL_FQN = "Illuminate\\Database\\Eloquent\\Model"

    fun build(project: Project, handler: MagicTypeFeatureHandler, targetFqns: Set<String>): MagicTypeShape {
        val keys = targetFqns
            .flatMap { fields(project, it) }
            .groupBy { it.name }
            .toSortedMap()
            .map { (name, fields) ->
                val mergedType = PhpType()
                fields.forEach { mergedType.add(it.type) }
                MagicTypeShapeKey(name, fields.distinctBy(::fieldIdentity), mergedType.takeIf { it.types.isNotEmpty() })
            }

        return MagicTypeShape(handler, targetFqns, keys)
    }

    private fun fields(project: Project, targetFqn: String): List<Field> {
        val classes = findClassesByFqn(project, targetFqn)
        if (classes.isEmpty()) return emptyList()

        val helperAttributes = classes
            .asSequence()
            .flatMap { helperModelProperties(it).asSequence() }
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name }
            .toList()
        if (helperAttributes.isNotEmpty()) {
            return helperAttributes
        }

        return classes
            .asSequence()
            .flatMap { phpClass ->
                val fields = if (isLaravelModelDescendant(phpClass)) phpClass.ownFields.asSequence() else phpClass.fields.asSequence()
                fields
            }
            .filter { it.name.isNotBlank() }
            .distinctBy(::fieldIdentity)
            .toList()
    }

    private fun findClassesByFqn(project: Project, targetFqn: String): Collection<PhpClass> {
        val phpIndex = PhpIndex.getInstance(project)
        val candidates = linkedSetOf(targetFqn, targetFqn.removePrefix("\\"))
        val matches = linkedSetOf<PhpClass>()

        for (candidate in candidates) {
            if (candidate.isBlank()) continue
            matches += phpIndex.getClassesByFQN(candidate)
        }

        matches += findClassesByPsiFallback(project, candidates)
        return matches
    }

    private fun findClassesByPsiFallback(project: Project, candidates: Set<String>): Collection<PhpClass> {
        val normalizedFqns = candidates
            .asSequence()
            .filter { it.isNotBlank() }
            .flatMap { sequenceOf(it, it.removePrefix("\\")) }
            .toSet()
        if (normalizedFqns.isEmpty()) return emptyList()

        val shortNames = normalizedFqns.map { it.substringAfterLast('\\') }.filter { it.isNotBlank() }.toSet()
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
            file.children.forEach { child -> collectMatchingClasses(child, shortNames, normalizedFqns, psiManager, matches) }
            return
        }
        if (!file.name.endsWith(".php", ignoreCase = true)) return
        if (shortNames.isNotEmpty() && file.nameWithoutExtension !in shortNames && file.name != IDE_HELPER_MODELS_FILE) return

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

    private fun helperModelProperties(phpClass: PhpClass): List<PhpDocProperty> {
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

    private fun isLaravelModelDescendant(phpClass: PhpClass, visited: MutableSet<String>): Boolean {
        val classFqn = phpClass.fqn
        if (!visited.add(classFqn)) return false
        if (classFqn == LARAVEL_MODEL_FQN) return true

        val superClass = phpClass.superClass ?: return false
        return isLaravelModelDescendant(superClass, visited)
    }

    private fun fieldIdentity(field: Field): String {
        val owner = field.containingClass?.fqn ?: field.containingFile?.virtualFile?.path ?: "unknown"
        return "$owner#${field.name}"
    }
}
