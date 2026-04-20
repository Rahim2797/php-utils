package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeDocumentationProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.PhpReference

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

    fun testResolvesImportedPropertiesIdentifierToCustomDocumentation() {
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

        myFixture.configureByText(
            "docsImported.php",
            """
            <?php
            use Rahim2797\MagicTypes\Properties;

            /**
             * @param Pro<caret>perties<User> ${'$'}attributes
             */
            function takesAttributes(array ${'$'}attributes): void {}
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset) as? PhpReference
        assertNotNull(reference)

        val provider = MagicTypeDocumentationProvider()
        val context = provider.getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            reference!!.element,
            myFixture.caretOffset
        )
        val html = provider.generateDoc(context, reference.element)

        assertNotNull(context)
        assertTrue(context is PhpDocType)
        assertNotNull(html)
        assertTrue(html!!.contains("array{"))
        assertTrue(html.contains("email"))
        assertTrue(html.contains("Properties&lt;User&gt;"))
    }

    fun testResolvesFullyQualifiedPropertiesIdentifierToCustomDocumentation() {
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

        myFixture.configureByText(
            "docsFqn.php",
            """
            <?php
            /**
             * @param \Rahim2797\MagicTypes\Pro<caret>perties<User> ${'$'}attributes
             */
            function takesAttributes(array ${'$'}attributes): void {}
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset) as? PhpReference
        assertNotNull(reference)

        val provider = MagicTypeDocumentationProvider()
        val context = provider.getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            reference!!.element,
            myFixture.caretOffset
        )
        val html = provider.generateDoc(context, reference.element)

        assertNotNull(context)
        assertTrue(context is PhpDocType)
        assertNotNull(html)
        assertTrue(html!!.contains("array{"))
        assertTrue(html.contains("email"))
        assertTrue(html.contains("Properties&lt;User&gt;"))
    }
}
