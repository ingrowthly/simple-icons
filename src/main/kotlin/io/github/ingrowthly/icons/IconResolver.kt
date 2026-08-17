package io.github.ingrowthly.icons

import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

internal object IconResolver {
    fun resolve(file: VirtualFile): Icon? {
        val name = file.name.lowercase()

        if (file.isDirectory) {
            return BinaryIconDictionary.resolveFolder(name)
        }

        return BinaryIconDictionary.resolveFile(name, file.path)
    }
}
