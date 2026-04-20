package com.github.rahim2797.phputils.properties

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

class PropertiesStubLibraryRootsProvider : AdditionalLibraryRootsProvider() {
    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val root = ensureStubRoot() ?: return emptyList()
        return listOf(SyntheticLibrary.newImmutableLibrary(listOf(root)))
    }

    override fun getRootsToWatch(project: Project): Collection<VirtualFile> {
        return ensureStubRoot()?.let(::listOf).orEmpty()
    }

    private fun ensureStubRoot(): VirtualFile? {
        val targetRoot = PropertiesStubSupport.ensureStubRootOnDisk(
            targetRoot = Path.of(PathManager.getSystemPath(), "php-utils", "stubs"),
            classLoader = javaClass.classLoader,
        ) ?: return null

        return PropertiesStubSupport.refreshStubRoot(targetRoot)
    }
}

internal object PropertiesStubSupport {
    private const val RESOURCE_PATH = "stubs/Rahim2797/MagicTypes/Properties.php"

    fun ensureStubRootOnDisk(targetRoot: Path, classLoader: ClassLoader): Path? {
        val targetFile = targetRoot.resolve("Rahim2797").resolve("MagicTypes").resolve("Properties.php")

        Files.createDirectories(targetFile.parent)
        if (Files.exists(targetFile)) {
            return targetRoot
        }

        val inputStream = classLoader.getResourceAsStream(RESOURCE_PATH) ?: return null
        inputStream.use { input ->
            try {
                Files.copy(input, targetFile)
            } catch (_: FileAlreadyExistsException) {
                // Another concurrent caller created the stub first; that is a valid final state.
            }
        }

        return targetRoot
    }

    fun refreshStubRoot(targetRoot: Path): VirtualFile? {
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetRoot)
    }
}
