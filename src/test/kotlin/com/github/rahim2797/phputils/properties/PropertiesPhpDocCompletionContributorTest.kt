package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PropertiesPhpDocCompletionContributorTest : BasePlatformTestCase() {

    fun testMagicPropertiesTypeIsRankedFirstInPhpDocCompletion() {
        myFixture.addFileToProject(
            "AppProperties.php",
            """
            <?php
            namespace App;
            
            class Properties {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "AppPropertiesBag.php",
            """
            <?php
            namespace App;
            
            class PropertiesBag {}
            """.trimIndent()
        )

        myFixture.configureByText(
            "completion.php",
            """
            <?php
            /** @var Pro<caret> ${'$'}props */
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertTrue(items!!.isNotEmpty())

        val firstPresentation = LookupElementPresentation()
        items.first().renderElement(firstPresentation)

        assertEquals("Properties", firstPresentation.itemText)
        assertEquals(PropertiesMagicTypeNames.QUALIFIED_NAME, firstPresentation.typeText)

        val propertiesEntries = items
            .map {
                LookupElementPresentation().also(it::renderElement)
            }
            .filter { it.itemText == "Properties" }

        assertTrue(propertiesEntries.size >= 2)
        assertTrue(propertiesEntries.any { it.typeText != PropertiesMagicTypeNames.QUALIFIED_NAME })
    }

    fun testMagicPropertiesInsertionAddsCanonicalFqnWithoutImport() {
        myFixture.configureByText(
            "insertGenerics.php",
            """
            <?php
            /** @var Pro<caret> ${'$'}props */
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertNotNull(myFixture.lookup)
        myFixture.lookup.currentItem = items!!.first { it.isMagicPropertiesLookup() }
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)

        myFixture.checkResult(
            """
            <?php
            /** @var \Rahim2797\MagicTypes\Properties<<caret>> ${'$'}props */
            """.trimIndent()
        )
    }

    fun testMagicPropertiesInsertionUsesImportedShortName() {
        myFixture.configureByText(
            "reuseGenerics.php",
            """
            <?php
            use Rahim2797\MagicTypes\Properties;

            /** @var Pro<caret><> ${'$'}props */
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertNotNull(myFixture.lookup)
        myFixture.lookup.currentItem = items!!.first { it.isMagicPropertiesLookup() }
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)

        myFixture.checkResult(
            """
            <?php
            use Rahim2797\MagicTypes\Properties;

            /** @var Properties<<caret>> ${'$'}props */
            """.trimIndent()
        )
    }

    fun testMagicPropertiesInsertionUsesImportedAlias() {
        myFixture.configureByText(
            "aliasInsert.php",
            """
            <?php
            use Rahim2797\MagicTypes\Properties as MTProps;

            /** @var Pro<caret> ${'$'}props */
            """.trimIndent()
        )

        val items = myFixture.completeBasic()
        assertNotNull(items)
        assertNotNull(myFixture.lookup)
        myFixture.lookup.currentItem = items!!.first { it.isMagicPropertiesLookup() }
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)

        myFixture.checkResult(
            """
            <?php
            use Rahim2797\MagicTypes\Properties as MTProps;

            /** @var MTProps<<caret>> ${'$'}props */
            """.trimIndent()
        )
    }

    private fun com.intellij.codeInsight.lookup.LookupElement.isMagicPropertiesLookup(): Boolean {
        val presentation = LookupElementPresentation()
        renderElement(presentation)
        return presentation.itemText == "Properties" && presentation.typeText == PropertiesMagicTypeNames.QUALIFIED_NAME
    }
}
