package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeArrayAccessTypeProvider
import com.github.rahim2797.phputils.magictypes.parser.MagicTypeParser
import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.DumbModeTestUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesArrayAccessTypeProviderTest : BasePlatformTestCase() {
    private val provider = MagicTypeArrayAccessTypeProvider()

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
             * @var \Rahim2797\MagicTypes\Properties<\User> ${'$'}properties
             */
            ${'$'}properties = null;
            ${'$'}value = ${'$'}properties['name'];
            """.trimIndent()
        )

        val arrayAccess = PsiTreeUtil.findChildOfType(file, ArrayAccessExpression::class.java)
        assertNotNull(arrayAccess)

        val receiver = arrayAccess!!.value as? PhpExpression
        assertNotNull(receiver)
        val match = MagicTypeTargetResolver.resolveArrayAccessMatches(receiver!!).firstOrNull()
        assertEquals("\\User", match?.targetFqns?.firstOrNull())

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
             * @var \Rahim2797\MagicTypes\Properties<\User> ${'$'}properties
             */
            ${'$'}properties = null;
            ${'$'}value = ${'$'}properties['name'];
            """.trimIndent()
        )

        val encoded = PropertiesArrayAccessTypeCodec.encode("#C\\Rahim2797\\MagicTypes\\Properties<\\User>", "name")
        val payload = PropertiesArrayAccessTypeCodec.decode(encoded)
        assertNotNull(payload)
        assertEquals("\\User", MagicTypeParser.extractTargetFqns(payload!!.receiverRawType).firstOrNull())
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
             * @param \Rahim2797\MagicTypes\Properties<\User> ${'$'}data
             */
            function takesProperties(array ${'$'}data): void {}
            
            takesProperties(['name' => 'Ada']);
            """.trimIndent()
        )

        val literal = PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
            .first { it.contents == "name" }

        val arrayCreation = PsiGuards.getOwningArrayCreation(literal)
        assertNotNull(arrayCreation)
        assertEquals("\\User", MagicTypeTargetResolver.resolveArrayLiteralMatches(arrayCreation!!).firstOrNull()?.targetFqns?.firstOrNull())
    }

    fun testArrayLiteralTargetInferenceFromConditionalAssignmentDocStillWorks() {
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

        val file = myFixture.configureByText(
            "arrayLiteralConditionalAssignment.php",
            """
            <?php
            function fallback(): array { return []; }

            /**
             * @var \Rahim2797\MagicTypes\Properties<\User> ${'$'}data
             */
            ${'$'}data = rand(0, 1)
                ? fallback()
                : ['email' => 'Ada'];
            """.trimIndent()
        )

        val literal = PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
            .first { it.contents == "email" }

        val arrayCreation = PsiGuards.getOwningArrayCreation(literal)
        assertNotNull(arrayCreation)
        assertEquals("\\User", MagicTypeTargetResolver.resolveArrayLiteralMatches(arrayCreation!!).firstOrNull()?.targetFqns?.firstOrNull())
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
             * @var \Rahim2797\MagicTypes\Properties<\User> ${'$'}properties
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

    fun testDeferredCompletionReturnsNullInDumbMode() {
        val encoded = PropertiesArrayAccessTypeCodec.encode("#C\\App\\Support\\PropertyBag", "name")

        DumbModeTestUtils.runInDumbModeSynchronously(project) {
            assertNull(provider.complete("#Q$encoded", project))
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
             * @var \Rahim2797\MagicTypes\Properties<\User> ${'$'}properties
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
