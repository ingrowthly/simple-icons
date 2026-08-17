package io.github.ingrowthly.icons

internal data class RuleMatch(
    val iconId: Int,
    val priority: Int,
)

internal data class PathRule(
    val suffix: String,
    val match: RuleMatch,
)

internal data class NameGlobRule(
    val prefix: String,
    val suffix: String,
    val pathAware: Boolean,
    val match: RuleMatch,
) {
    val literalLength: Int = prefix.length + suffix.length
}

internal class FileRuleResolver(
    private val exactNameRules: Map<String, RuleMatch>,
    exactPathRules: List<PathRule>,
    nameGlobRules: List<NameGlobRule>,
    private val extensionTrie: ExtensionTrie,
) {
    private val exactPathRulesByName = exactPathRules.groupBy { it.suffix.substringAfterLast('/') }
    private val globIndex = GlobSuffixIndex(nameGlobRules)

    fun resolveFile(name: String, path: String): Int? {
        var bestIconId = NO_ICON
        var bestRank = Long.MIN_VALUE

        exactNameRules[name]?.let { match ->
            bestIconId = match.iconId
            bestRank = rank(match, TYPE_EXACT_NAME, name.length)
        }

        exactPathRulesByName[name]?.forEach { rule ->
            if (pathEndsWith(path, rule.suffix)) {
                val candidateRank = rank(rule.match, TYPE_EXACT_PATH, rule.suffix.length)
                if (candidateRank > bestRank) {
                    bestIconId = rule.match.iconId
                    bestRank = candidateRank
                }
            }
        }

        globIndex.match(name, path)?.let { rule ->
            val type = if (rule.pathAware) TYPE_GLOB_PATH else TYPE_GLOB_NAME
            val candidateRank = rank(rule.match, type, rule.literalLength)
            if (candidateRank > bestRank) {
                bestIconId = rule.match.iconId
                bestRank = candidateRank
            }
        }

        val extensionNode = extensionTrie.matchNode(name)
        if (extensionNode != NO_MATCH) {
            val candidateRank = rank(extensionTrie.priorityAt(extensionNode), TYPE_EXTENSION, 0)
            if (candidateRank > bestRank) {
                bestIconId = extensionTrie.iconIdAt(extensionNode)
            }
        }

        return bestIconId.takeIf { it != NO_ICON }
    }

    private fun rank(match: RuleMatch, type: Int, literalLength: Int): Long =
        rank(match.priority, type, literalLength)

    // Packing the rank keeps the per-file resolution path allocation-free.
    private fun rank(priority: Int, type: Int, literalLength: Int): Long {
        val specificity = (type shl TYPE_SHIFT) or literalLength.coerceAtMost(LENGTH_MASK)
        return (priority.toLong() shl Int.SIZE_BITS) or (specificity.toLong() and UINT_MASK)
    }

    private companion object {
        const val NO_ICON = -1
        const val NO_MATCH = -1
        const val TYPE_SHIFT = 24
        const val LENGTH_MASK = 0x00FF_FFFF
        const val UINT_MASK = 0xFFFF_FFFFL

        const val TYPE_EXTENSION = 1
        const val TYPE_GLOB_NAME = 2
        const val TYPE_GLOB_PATH = 3
        const val TYPE_EXACT_NAME = 4
        const val TYPE_EXACT_PATH = 5
    }
}
