package io.github.ingrowthly.icons

internal class GlobSuffixIndex(rules: List<NameGlobRule>) {
    private class Node {
        val children = sortedMapOf<Char, Node>()
        val rules = mutableListOf<NameGlobRule>()
        var index = -1
    }

    private val edgeStarts: IntArray
    private val edgeCounts: IntArray
    private val edgeChars: CharArray
    private val edgeTargets: IntArray
    private val rulesByNode: Array<List<NameGlobRule>>

    init {
        val root = Node()
        rules.forEach { rule ->
            var node = root
            for (char in rule.suffix.reversed()) {
                node = node.children.getOrPut(char) { Node() }
            }
            node.rules += rule
        }

        val nodes = mutableListOf<Node>()
        fun collect(node: Node) {
            node.index = nodes.size
            nodes += node
            node.children.values.forEach(::collect)
        }
        collect(root)

        edgeStarts = IntArray(nodes.size)
        edgeCounts = IntArray(nodes.size)
        rulesByNode = Array(nodes.size) { nodes[it].rules }

        var edgeStart = 0
        nodes.forEachIndexed { index, node ->
            edgeStarts[index] = edgeStart
            edgeCounts[index] = node.children.size
            edgeStart += node.children.size
        }

        edgeChars = CharArray(edgeStart)
        edgeTargets = IntArray(edgeStart)
        var cursor = 0
        nodes.forEach { node ->
            node.children.forEach { (char, child) ->
                edgeChars[cursor] = char
                edgeTargets[cursor] = child.index
                cursor++
            }
        }
    }

    fun match(name: String, path: String): NameGlobRule? {
        var nodeIndex = 0
        var best: NameGlobRule? = null

        for (index in name.lastIndex downTo 0) {
            val edgeIndex = findEdge(nodeIndex, name[index])
            if (edgeIndex == NO_MATCH) break
            nodeIndex = edgeTargets[edgeIndex]

            for (rule in rulesByNode[nodeIndex]) {
                val matches = if (rule.pathAware) {
                    pathGlobMatches(path, rule.prefix)
                } else {
                    name.length >= rule.literalLength && name.startsWith(rule.prefix)
                }
                if (matches && isBetter(rule, best)) best = rule
            }
        }

        return best
    }

    private fun findEdge(nodeIndex: Int, target: Char): Int {
        var low = edgeStarts[nodeIndex]
        var high = low + edgeCounts[nodeIndex] - 1

        while (low <= high) {
            val mid = (low + high) ushr 1
            when (val candidate = edgeChars[mid]) {
                target -> return mid
                else -> if (candidate < target) low = mid + 1 else high = mid - 1
            }
        }

        return NO_MATCH
    }

    private fun isBetter(candidate: NameGlobRule, current: NameGlobRule?): Boolean {
        if (current == null) return true
        return when {
            candidate.match.priority != current.match.priority -> candidate.match.priority > current.match.priority
            candidate.pathAware != current.pathAware -> candidate.pathAware
            else -> candidate.literalLength > current.literalLength
        }
    }

    private companion object {
        const val NO_MATCH = -1
    }
}

internal fun pathEndsWith(path: String, suffix: String): Boolean {
    val start = path.length - suffix.length
    if (start < 0 || start > 0 && !path[start - 1].isPathSeparator()) return false
    return path.regionMatchesPath(start, suffix)
}

private fun pathGlobMatches(path: String, prefix: String): Boolean {
    val fileNameStart = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1
    val prefixStart = fileNameStart - prefix.length
    if (prefixStart < 0 || prefixStart > 0 && !path[prefixStart - 1].isPathSeparator()) return false
    return path.regionMatchesPath(prefixStart, prefix)
}

private fun String.regionMatchesPath(startIndex: Int, expected: String): Boolean {
    if (startIndex < 0 || startIndex + expected.length > length) return false
    for (index in expected.indices) {
        val actual = this[startIndex + index]
        val target = expected[index]
        if (actual.isPathSeparator() && target.isPathSeparator()) continue
        if (!actual.equals(target, ignoreCase = true)) return false
    }
    return true
}

private fun Char.isPathSeparator(): Boolean = this == '/' || this == '\\'
