package io.github.ingrowthly.icons.gen

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class Wrapper(val value: Rule)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Rule(
    val fileNames: String? = null,
    val folderNames: String? = null,
    val icon: String,
    val priority: String? = "0",
)

private data class RuleMatch(val iconId: Int, val priority: Int)
private data class NameGlob(val prefix: String, val suffix: String, val pathAware: Boolean)

private class TrieNode {
    val children = sortedMapOf<Char, TrieNode>()
    var match: RuleMatch? = null
    var index: Int = -1
}

@CacheableTask
abstract class IconGenTask : DefaultTask() {
    companion object {
        private const val FILE_RULE_MAGIC = 0x53494631 // SIF1
        private const val NAME_MAP_MAGIC = 0x53494D31 // SIM1
        private const val TRIE_MAGIC = 0x53495431 // SIT1
        private const val NAME_GLOB_MAGIC = 0x53494E31 // SIN1
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val filesJson = project.objects.fileProperty()

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val foldersJson = project.objects.fileProperty()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val fileIconDir: DirectoryProperty = project.objects.directoryProperty()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val folderIconDir: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val kotlinOutputDir = project.objects.directoryProperty()

    @get:OutputDirectory
    val resourceOutputDir = project.objects.directoryProperty()

    @TaskAction
    fun run() {
        val mapper = jacksonObjectMapper()

        val fileRules: List<Wrapper> = mapper.readValue(filesJson.get().asFile)
        val folderRules: List<Wrapper> = mapper.readValue(foldersJson.get().asFile)

        val exactNameRules = mutableMapOf<String, RuleMatch>()
        val exactPathRules = mutableMapOf<String, RuleMatch>()
        val nameGlobRules = mutableMapOf<NameGlob, RuleMatch>()
        val folderMap = mutableMapOf<String, RuleMatch>()
        val extensionRules = mutableMapOf<String, RuleMatch>()
        val iconIds = linkedMapOf<String, Int>()
        val availableFileIcons = fileIconDir.get().asFile.listIconNames()
        val availableFolderIcons = folderIconDir.get().asFile.listIconNames()

        fun splitNames(raw: String?) =
            raw?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() } ?: emptyList()

        fun addExactRule(map: MutableMap<String, RuleMatch>, key: String, iconId: Int, priority: Int) {
            val existing = map[key]
            if (existing == null || priority > existing.priority) {
                map[key] = RuleMatch(iconId, priority)
            }
        }

        fun addGlobRule(map: MutableMap<NameGlob, RuleMatch>, key: NameGlob, iconId: Int, priority: Int) {
            val existing = map[key]
            if (existing == null || priority > existing.priority) {
                map[key] = RuleMatch(iconId, priority)
            }
        }

        fun parseGlob(raw: String): NameGlob? {
            val starIndex = raw.indexOf('*')
            if (starIndex == -1 || raw.indexOf('*', starIndex + 1) != -1) return null
            val glob = NameGlob(
                prefix = raw.substring(0, starIndex),
                suffix = raw.substring(starIndex + 1),
                pathAware = '/' in raw,
            )
            if (glob.suffix.isEmpty() || glob.pathAware && !glob.prefix.endsWith('/')) return null
            return glob
        }

        fun iconIdFor(rule: Rule, isFolder: Boolean): Int {
            val iconFile = rule.icon.substringAfterLast('/')
            val availableIcons = if (isFolder) availableFolderIcons else availableFileIcons
            if (iconFile !in availableIcons) {
                throw GradleException("Missing ${if (isFolder) "folder" else "file"} icon: $iconFile")
            }
            val path = if (isFolder) "/icons/folders/$iconFile" else "/icons/files/$iconFile"
            return iconIds.getOrPut(path) { iconIds.size }
        }

        fun priorityOf(rule: Rule): Int {
            val rawPriority = rule.priority ?: return 0
            return rawPriority.toIntOrNull()
                ?: throw GradleException("Invalid priority '$rawPriority' for icon ${rule.icon}")
        }

        fun processFileRule(rule: Rule) {
            val priority = priorityOf(rule)
            val iconId = iconIdFor(rule, isFolder = false)

            splitNames(rule.fileNames).forEach { raw ->
                when {
                    raw.startsWith("*.") -> {
                        addExactRule(extensionRules, raw.removePrefix("*"), iconId, priority)
                    }
                    '*' in raw -> {
                        val glob = parseGlob(raw)
                            ?: throw GradleException("Unsupported glob '$raw': expected exactly one '*' wildcard")
                        addGlobRule(nameGlobRules, glob, iconId, priority)
                    }
                    '/' in raw -> {
                        addExactRule(exactPathRules, raw, iconId, priority)
                    }
                    else -> {
                        addExactRule(exactNameRules, raw, iconId, priority)
                    }
                }
            }
        }

        fun processFolderRule(rule: Rule) {
            val priority = priorityOf(rule)
            val iconId = iconIdFor(rule, isFolder = true)
            splitNames(rule.folderNames).forEach { folderName ->
                val existing = folderMap[folderName]
                if (existing == null || priority >= existing.priority) {
                    folderMap[folderName] = RuleMatch(iconId, priority)
                }
            }
        }

        fileRules.forEach { processFileRule(it.value) }
        folderRules.forEach { processFolderRule(it.value) }

        val root = TrieNode()
        extensionRules.entries
            .sortedBy { it.key }
            .forEach { (extension, match) ->
                var node = root
                for (char in extension.reversed()) {
                    node = node.children.computeIfAbsent(char) { TrieNode() }
                }
                val existing = node.match
                if (existing == null || match.priority > existing.priority) {
                    node.match = match
                }
            }

        val sortedIconPaths = iconIds.entries.sortedBy { it.value }.map { it.key }
        val iconsCode = buildString {
            appendLine("package io.github.ingrowthly.icons.generated")
            appendLine("import com.intellij.openapi.util.IconLoader")
            appendLine("import java.util.concurrent.atomic.AtomicReferenceArray")
            appendLine("import javax.swing.Icon")
            appendLine("object GeneratedIcons {")
            appendLine("    private val PATHS = arrayOf(")
            sortedIconPaths.forEach { path -> appendLine("""        "$path",""") }
            appendLine("    )")
            appendLine()
            appendLine("    private val cache = AtomicReferenceArray<Icon?>(PATHS.size)")
            appendLine()
            appendLine("    fun get(iconId: Int): Icon {")
            appendLine("        cache.get(iconId)?.let { return it }")
            appendLine("        val loaded = IconLoader.getIcon(PATHS[iconId], GeneratedIcons::class.java)")
            appendLine("        if (cache.compareAndSet(iconId, null, loaded)) return loaded")
            appendLine("        return cache.get(iconId) ?: loaded")
            appendLine("    }")
            appendLine("}")
        }

        fun writeExactRules(file: File, entries: List<Pair<String, RuleMatch>>) {
            file.parentFile.mkdirs()
            DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
                output.writeInt(FILE_RULE_MAGIC)
                output.writeInt(entries.size)
                entries.forEach { (key, match) ->
                    val bytes = key.toByteArray(Charsets.UTF_8)
                    require(bytes.size <= 0xFFFF) { "Rule key too long: $key" }
                    output.writeShort(bytes.size)
                    output.write(bytes)
                    output.writeInt(match.iconId)
                    output.writeInt(match.priority)
                }
            }
        }

        fun writeFolderMap(file: File, entries: List<Pair<String, Int>>) {
            file.parentFile.mkdirs()
            DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
                output.writeInt(NAME_MAP_MAGIC)
                output.writeInt(entries.size)
                entries.forEach { (name, iconId) ->
                    val bytes = name.toByteArray(Charsets.UTF_8)
                    require(bytes.size <= 0xFFFF) { "Folder rule too long: $name" }
                    output.writeShort(bytes.size)
                    output.write(bytes)
                    output.writeInt(iconId)
                }
            }
        }

        fun writeNameGlobs(file: File, entries: List<Pair<NameGlob, RuleMatch>>) {
            file.parentFile.mkdirs()
            DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
                output.writeInt(NAME_GLOB_MAGIC)
                output.writeInt(entries.size)
                entries.forEach { (glob, match) ->
                    val prefixBytes = glob.prefix.toByteArray(Charsets.UTF_8)
                    val suffixBytes = glob.suffix.toByteArray(Charsets.UTF_8)
                    require(prefixBytes.size <= 0xFFFF) { "Glob prefix too long: ${glob.prefix}" }
                    require(suffixBytes.size <= 0xFFFF) { "Glob suffix too long: ${glob.suffix}" }
                    output.writeShort(prefixBytes.size)
                    output.write(prefixBytes)
                    output.writeShort(suffixBytes.size)
                    output.write(suffixBytes)
                    output.writeBoolean(glob.pathAware)
                    output.writeInt(match.iconId)
                    output.writeInt(match.priority)
                }
            }
        }

        fun writeExtensionTrie(file: File, rootNode: TrieNode) {
            val nodes = mutableListOf<TrieNode>()

            fun collect(node: TrieNode) {
                if (node.index != -1) return
                node.index = nodes.size
                nodes += node
                node.children.values.forEach(::collect)
            }

            collect(rootNode)

            file.parentFile.mkdirs()
            DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
                output.writeInt(TRIE_MAGIC)
                output.writeInt(nodes.size)
                output.writeInt(nodes.sumOf { it.children.size })

                var edgeStart = 0
                nodes.forEach { node ->
                    output.writeInt(node.match?.iconId ?: -1)
                    output.writeInt(node.match?.priority ?: Int.MIN_VALUE)
                    output.writeInt(edgeStart)
                    output.writeInt(node.children.size)
                    edgeStart += node.children.size
                }

                nodes.forEach { node ->
                    node.children.forEach { (char, child) ->
                        output.writeChar(char.code)
                        output.writeInt(child.index)
                    }
                }
            }
        }

        val kotlinOut = kotlinOutputDir.get().asFile
        kotlinOut.mkdirs()
        File(kotlinOut, "GeneratedIcons.kt").writeText(iconsCode)

        val resourceOut = resourceOutputDir.get().asFile
        writeExactRules(
            File(resourceOut, "icons/generated/file-names.bin"),
            exactNameRules.entries.sortedBy { it.key }.map { it.key to it.value },
        )
        writeExactRules(
            File(resourceOut, "icons/generated/file-paths.bin"),
            exactPathRules.entries.sortedByDescending { it.value.priority }.map { it.key to it.value },
        )
        writeNameGlobs(
            File(resourceOut, "icons/generated/file-name-globs.bin"),
            nameGlobRules.entries.sortedByDescending { it.value.priority }.map { it.key to it.value },
        )
        writeFolderMap(
            File(resourceOut, "icons/generated/folder-names.bin"),
            folderMap.entries.sortedBy { it.key }.map { it.key to it.value.iconId },
        )
        writeExtensionTrie(File(resourceOut, "icons/generated/extension-trie.bin"), root)
    }

    private fun File.listIconNames(): Set<String> =
        listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == "svg" }
            ?.mapTo(mutableSetOf()) { it.name }
            ?: throw GradleException("Icon directory does not exist: $this")
}
