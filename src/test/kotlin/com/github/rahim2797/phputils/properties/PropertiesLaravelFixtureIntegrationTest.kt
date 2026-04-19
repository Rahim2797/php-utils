package com.github.rahim2797.phputils.properties

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.Field

class PropertiesLaravelFixtureIntegrationTest : LaravelFixtureTestCase() {
    private val provider = PropertiesArrayAccessTypeProvider()

    fun testEloquentCompletionUsesHelperAttributesInsteadOfModelFields() {
        openFixtureFile("tests/Plugin/Properties/product_completion.php")

        val variants = keyReferenceAtCaret().variants
            .mapNotNull { it as? LookupElement }
            .map { it.lookupString }
            .sorted()

        assertTrue(variants.containsAll(listOf("created_at", "id", "is_active", "price_cents", "published_at", "sku", "title", "updated_at")))
        assertFalse(variants.contains("transientLabel"))
    }

    fun testResolvedLaravelAttributeComesFromIdeHelperFile() {
        val file = openFixtureFile("tests/Plugin/Properties/product_access.php")
        val literal = PsiTreeUtil.findChildrenOfType(file, com.jetbrains.php.lang.psi.elements.StringLiteralExpression::class.java)
            .first { it.contents == "sku" }

        val reference = literal.references.singleOrNull() as? PropertiesKeyReference
        assertNotNull(reference)

        val resolved = reference!!.resolve() as? Field
        assertNotNull(resolved)
        assertEquals("_ide_helper_models.php", resolved!!.containingFile.name)
        assertEquals("sku", resolved.name)
    }

    fun testLaravelAttributeAccessInfersHelperValueType() {
        openFixtureFile("tests/Plugin/Properties/product_access.php")

        val resolvedType = provider.getType(firstArrayAccess())
        assertNotNull(resolvedType)
        assertTrue(resolvedType!!.types.contains("\\string"))
    }

    fun testInheritedContractReturnWorksInsideLaravelFixture() {
        openFixtureFile("tests/Plugin/Properties/contract_return.php")

        val resolvedType = provider.getType(firstArrayAccess())
        assertNotNull(resolvedType)
        assertTrue(resolvedType!!.types.contains("\\string"))
    }

    fun testArrayLiteralParameterCompletionUsesHelperAttributes() {
        openFixtureFile("tests/Plugin/Properties/parameter_literal.php")

        val variants = keyReferenceAtCaret().variants
            .mapNotNull { it as? LookupElement }
            .map { it.lookupString }
            .sorted()

        assertTrue(variants.contains("sku"))
        assertFalse(variants.contains("transientLabel"))
    }

    fun testUnionCompletionMergesModelHelperAttributesWithoutRuntimeFields() {
        openFixtureFile("tests/Plugin/Properties/union_access.php")

        val variants = keyReferenceAtCaret().variants
            .mapNotNull { it as? LookupElement }
            .map { it.lookupString }
            .sorted()

        assertTrue(variants.containsAll(listOf("is_active", "plan", "sku", "title")))
        assertFalse(variants.contains("runtimeOnlyFlag"))
        assertFalse(variants.contains("transientLabel"))
    }

    fun testDtoCompletionStillUsesRealClassFields() {
        openFixtureFile("tests/Plugin/Properties/dto_completion.php")

        val variants = keyReferenceAtCaret().variants
            .mapNotNull { it as? LookupElement }
            .map { it.lookupString }
            .sorted()

        assertEquals(listOf("is_active", "sku", "title"), variants)
    }

    fun testMagicTypeDocumentationUsesLaravelHelperShape() {
        val file = openFixtureFile("tests/Plugin/Properties/product_completion.php")
        val phpDocType = PsiTreeUtil.findChildOfType(file, PhpDocType::class.java)
        assertNotNull(phpDocType)

        val html = PropertiesDocumentationProvider().generateDoc(phpDocType, phpDocType)
        assertNotNull(html)
        assertTrue(html!!.contains("sku"))
        assertTrue(html.contains("price_cents"))
        assertFalse(html.contains("transientLabel"))
    }
}
