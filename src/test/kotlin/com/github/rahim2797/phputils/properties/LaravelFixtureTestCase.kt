package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeKeyReference
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

abstract class LaravelFixtureTestCase : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        loadLaravelFixtureProject()
        DumbService.getInstance(project).waitForSmartMode()
    }

    protected fun openFixtureFile(relativePath: String): PsiFile {
        val normalized = relativePath.replace('\\', '/')
        val virtualFile = myFixture.findFileInTempDir(normalized)
        assertNotNull("Fixture file not found: $normalized", virtualFile)
        myFixture.configureFromExistingVirtualFile(virtualFile!!)
        return myFixture.file
    }

    protected fun keyReferenceAtCaret(): MagicTypeKeyReference {
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset) as? MagicTypeKeyReference
        assertNotNull("Expected a MagicTypeKeyReference at the caret", reference)
        return reference!!
    }

    protected fun firstArrayAccess(): ArrayAccessExpression {
        val access = PsiTreeUtil.findChildOfType(myFixture.file, ArrayAccessExpression::class.java)
        assertNotNull("Expected an array access expression", access)
        return access!!
    }

    private fun loadLaravelFixtureProject() {
        Files.walk(fixtureRoot()).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .sorted()
                .collect(Collectors.toList())
                .forEach { file ->
                    val relativePath = fixtureRoot().relativize(file).toString().replace('\\', '/')
                    if (relativePath.endsWith(".sqlite")) {
                        return@forEach
                    }

                    myFixture.addFileToProject(relativePath, Files.readString(file, StandardCharsets.UTF_8))
                }
        }
    }

    private fun fixtureRoot(): Path {
        return Path.of(
            System.getProperty("user.dir"),
            "src",
            "test",
            "resources",
            "fixtures",
            "laravel-app"
        )
    }
}
