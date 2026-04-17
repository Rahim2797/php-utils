package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesKeyReferenceContributorTest : BasePlatformTestCase() {
    fun testArrayAccessKeyResolvesToClassField() {
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

        myFixture.configureByText(
            "resolve.php",
            """
            <?php
            /** @var Properties<\User> ${'$'}props */
            ${'$'}props = [];
            ${'$'}value = ${'$'}props['na<caret>me'];
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset) as? PropertiesKeyReference
        assertNotNull(reference)

        val field = reference!!.resolve()
        assertNotNull(field)
        assertEquals("name", field!!.name)
        assertEquals("\\User", field.containingClass?.fqn)
    }

    fun testArrayAccessVariantsExposeKnownPropertyKeys() {
        myFixture.addFileToProject(
            "User.php",
            """
            <?php
            class User {
                /** @var string */
                public ${'$'}email;

                /** @var bool */
                public ${'$'}is_active;
            }
            """.trimIndent()
        )

        myFixture.configureByText(
            "variants.php",
            """
            <?php
            /** @var Properties<\User> ${'$'}props */
            ${'$'}props = [];
            ${'$'}value = ${'$'}props['<caret>'];
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset) as? PropertiesKeyReference
        assertNotNull(reference)

        val variants = reference!!.variants
            .mapNotNull { it as? com.intellij.codeInsight.lookup.LookupElement }
            .map {
                LookupElementPresentation().also(it::renderElement)
            }

        assertEquals(listOf("email", "is_active"), variants.mapNotNull { it.itemText })
        assertEquals(listOf("string", "bool"), variants.mapNotNull { it.typeText })
    }

    fun testArrayLiteralParameterKeyResolvesToClassField() {
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
            "literalParam.php",
            """
            <?php
            /**
             * @param Properties<\User> ${'$'}props
             */
            function takesProps(array ${'$'}props): void {}

            takesProps([
                'ema<caret>il' => 'hello@example.com',
            ]);
            """.trimIndent()
        )

        val literal = PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java)
            .first { it.contents == "email" }
        val reference = literal.references.singleOrNull() as? PropertiesKeyReference
        assertNotNull(reference)
        assertEquals("email", reference!!.resolve()?.name)
    }
}
