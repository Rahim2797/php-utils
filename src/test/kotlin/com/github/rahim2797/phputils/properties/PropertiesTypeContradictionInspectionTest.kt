package com.github.rahim2797.phputils.properties

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PropertiesTypeContradictionInspectionTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(PropertiesTypeContradictionInspection())
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
             * @return <warning descr="Properties<\User> is array-like, but declared return type `string` is not array-compatible.">Properties<\User></warning>
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
             * @return Properties<\User>
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
             * @return Properties<\User>
             */
            function getProperties(): array|string
            {
                return [];
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()
    }
}
