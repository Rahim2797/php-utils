package com.github.rahim2797.phputils.properties

import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PropertiesDumbModeGuardsTest : BasePlatformTestCase() {
    fun testSafeIdeLookupSuppressesIndexNotReadyException() {
        val result = PropertiesDumbModeGuards.safeIdeLookup(project, "test lookup") {
            throw IndexNotReadyException.create()
        }

        assertNull(result)
    }

    fun testSafeIdeLookupSuppressesOutdatedStubFailure() {
        val result = PropertiesDumbModeGuards.safeIdeLookup(project, "test lookup") {
            throw IllegalStateException("Outdated stub in index: file://broken.php")
        }

        assertNull(result)
    }

    fun testSafeIdeLookupStillThrowsUnexpectedExceptions() {
        assertThrows(IllegalArgumentException::class.java) {
            PropertiesDumbModeGuards.safeIdeLookup(project, "test lookup") {
                throw IllegalArgumentException("boom")
            }
        }
    }

    fun testRecoverableFailureDetectionChecksCauseChain() {
        val exception = RuntimeException(
            "wrapper",
            IllegalStateException("stub and index do not match")
        )

        assertTrue(PropertiesDumbModeGuards.isRecoverableResolutionFailure(exception))
    }
}
