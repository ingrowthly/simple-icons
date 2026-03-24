package io.github.ingrowthly.icons

import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

object IconResolver {

    fun resolve(file: VirtualFile): Icon? {
        val name = file.name.lowercase()
        val path = file.path.replace('\\', '/').lowercase()

        if (file.isDirectory) {
            return BinaryIconDictionary.resolveFolder(name)
        }

        return BinaryIconDictionary.resolveFile(name, path)
    }

}
