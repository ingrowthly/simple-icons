package io.github.ingrowthly.icons

import org.junit.Assert.assertEquals
import org.junit.Test

class FileRuleResolverTest {
    @Test
    fun exactNameBeatsExtensionAtSamePriority() {
        val resolver = FileRuleResolver(
            exactNameRules = mapOf("readme.md" to RuleMatch(iconId = 1, priority = 100)),
            exactPathRules = emptyList(),
            nameGlobRules = emptyList(),
            extensionTrie = ExtensionTrie.fromExtensions(
                listOf(".md" to RuleMatch(iconId = 2, priority = 100)),
            ),
        )

        assertEquals(1, resolver.resolveFile("readme.md", "readme.md"))
    }

    @Test
    fun exactPathBeatsExactNameAtSamePriority() {
        val resolver = FileRuleResolver(
            exactNameRules = mapOf("plugin.xml" to RuleMatch(iconId = 1, priority = 100)),
            exactPathRules = listOf("meta-inf/plugin.xml" to RuleMatch(iconId = 2, priority = 100)),
            nameGlobRules = emptyList(),
            extensionTrie = ExtensionTrie.fromExtensions(emptyList()),
        )

        assertEquals(2, resolver.resolveFile("plugin.xml", "src/main/resources/meta-inf/plugin.xml"))
    }

    @Test
    fun nameGlobMatchesWithoutRegex() {
        val resolver = FileRuleResolver(
            exactNameRules = emptyMap(),
            exactPathRules = emptyList(),
            nameGlobRules = listOf(
                NameGlobRule("vitest.", ".config.ts", pathAware = false, match = RuleMatch(iconId = 3, priority = 100)),
            ),
            extensionTrie = ExtensionTrie.fromExtensions(
                listOf(".ts" to RuleMatch(iconId = 4, priority = 1)),
            ),
        )

        assertEquals(3, resolver.resolveFile("vitest.extensions.config.ts", "vitest.extensions.config.ts"))
    }

    @Test
    fun pathAwareGlobMatchesNormalizedPath() {
        val resolver = FileRuleResolver(
            exactNameRules = emptyMap(),
            exactPathRules = emptyList(),
            nameGlobRules = listOf(
                NameGlobRule(".github/", ".yml", pathAware = true, match = RuleMatch(iconId = 5, priority = 90)),
            ),
            extensionTrie = ExtensionTrie.fromExtensions(
                listOf(".yml" to RuleMatch(iconId = 6, priority = 1)),
            ),
        )

        assertEquals(5, resolver.resolveFile("settings.yml", ".github/settings.yml"))
        assertEquals(6, resolver.resolveFile("settings.yml", "config/settings.yml"))
    }

    @Test
    fun extensionRuleHandlesPlainFiles() {
        val resolver = FileRuleResolver(
            exactNameRules = emptyMap(),
            exactPathRules = emptyList(),
            nameGlobRules = emptyList(),
            extensionTrie = ExtensionTrie.fromExtensions(
                listOf(".go" to RuleMatch(iconId = 7, priority = 50)),
            ),
        )

        assertEquals(7, resolver.resolveFile("main.go", "src/main.go"))
    }
}
