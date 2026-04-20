package com.github.rahim2797.phputils.properties

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PropertiesStubLibraryRootsProviderTest {
    private val classLoader = PropertiesStubLibraryRootsProvider::class.java.classLoader

    @Test
    fun testEnsureStubRootOnDiskIsIdempotentForExistingStub() {
        withTempDirectory { tempDir ->
            val firstRoot = PropertiesStubSupport.ensureStubRootOnDisk(tempDir, classLoader)
            val secondRoot = PropertiesStubSupport.ensureStubRootOnDisk(tempDir, classLoader)

            val targetFile = stubFile(tempDir)
            assertEquals(tempDir, firstRoot)
            assertEquals(tempDir, secondRoot)
            assertTrue(Files.exists(targetFile))
            assertEquals(readBundledStub(), Files.readString(targetFile, StandardCharsets.UTF_8))
        }
    }

    @Test
    fun testEnsureStubRootOnDiskTreatsPreExistingStubAsValid() {
        withTempDirectory { tempDir ->
            val targetFile = stubFile(tempDir)
            Files.createDirectories(targetFile.parent)
            Files.writeString(targetFile, "preexisting", StandardCharsets.UTF_8)

            val root = PropertiesStubSupport.ensureStubRootOnDisk(tempDir, classLoader)

            assertEquals(tempDir, root)
            assertEquals("preexisting", Files.readString(targetFile, StandardCharsets.UTF_8))
        }
    }

    @Test
    fun testEnsureStubRootOnDiskHandlesConcurrentCreation() {
        withTempDirectory { tempDir ->
            val executor = Executors.newFixedThreadPool(2)
            val barrier = CyclicBarrier(2)

            try {
                val futures = List(2) {
                    executor.submit(Callable {
                        barrier.await(5, TimeUnit.SECONDS)
                        PropertiesStubSupport.ensureStubRootOnDisk(tempDir, classLoader)
                    })
                }

                val roots = futures.map { it.get(5, TimeUnit.SECONDS) }
                val targetFile = stubFile(tempDir)

                roots.forEach { assertEquals(tempDir, it) }
                assertTrue(Files.exists(targetFile))
                assertTrue(Files.isReadable(targetFile))
                assertEquals(readBundledStub(), Files.readString(targetFile, StandardCharsets.UTF_8))
            } finally {
                executor.shutdownNow()
            }
        }
    }

    private fun stubFile(root: Path): Path {
        return root.resolve("Rahim2797").resolve("MagicTypes").resolve("Properties.php")
    }

    private fun readBundledStub(): String {
        val input = classLoader.getResourceAsStream("stubs/Rahim2797/MagicTypes/Properties.php")
        assertNotNull(input)
        return input!!.use { stream -> String(stream.readAllBytes(), StandardCharsets.UTF_8) }
    }

    private fun withTempDirectory(block: (Path) -> Unit) {
        val tempDir = Files.createTempDirectory("properties-stub-provider-test")
        try {
            block(tempDir)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}
