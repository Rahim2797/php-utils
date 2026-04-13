package com.github.rahim2797.phputils.properties

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesArrayAccessTypeProviderTest : BasePlatformTestCase() {
    private val provider = PropertiesArrayAccessTypeProvider()

    fun testLocalReceiverResolutionRemainsAvailable() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public $name;
            }
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "localReceiver.php",
            """
            <?php
            /**
             * @var \Properties<\User> ${'$'}properties
             */
            ${'$'}properties = null;
            ${'$'}value = ${'$'}properties['name'];
            """.trimIndent()
        )

        val arrayAccess = PsiTreeUtil.findChildOfType(file, ArrayAccessExpression::class.java)
        assertNotNull(arrayAccess)

        val receiver = arrayAccess!!.value as? PhpExpression
        assertNotNull(receiver)
        assertEquals("\\User", PropertiesTargetResolver.resolveTargetFqnLocally(receiver!!))

        val resolvedType = provider.getType(arrayAccess)
        assertNotNull(resolvedType)
    }

    fun testDeferredPayloadPreservesTargetFqn() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public $name;
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "deferredCompletion.php",
            """
            <?php
            /**
             * @var \Properties<\User> ${'$'}properties
             */
            ${'$'}properties = null;
            ${'$'}value = ${'$'}properties['name'];
            """.trimIndent()
        )

        val encoded = PropertiesArrayAccessTypeCodec.encode("#C\\Properties<\\User>", "name")
        val payload = PropertiesArrayAccessTypeCodec.decode(encoded)
        assertNotNull(payload)
        assertEquals("\\User", PropertiesTypeInspector.extractTargetFqn(payload!!.receiverRawType))
        assertEquals("name", payload.key)
    }

    fun testArrayLiteralTargetInferenceFromParameterDocStillWorks() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public $name;
            }
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "arrayLiteralParam.php",
            """
            <?php
            /**
             * @param \Properties<\User> ${'$'}data
             */
            function takesProperties(array ${'$'}data): void {}
            
            takesProperties(['name' => 'Ada']);
            """.trimIndent()
        )

        val literal = PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
            .first { it.contents == "name" }

        val arrayCreation = PsiGuards.getOwningArrayCreation(literal)
        assertNotNull(arrayCreation)
        assertEquals("\\User", PropertiesTargetResolver.resolveTargetFqnForArrayLiteral(arrayCreation!!))
    }

    fun testTypeProviderDoesNotCrashInDumbMode() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public $name;
            }
            """.trimIndent()
        )

        val file = myFixture.configureByText(
            "dumbModeTypeProvider.php",
            """
            <?php
            /**
             * @var \Properties<\User> ${'$'}properties
             */
            ${'$'}properties = null;
            ${'$'}value = ${'$'}properties['name'];
            """.trimIndent()
        )

        val arrayAccess = PsiTreeUtil.findChildOfType(file, ArrayAccessExpression::class.java)
        assertNotNull(arrayAccess)

        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            provider.getType(arrayAccess!!)
        }
    }

    fun testKeyReferenceContributorDoesNotCrashInDumbMode() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public $name;
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "dumbModeReference.php",
            """
            <?php
            /**
             * @var \Properties<\User> ${'$'}properties
             */
            ${'$'}properties = null;
            ${'$'}value = ${'$'}properties['na<caret>me'];
            """.trimIndent()
        )

        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            myFixture.file.findReferenceAt(myFixture.caretOffset)
        }
    }
}
