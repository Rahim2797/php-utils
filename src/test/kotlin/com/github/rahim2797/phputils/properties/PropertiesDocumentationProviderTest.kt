package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeDocumentationProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType

class PropertiesDocumentationProviderTest : BasePlatformTestCase() {
    fun testGeneratesShapeDocumentationForPropertiesMagicType() {
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

        val file = myFixture.configureByText(
            "docs.php",
            """
            <?php
            /** @var \Rahim2797\MagicTypes\Properties<\User> ${'$'}props */
            ${'$'}props = [];
            """.trimIndent()
        )

        val phpDocType = PsiTreeUtil.findChildOfType(file, PhpDocType::class.java)
        assertNotNull(phpDocType)

        val html = MagicTypeDocumentationProvider().generateDoc(phpDocType, phpDocType)
        assertNotNull(html)
        assertTrue(html!!.contains("array{"))
        assertTrue(html.contains("email"))
        assertTrue(html.contains("is_active"))
        assertTrue(html.contains("Rahim2797\\MagicTypes\\Properties&lt;User&gt;"))
    }
}
