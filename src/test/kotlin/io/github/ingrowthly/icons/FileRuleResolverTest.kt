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
            exactPathRules = listOf(PathRule("meta-inf/plugin.xml", RuleMatch(iconId = 2, priority = 100))),
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

    @Test
    fun higherPriorityExtensionBeatsLongerExtension() {
        val resolver = resolver(
            extensions = listOf(
                ".ts" to RuleMatch(iconId = 1, priority = 100),
                ".spec.ts" to RuleMatch(iconId = 2, priority = 50),
            ),
        )

        assertEquals(1, resolver.resolveFile("example.spec.ts", "/project/example.spec.ts"))
    }

    @Test
    fun longerExtensionWinsAtSamePriority() {
        val resolver = resolver(
            extensions = listOf(
                ".ts" to RuleMatch(iconId = 1, priority = 100),
                ".spec.ts" to RuleMatch(iconId = 2, priority = 100),
            ),
        )

        assertEquals(2, resolver.resolveFile("example.spec.ts", "/project/example.spec.ts"))
    }

    @Test
    fun pathGlobMatchesNestedAbsolutePath() {
        val resolver = resolver(
            globs = listOf(
                NameGlobRule("actions/", ".ts", pathAware = true, match = RuleMatch(iconId = 3, priority = 100)),
            ),
        )

        assertEquals(3, resolver.resolveFile("build.ts", "/project/src/actions/build.ts"))
        assertEquals(3, resolver.resolveFile("build.ts", "C:\\Project\\ACTIONS\\build.ts"))
        assertEquals(null, resolver.resolveFile("build.ts", "/project/src/transactions/build.ts"))
        assertEquals(null, resolver.resolveFile("build.ts", "/project/src/actions/nested/build.ts"))
    }

    @Test
    fun exactPathHandlesWindowsSeparatorsAndCase() {
        val resolver = resolver(
            paths = listOf(
                PathRule("meta-inf/plugin.xml", RuleMatch(iconId = 4, priority = 100)),
            ),
        )

        assertEquals(4, resolver.resolveFile("plugin.xml", "C:\\Project\\META-INF\\plugin.xml"))
    }

    @Test
    fun priorityBeatsRuleType() {
        val resolver = FileRuleResolver(
            exactNameRules = mapOf("readme.md" to RuleMatch(iconId = 1, priority = 50)),
            exactPathRules = emptyList(),
            nameGlobRules = emptyList(),
            extensionTrie = ExtensionTrie.fromExtensions(
                listOf(".md" to RuleMatch(iconId = 2, priority = 100)),
            ),
        )

        assertEquals(2, resolver.resolveFile("readme.md", "/project/readme.md"))
    }

    @Test
    fun moreSpecificGlobWinsAtSamePriority() {
        val resolver = resolver(
            globs = listOf(
                NameGlobRule("vitest.", ".ts", pathAware = false, match = RuleMatch(iconId = 1, priority = 100)),
                NameGlobRule(
                    "vitest.extensions.",
                    ".ts",
                    pathAware = false,
                    match = RuleMatch(iconId = 2, priority = 100),
                ),
            ),
        )

        assertEquals(2, resolver.resolveFile("vitest.extensions.config.ts", "/project/vitest.extensions.config.ts"))
    }

    private fun resolver(
        paths: List<PathRule> = emptyList(),
        globs: List<NameGlobRule> = emptyList(),
        extensions: List<Pair<String, RuleMatch>> = emptyList(),
    ) = FileRuleResolver(
        exactNameRules = emptyMap(),
        exactPathRules = paths,
        nameGlobRules = globs,
        extensionTrie = ExtensionTrie.fromExtensions(extensions),
    )
}
