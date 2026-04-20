package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeContradictionInspection
import com.github.rahim2797.phputils.magictypes.ide.MagicTypeInspectionSuppressor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PropertiesTypeContradictionInspectionTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MagicTypeContradictionInspection())
    }

    fun testReportsContradictingNativeReturnType() {
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
            "contradiction.php",
            """
            <?php
            /**
* @return <warning descr="\Rahim2797\MagicTypes\Properties<\User> is array-like, but declared return type `string` is not array-compatible.">\Rahim2797\MagicTypes\Properties<\User></warning>
             */
            function getProperties(): string
            {
                return '';
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testAllowsArrayNativeReturnType() {
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
            "arrayReturn.php",
            """
            <?php
            /**
* @return \Rahim2797\MagicTypes\Properties<\User>
             */
            function getProperties(): array
            {
                return [];
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testAllowsUnionContainingArray() {
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
            "unionReturn.php",
            """
            <?php
            /**
* @return \Rahim2797\MagicTypes\Properties<\User>
             */
            function getProperties(): array|string
            {
                return [];
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }

    fun testSuppressesPhpDocSignatureInspectionForImportedPropertiesParam() {
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
            "signatureSuppressedImported.php",
            """
            <?php
            use Rahim2797\MagicTypes\Properties;

            /**
             * @param Pro<caret>perties<User> ${'$'}attributes
             */
            function takesAttributes(array ${'$'}attributes): void {}
            """.trimIndent()
        )

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(element)
        assertTrue(MagicTypeInspectionSuppressor().isSuppressedFor(element!!, "PhpDocSignatureInspection"))
    }

    fun testSuppressesPhpDocSignatureInspectionForFullyQualifiedPropertiesParam() {
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
            "signatureSuppressedFqn.php",
            """
            <?php
            /**
             * @param \Rahim2797\MagicTypes\Pro<caret>perties<User> ${'$'}attributes
             */
            function takesAttributes(array ${'$'}attributes): void {}
            """.trimIndent()
        )

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(element)
        assertTrue(MagicTypeInspectionSuppressor().isSuppressedFor(element!!, "PhpDocSignatureInspection"))
    }
}
