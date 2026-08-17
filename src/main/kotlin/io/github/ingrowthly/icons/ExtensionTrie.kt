package io.github.ingrowthly.icons

internal class ExtensionTrie(
    private val iconIds: IntArray,
    private val priorities: IntArray,
    private val edgeStarts: IntArray,
    private val edgeCounts: IntArray,
    private val edgeChars: CharArray,
    private val edgeTargets: IntArray,
) {
    fun matchNode(name: String): Int {
        var nodeIndex = 0
        var bestNode = NO_MATCH
        var bestPriority = Int.MIN_VALUE
        var bestLength = -1

        for (index in name.lastIndex downTo 0) {
            val edgeIndex = findEdge(nodeIndex, name[index])
            if (edgeIndex == NO_MATCH) break
            nodeIndex = edgeTargets[edgeIndex]

            val iconId = iconIds[nodeIndex]
            val length = name.length - index
            val priority = priorities[nodeIndex]
            if (iconId >= 0 && (priority > bestPriority || priority == bestPriority && length > bestLength)) {
                bestNode = nodeIndex
                bestPriority = priority
                bestLength = length
            }
        }

        return bestNode
    }

    fun iconIdAt(nodeIndex: Int): Int = iconIds[nodeIndex]

    fun priorityAt(nodeIndex: Int): Int = priorities[nodeIndex]

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

    internal companion object {
        private const val NO_MATCH = -1

        fun fromExtensions(entries: List<Pair<String, RuleMatch>>): ExtensionTrie {
            class Node {
                val children = sortedMapOf<Char, Node>()
                var match: RuleMatch? = null
                var index: Int = -1
            }

            val root = Node()
            entries.forEach { (extension, match) ->
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
