package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeArrayAccessTypeProvider
import com.github.rahim2797.phputils.magictypes.ide.MagicTypeKeyReference
import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesCrossFileSupportTest : BasePlatformTestCase() {
    private val provider = MagicTypeArrayAccessTypeProvider()

    fun testCrossFileFunctionReturnSupportsArrayAccessTypeInference() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public ${'$'}name;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "PropertiesApi.php",
            """
            <?php
            /**
             * @return \Rahim2797\MagicTypes\Properties<\User>
             */
            function getUserProperties(): array
            {
                return [];
            }
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "consumer.php",
            """
            <?php
            ${'$'}value = getUserProperties()['name'];
            """.trimIndent()
        )

        val arrayAccess = PsiTreeUtil.findChildOfType(file, ArrayAccessExpression::class.java)
        assertNotNull(arrayAccess)

        val receiver = arrayAccess!!.value as? PhpExpression
        assertNotNull(receiver)
        assertEquals(setOf("\\User"), MagicTypeTargetResolver.resolveArrayAccessMatches(receiver!!).flatMapTo(linkedSetOf()) { it.targetFqns })

        val resolvedType = provider.getType(arrayAccess)
        assertNotNull(resolvedType)
        assertTrue(resolvedType!!.types.contains("\\string"))
    }

    fun testCrossFilePropertyDocSupportsKeyReferenceResolution() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public ${'$'}email;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Holder.php",
            """
            <?php
            class Holder {
                /** @var \Rahim2797\MagicTypes\Properties<\User> */
                public array ${'$'}props = [];
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "consumer.php",
            """
            <?php
            ${'$'}holder = new Holder();
            ${'$'}value = ${'$'}holder->props['ema<caret>il'];
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset) as? MagicTypeKeyReference
        assertNotNull(reference)
        assertEquals("email", (reference!!.resolve() as? Field)?.name)
    }

    fun testInheritedMethodDocsPropagatePropertiesTargets() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public ${'$'}name;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Contracts.php",
            """
            <?php
            interface ProvidesProperties {
                /**
                 * @return \Rahim2797\MagicTypes\Properties<\User>
                 */
                public function getProps(): array;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Provider.php",
            """
            <?php
            class Provider implements ProvidesProperties {
                public function getProps(): array
                {
                    return [];
                }
            }
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "consumer.php",
            """
            <?php
            ${'$'}value = (new Provider())->getProps()['name'];
            """.trimIndent()
        )

        val arrayAccess = PsiTreeUtil.findChildOfType(file, ArrayAccessExpression::class.java)
        assertNotNull(arrayAccess)

        val resolvedType = provider.getType(arrayAccess!!)
        assertNotNull(resolvedType)
        assertTrue(resolvedType!!.types.contains("\\string"))
    }

    fun testCrossFileParameterDocsSupportArrayLiteralKeys() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public ${'$'}name;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "PropertiesApi.php",
            """
            <?php
            /**
             * @param \Rahim2797\MagicTypes\Properties<\User> ${'$'}props
             */
            function takesProperties(array ${'$'}props): void {}
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "consumer.php",
            """
            <?php
            takesProperties([
                'na<caret>me' => 'Ada',
            ]);
            """.trimIndent()
        )

        val literal = PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
            .first { it.contents == "name" }
        val reference = literal.references.singleOrNull() as? MagicTypeKeyReference
        assertNotNull(reference)
        assertEquals("name", (reference!!.resolve() as? Field)?.name)
    }

    fun testUnionTargetsMergeFieldTypesAndVariants() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public ${'$'}name;

                /** @var string */
                public ${'$'}email;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "Admin.php",
            """
            <?php
            class Admin {
                /** @var int */
                public ${'$'}name;

                /** @var bool */
                public ${'$'}is_active;
            }
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "union.php",
            """
            <?php
            /**
             * @return \Rahim2797\MagicTypes\Properties<\User>|\Rahim2797\MagicTypes\Properties<\Admin>
             */
            function getProperties(): array
            {
                return [];
            }

            ${'$'}props = getProperties();
            ${'$'}value = ${'$'}props['name'];
            ${'$'}other = ${'$'}props['<caret>'];
            """.trimIndent()
        )

        val arrayAccesses = PsiTreeUtil.findChildrenOfType(file, ArrayAccessExpression::class.java).toList()
        val valueAccess = arrayAccesses.first { it.text.contains("['name']") }
        val valueType = provider.getType(valueAccess)
        assertNotNull(valueType)
        assertTrue(valueType!!.types.contains("\\string"))
        assertTrue(valueType.types.contains("\\int"))

        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset) as? MagicTypeKeyReference
        assertNotNull(reference)

        val variants = reference!!.variants
            .mapNotNull { it as? com.intellij.codeInsight.lookup.LookupElement }
            .map { it.lookupString }
            .sorted()

        assertEquals(listOf("email", "is_active", "name"), variants)
    }
}
