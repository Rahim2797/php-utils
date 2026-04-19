package com.github.rahim2797.phputils.properties

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class PropertiesStubLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val root = ensureStubRoot() ?: return emptyList()
        return listOf(SyntheticLibrary.newImmutableLibrary(listOf(root)))
    }

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
        return ensureStubRoot()?.let(::listOf).orEmpty()
    }

    private fun ensureStubRoot(): VirtualFile? {
        val targetRoot = Path.of(PathManager.getSystemPath(), "php-utils", "stubs")
        val targetFile = targetRoot.resolve("Rahim2797").resolve("MagicTypes").resolve("Properties.php")
        val resourcePath = "stubs/Rahim2797/MagicTypes/Properties.php"

        val inputStream = javaClass.classLoader.getResourceAsStream(resourcePath) ?: return null
        inputStream.use { input ->
            Files.createDirectories(targetFile.parent)
            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetRoot)
    }
}
