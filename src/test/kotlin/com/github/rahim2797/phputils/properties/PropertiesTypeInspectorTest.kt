package com.github.rahim2797.phputils.properties

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.resolve.types.PhpType

class PropertiesTypeInspectorTest : BasePlatformTestCase() {
    fun testExtractsTargetsFromNestedGenericType() {
        val targets = PropertiesTypeInspector.extractTargetFqns("\\Generator<int,\\Properties<\\User>>")
        assertEquals(setOf("\\User"), targets)
    }

    fun testExtractsTargetsFromUnionedPropertiesTypes() {
        val targets = PropertiesTypeInspector.extractTargetFqns("\\Properties<\\User>|\\Properties<\\Admin>|\\null")
        assertEquals(setOf("\\User", "\\Admin"), targets)
    }

    fun testExtractsTargetsFromSignedReceiverType() {
        val targets = PropertiesTypeInspector.extractTargetFqns(PhpType().add("#C\\Properties<\\User>"))
        assertEquals(setOf("\\User"), targets)
    }
}
