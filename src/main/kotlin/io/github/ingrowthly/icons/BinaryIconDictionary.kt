package io.github.ingrowthly.icons

import io.github.ingrowthly.icons.generated.GeneratedIcons
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.nio.charset.StandardCharsets
import javax.swing.Icon

object BinaryIconDictionary {
    private const val FILE_RULE_MAGIC = 0x53494631 // SIF1
    private const val NAME_MAP_MAGIC = 0x53494D31 // SIM1
    private const val TRIE_MAGIC = 0x53495431 // SIT1
    private const val NAME_GLOB_MAGIC = 0x53494E31 // SIN1

    private val fileRuleResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        FileRuleResolver(
            exactNameRules = loadRuleMap("/icons/generated/file-names.bin"),
            exactPathRules = loadPathRules("/icons/generated/file-paths.bin"),
            nameGlobRules = loadNameGlobs("/icons/generated/file-name-globs.bin"),
            extensionTrie = loadExtensionTrie("/icons/generated/extension-trie.bin"),
        )
    }

    private val folderNameMap by lazy(LazyThreadSafetyMode.PUBLICATION) {
        loadNameMap("/icons/generated/folder-names.bin")
    }

    fun resolveFile(name: String, path: String): Icon? {
        val iconId = fileRuleResolver.resolveFile(name, path) ?: return null
        return GeneratedIcons.get(iconId)
    }

    fun resolveFolder(name: String): Icon? {
        val iconId = folderNameMap[name] ?: return null
        return GeneratedIcons.get(iconId)
    }

    private fun loadNameMap(path: String): Map<String, Int> {
        val input = openStream(path)
        DataInputStream(BufferedInputStream(input)).use { stream ->
            require(stream.readInt() == NAME_MAP_MAGIC) { "Invalid dictionary: $path" }
            val entryCount = stream.readInt()
            val map = HashMap<String, Int>(entryCount * 4 / 3 + 1)
            repeat(entryCount) {
                val name = stream.readUtf8()
                map[name] = stream.readInt()
            }
            return map
        }
    }

    private fun loadRuleMap(path: String): Map<String, RuleMatch> {
        val input = openStream(path)
        DataInputStream(BufferedInputStream(input)).use { stream ->
            require(stream.readInt() == FILE_RULE_MAGIC) { "Invalid rule map: $path" }
            val entryCount = stream.readInt()
            val map = HashMap<String, RuleMatch>(entryCount * 4 / 3 + 1)
            repeat(entryCount) {
                val key = stream.readUtf8()
                map[key] = RuleMatch(
                    iconId = stream.readInt(),
                    priority = stream.readInt(),
                )
            }
            return map
        }
    }

    private fun loadPathRules(path: String): List<Pair<String, RuleMatch>> {
        val input = openStream(path)
        DataInputStream(BufferedInputStream(input)).use { stream ->
            require(stream.readInt() == FILE_RULE_MAGIC) { "Invalid path rule map: $path" }
            val entryCount = stream.readInt()
            return List(entryCount) {
                stream.readUtf8() to RuleMatch(
                    iconId = stream.readInt(),
                    priority = stream.readInt(),
                )
            }
        }
    }

    private fun loadNameGlobs(path: String): List<NameGlobRule> {
        val input = openStream(path)
        DataInputStream(BufferedInputStream(input)).use { stream ->
            require(stream.readInt() == NAME_GLOB_MAGIC) { "Invalid name globs: $path" }
            val entryCount = stream.readInt()
            return List(entryCount) {
                NameGlobRule(
                    prefix = stream.readUtf8(),
                    suffix = stream.readUtf8(),
                    pathAware = stream.readBoolean(),
                    match = RuleMatch(
                        iconId = stream.readInt(),
                        priority = stream.readInt(),
                    ),
                )
            }
        }
    }

    private fun loadExtensionTrie(path: String): ExtensionTrie {
        val input = openStream(path)
        DataInputStream(BufferedInputStream(input)).use { stream ->
            require(stream.readInt() == TRIE_MAGIC) { "Invalid extension trie: $path" }
            val nodeCount = stream.readInt()
            val edgeCount = stream.readInt()

            val iconIds = IntArray(nodeCount)
            val priorities = IntArray(nodeCount)
            val edgeStarts = IntArray(nodeCount)
            val edgeCounts = IntArray(nodeCount)

            repeat(nodeCount) { index ->
                iconIds[index] = stream.readInt()
                priorities[index] = stream.readInt()
                edgeStarts[index] = stream.readInt()
                edgeCounts[index] = stream.readInt()
            }

            val edgeChars = CharArray(edgeCount)
            val edgeTargets = IntArray(edgeCount)

            repeat(edgeCount) { index ->
                edgeChars[index] = stream.readChar()
                edgeTargets[index] = stream.readInt()
            }

            return ExtensionTrie(
                iconIds = iconIds,
                priorities = priorities,
                edgeStarts = edgeStarts,
                edgeCounts = edgeCounts,
                edgeChars = edgeChars,
                edgeTargets = edgeTargets,
            )
        }
    }

    private fun openStream(path: String) =
        BinaryIconDictionary::class.java.getResourceAsStream(path)
            ?: error("Missing binary icon dictionary: $path")

    private fun DataInputStream.readUtf8(): String {
        val size = readUnsignedShort()
        val bytes = ByteArray(size)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
