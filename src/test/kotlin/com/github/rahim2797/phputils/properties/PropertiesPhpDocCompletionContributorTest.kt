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
        assertEquals("magic type", firstPresentation.typeText)

        val propertiesEntries = items
            .map {
                LookupElementPresentation().also(it::renderElement)
            }
            .filter { it.itemText == "Properties" }

        assertTrue(propertiesEntries.size >= 2)
        assertTrue(propertiesEntries.any { it.typeText != "magic type" })
    }

    fun testMagicPropertiesInsertionAddsGenericPlaceholders() {
        myFixture.addFileToProject(
            "PropertiesBag.php",
            """
            <?php
            class PropertiesBag {}
            """.trimIndent()
        )

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
            /** @var Properties<<caret>> ${'$'}props */
            """.trimIndent()
        )
    }

    fun testMagicPropertiesInsertionReusesExistingAngleBrackets() {
        myFixture.addFileToProject(
            "PropertiesBag.php",
            """
            <?php
            class PropertiesBag {}
            """.trimIndent()
        )

        myFixture.configureByText(
            "reuseGenerics.php",
            """
            <?php
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
            /** @var Properties<<caret>> ${'$'}props */
            """.trimIndent()
        )
    }

    private fun com.intellij.codeInsight.lookup.LookupElement.isMagicPropertiesLookup(): Boolean {
        val presentation = LookupElementPresentation()
        renderElement(presentation)
        return presentation.itemText == "Properties" && presentation.typeText == "magic type"
    }
}
