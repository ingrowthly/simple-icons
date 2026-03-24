package io.github.ingrowthly.icons

internal data class RuleMatch(
    val iconId: Int,
    val priority: Int,
    val specificity: Int = 0,
)

internal data class NameGlobRule(
    val prefix: String,
    val suffix: String,
    val pathAware: Boolean,
    val match: RuleMatch,
)

internal class FileRuleResolver(
    private val exactNameRules: Map<String, RuleMatch>,
    private val exactPathRules: List<Pair<String, RuleMatch>>,
    private val nameGlobRules: List<NameGlobRule>,
    private val extensionTrie: ExtensionTrie,
) {
    fun resolveFile(name: String, path: String): Int? {
        var best: RuleMatch? = null

        exactNameRules[name]?.let { best = pick(best, it, SPECIFICITY_EXACT_NAME) }

        for ((pathSuffix, match) in exactPathRules) {
            if (path == pathSuffix || path.endsWith("/$pathSuffix")) {
                best = pick(best, match, SPECIFICITY_EXACT_PATH)
                break
            }
        }

        for (rule in nameGlobRules) {
            val subject = if (rule.pathAware) path else name
            if (subject.startsWith(rule.prefix) && subject.endsWith(rule.suffix)) {
                best = pick(best, rule.match, if (rule.pathAware) SPECIFICITY_GLOB_PATH else SPECIFICITY_GLOB_NAME)
            }
        }

        extensionTrie.match(name)?.let { best = pick(best, it, SPECIFICITY_EXTENSION) }

        return best?.iconId
    }

    private fun pick(current: RuleMatch?, candidate: RuleMatch, specificity: Int): RuleMatch {
        val rankedCandidate = candidate.copy(specificity = specificity)
        if (current == null) return rankedCandidate

        return when {
            rankedCandidate.priority > current.priority -> rankedCandidate
            rankedCandidate.priority < current.priority -> current
            rankedCandidate.specificity > current.specificity -> rankedCandidate
            else -> current
        }
    }

    internal companion object {
        const val SPECIFICITY_EXTENSION = 100
        const val SPECIFICITY_GLOB_NAME = 200
        const val SPECIFICITY_GLOB_PATH = 300
        const val SPECIFICITY_EXACT_NAME = 400
        const val SPECIFICITY_EXACT_PATH = 500
    }
}

internal class ExtensionTrie(
    private val iconIds: IntArray,
    private val priorities: IntArray,
    private val edgeStarts: IntArray,
    private val edgeCounts: IntArray,
    private val edgeChars: CharArray,
    private val edgeTargets: IntArray,
) {
    fun match(name: String): RuleMatch? {
        var nodeIndex = 0
        var best: RuleMatch? = null

        for (index in name.lastIndex downTo 0) {
            val edgeIndex = findEdge(nodeIndex, name[index]) ?: break
            nodeIndex = edgeTargets[edgeIndex]

            val iconId = iconIds[nodeIndex]
            if (iconId >= 0) {
                best = RuleMatch(iconId = iconId, priority = priorities[nodeIndex])
            }
        }

        return best
    }

    private fun findEdge(nodeIndex: Int, target: Char): Int? {
        var low = edgeStarts[nodeIndex]
        var high = low + edgeCounts[nodeIndex] - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            when (val candidate = edgeChars[mid]) {
                target -> return mid
                else -> if (candidate < target) low = mid + 1 else high = mid - 1
            }
        }

        return null
    }

    internal companion object {
        fun fromExtensions(entries: List<Pair<String, RuleMatch>>): ExtensionTrie {
            class Node {
                val children = sortedMapOf<Char, Node>()
                var match: RuleMatch? = null
                var index: Int = -1
            }

            val root = Node()
            entries.sortedByDescending { it.second.priority }.forEach { (extension, match) ->
                var node = root
                for (char in extension.reversed()) {
                    node = node.children.getOrPut(char) { Node() }
                }
                val existing = node.match
                if (existing == null || match.priority > existing.priority) {
                    node.match = match
                }
            }

            val nodes = mutableListOf<Node>()
            fun collect(node: Node) {
                if (node.index != -1) return
                node.index = nodes.size
                nodes += node
                node.children.values.forEach(::collect)
            }
            collect(root)

            val iconIds = IntArray(nodes.size)
            val priorities = IntArray(nodes.size)
            val edgeStarts = IntArray(nodes.size)
            val edgeCounts = IntArray(nodes.size)

            var edgeStart = 0
            nodes.forEachIndexed { index, node ->
                iconIds[index] = node.match?.iconId ?: -1
                priorities[index] = node.match?.priority ?: Int.MIN_VALUE
                edgeStarts[index] = edgeStart
                edgeCounts[index] = node.children.size
                edgeStart += node.children.size
            }

            val edgeChars = CharArray(edgeStart)
            val edgeTargets = IntArray(edgeStart)
            var cursor = 0
            nodes.forEach { node ->
                node.children.forEach { (char, child) ->
                    edgeChars[cursor] = char
                    edgeTargets[cursor] = child.index
                    cursor++
                }
            }

            return ExtensionTrie(iconIds, priorities, edgeStarts, edgeCounts, edgeChars, edgeTargets)
        }
    }
}
