package com.github.rahim2797.phputils.properties

import com.github.rahim2797.phputils.magictypes.ide.MagicTypeArrayAccessTypeProvider
import com.github.rahim2797.phputils.magictypes.ide.MagicTypeDocumentationProvider
import com.github.rahim2797.phputils.magictypes.ide.MagicTypeKeyReference
import com.github.rahim2797.phputils.magictypes.targets.MagicTypeTargetResolver
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocType
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.PhpExpression
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class PropertiesLargeFixtureStressTest : LaravelFixtureTestCase() {
    private val provider = MagicTypeArrayAccessTypeProvider()

    fun testLargeInteractingFilesStressArrayAccessResolution() {
        installLargeStressFixture()

        val file = openFixtureFile("tests/Plugin/Properties/Stress/large_access.php")
        val accesses = PsiTreeUtil.findChildrenOfType(file, ArrayAccessExpression::class.java).toList()
        assertTrue("Expected many array accesses in stress fixture", accesses.size >= 80)

        repeat(3) {
            accesses.forEach { access ->
                val receiver = access.value as? PhpExpression
                assertNotNull(receiver)

                val matches = MagicTypeTargetResolver.resolveArrayAccessMatches(receiver!!)
                assertFalse("Expected at least one match for ${access.text}", matches.isEmpty())

                val resolvedType = provider.getType(access)
                assertNotNull("Expected resolved type for ${access.text}", resolvedType)
                assertTrue("Expected non-empty type set for ${access.text}", resolvedType!!.types.isNotEmpty())
            }
        }

        val sharedAccess = accesses.first { it.text.contains("['shared_120']") }
        val sharedType = provider.getType(sharedAccess)
        assertNotNull(sharedType)
        assertTrue(sharedType!!.types.contains("\\string"))
        assertTrue(sharedType.types.contains("\\int"))

        val uniqueAccess = accesses.first { it.text.contains("['user_only_080']") }
        val uniqueType = provider.getType(uniqueAccess)
        assertNotNull(uniqueType)
        assertTrue(uniqueType!!.types.contains("\\string"))
    }

    fun testLargeInteractingFilesStressCompletionResolutionAndDocs() {
        installLargeStressFixture()

        val file = openFixtureFile("tests/Plugin/Properties/Stress/large_docs.php")
        val phpDocType = PsiTreeUtil.findChildOfType(file, PhpDocType::class.java)
        assertNotNull(phpDocType)

        val literals = PsiTreeUtil.findChildrenOfType(file, StringLiteralExpression::class.java).toList()
        val targetLiteral = literals.first { it.contents == "admin_only_080" }
        val targetMatches = MagicTypeTargetResolver.resolveLiteralMatches(targetLiteral)
        assertFalse("Expected literal matches for stress doc fixture", targetMatches.isEmpty())
        val targetReference = MagicTypeKeyReference(targetLiteral, targetLiteral.contents, targetMatches)

        val variants = targetReference.variants
            .mapNotNull { it as? LookupElement }
            .map { it.lookupString }
            .sorted()

        assertTrue("Expected lots of completion variants", variants.size >= 280)
        assertTrue(variants.contains("shared_001"))
        assertTrue(variants.contains("shared_120"))
        assertTrue(variants.contains("user_only_080"))
        assertTrue(variants.contains("admin_only_080"))
        assertEquals("admin_only_080", (targetReference.resolve() as? Field)?.name)

        val html = MagicTypeDocumentationProvider().generateDoc(phpDocType, phpDocType)
        assertNotNull(html)
        assertTrue(html!!.contains("shared_001"))
        assertTrue(html.contains("shared_120"))
        assertTrue(html.contains("user_only_080"))
        assertTrue(html.contains("StressUser"))
    }

    private fun installLargeStressFixture() {
        val sharedFields = (1..120).map { "shared_${it.toString().padStart(3, '0')}" }
        val userOnlyFields = (1..80).map { "user_only_${it.toString().padStart(3, '0')}" }
        val adminOnlyFields = (1..80).map { "admin_only_${it.toString().padStart(3, '0')}" }

        myFixture.addFileToProject(
            "app/Models/StressUser.php",
            buildModelClass(
                namespace = "App\\Models",
                className = "StressUser",
                fields = sharedFields.associateWith { "\\string" } + userOnlyFields.associateWith { "\\string" },
            )
        )
        myFixture.addFileToProject(
            "app/Models/StressAdmin.php",
            buildModelClass(
                namespace = "App\\Models",
                className = "StressAdmin",
                fields = sharedFields.associateWith { "\\int" } + adminOnlyFields.associateWith { "\\bool" },
            )
        )
        myFixture.addFileToProject(
            "app/Contracts/ProvidesStressProperties.php",
            """
            <?php
            
            namespace App\Contracts;
            
            interface ProvidesStressProperties
            {
                /**
                 * @return \Rahim2797\MagicTypes\Properties<\App\Models\StressUser>|\Rahim2797\MagicTypes\Properties<\App\Models\StressAdmin>
                 */
                public function provide(): array;
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/Support/AbstractStressProvider.php",
            """
            <?php
            
            namespace App\Support;
            
            use App\Contracts\ProvidesStressProperties;
            
            abstract class AbstractStressProvider implements ProvidesStressProperties
            {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/Support/LargeStressProvider.php",
            """
            <?php
            
            namespace App\Support;
            
            final class LargeStressProvider extends AbstractStressProvider
            {
                public function provide(): array
                {
                    return [];
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/Support/LargeStressHolder.php",
            """
            <?php
            
            namespace App\Support;
            
            final class LargeStressHolder
            {
                /**
                 * @var \Rahim2797\MagicTypes\Properties<\App\Models\StressUser>|\Rahim2797\MagicTypes\Properties<\App\Models\StressAdmin>
                 */
                public array ${'$'}props = [];
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "app/Support/LargeStressFunctions.php",
            """
            <?php
            
            namespace App\Support;
            
            /**
             * @return \Rahim2797\MagicTypes\Properties<\App\Models\StressUser>|\Rahim2797\MagicTypes\Properties<\App\Models\StressAdmin>
             */
            function loadStressProps(): array
            {
                return [];
            }
            
            /**
             * @param \Rahim2797\MagicTypes\Properties<\App\Models\StressUser>|\Rahim2797\MagicTypes\Properties<\App\Models\StressAdmin> ${'$'}props
             */
            function acceptStressProps(array ${'$'}props): void
            {
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "tests/Plugin/Properties/Stress/large_access.php",
            buildLargeAccessFile(sharedFields, userOnlyFields, adminOnlyFields)
        )
        myFixture.addFileToProject(
            "tests/Plugin/Properties/Stress/large_docs.php",
            """
            <?php
            
            /**
             * @var \Rahim2797\MagicTypes\Properties<\App\Models\StressUser>|\Rahim2797\MagicTypes\Properties<\App\Models\StressAdmin> ${'$'}props
             */
            ${'$'}props = [];
            ${'$'}value = ${'$'}props['admin_only_080'];
            """.trimIndent()
        )

        waitForSmartMode()
    }

    private fun buildModelClass(
        namespace: String,
        className: String,
        fields: Map<String, String>,
    ): String {
        val renderedFields = fields.entries.joinToString("\n\n") { (fieldName, fieldType) ->
            """
                /** @var $fieldType */
                public ${'$'}$fieldName;
            """.trimIndent()
        }

        return """
            <?php
            
            namespace $namespace;
            
            class $className
            {
            $renderedFields
            }
        """.trimIndent()
    }

    private fun buildLargeAccessFile(
        sharedFields: List<String>,
        userOnlyFields: List<String>,
        adminOnlyFields: List<String>,
    ): String {
        val sharedAccesses = sharedFields.take(40).joinToString("\n") { field ->
            "${'$'}shared_${field.takeLast(3)} = ${'$'}props['$field'];"
        }
        val userAccesses = userOnlyFields.take(20).joinToString("\n") { field ->
            "${'$'}user_${field.takeLast(3)} = ${'$'}holder->props['$field'];"
        }
        val adminAccesses = adminOnlyFields.take(20).joinToString("\n") { field ->
            "${'$'}admin_${field.takeLast(3)} = loadStressProps()['$field'];"
        }
        val literalEntries = (sharedFields.takeLast(10) + userOnlyFields.takeLast(5) + adminOnlyFields.takeLast(5))
            .joinToString(",\n") { field -> "    '$field' => null" }

        return """
            <?php
            
            use App\Support\LargeStressHolder;
            use App\Support\LargeStressProvider;
            use function App\Support\acceptStressProps;
            use function App\Support\loadStressProps;
            
            function runLargeStressFixture(): void
            {
                ${'$'}provider = new LargeStressProvider();
                ${'$'}props = ${'$'}provider->provide();
                ${'$'}holder = new LargeStressHolder();
                
                $sharedAccesses
                
                $userAccesses
                
                $adminAccesses
                
                acceptStressProps([
            $literalEntries
                ]);
                
                ${'$'}tail = ${'$'}props['shared_120'];
                ${'$'}userTail = ${'$'}holder->props['user_only_080'];
                ${'$'}adminTail = loadStressProps()['admin_only_080'];
            }
            
            runLargeStressFixture();
            """.trimIndent()
    }
}
