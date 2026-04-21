package com.github.rahim2797.phputils.properties

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MagicTypeTargetResolverGuardTest : BasePlatformTestCase() {
    fun testSafeResolveSuppressesOutdatedStubFailure() {
        val result = PropertiesDumbModeGuards.safeResolve(project, "field resolution") {
            throw IllegalStateException("Outdated stub in index: file://broken.php")
        }

        assertNull(result)
    }

    fun testSafeMultiResolveSuppressesOutdatedStubFailure() {
        val result = PropertiesDumbModeGuards.safeMultiResolve(project, "method resolution") {
            throw IllegalStateException("stub text inconsistency")
        }

        assertEmpty(result)
    }
}
