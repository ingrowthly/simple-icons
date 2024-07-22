package io.github.ingrowthly.icons.decorators

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.ui.IconManager
import io.github.ingrowthly.icons.util.MatchUtil

class IconDecorator : ProjectViewNodeDecorator, DumbAware {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file: VirtualFile = node.virtualFile ?: return
        if (file.isDirectory) {
            val matchFolderIcon = MatchUtil.matchFolderIcon(file.name) ?: return
            val folderIcon =
                IconManager.getInstance().getIcon(matchFolderIcon, this::class.java.classLoader)
            data.setIcon(folderIcon)
        }
    }
}