package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.MagicTypeRegistry
import com.github.rahim2797.phputils.magictypes.parser.MagicTypeParser
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.resolve.types.PhpType

class PropertiesTypeInspectorTest : BasePlatformTestCase() {
    fun testRegistryExposesPropertiesHandler() {
        assertTrue(MagicTypeRegistry.handlers.any { it.descriptor.id == "properties" })
    }

    fun testCanonicalPropertiesStubIsIndexedAsPhpClass() {
        val classes = PhpIndex.getInstance(project).getClassesByFQN(PropertiesMagicTypeNames.CANONICAL_FQN)
        assertTrue(classes.isNotEmpty())
        assertEquals(PropertiesMagicTypeNames.CANONICAL_FQN, classes.first().fqn)
        assertEquals("Properties.php", classes.first().containingFile.name)
    }

    fun testExtractsTargetsFromNestedGenericType() {
        val targets = MagicTypeParser.extractTargetFqns("\\Generator<int,\\Rahim2797\\MagicTypes\\Properties<\\User>>")
        assertEquals(setOf("\\User"), targets)
    }

    fun testExtractsTargetsFromUnionedPropertiesTypes() {
        val targets = MagicTypeParser.extractTargetFqns("\\Rahim2797\\MagicTypes\\Properties<\\User>|\\Rahim2797\\MagicTypes\\Properties<\\Admin>|\\null")
        assertEquals(setOf("\\User", "\\Admin"), targets)
    }

    fun testExtractsTargetsFromSignedReceiverType() {
        val targets = MagicTypeParser.extractTargetFqns(PhpType().add("#C\\Rahim2797\\MagicTypes\\Properties<\\User>"))
        assertEquals(setOf("\\User"), targets)
    }
}
