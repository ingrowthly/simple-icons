package io.github.ingrowthly.icons.util

import io.github.ingrowthly.icons.services.FolderAssociationService.Companion.getFolderAssociationMap

object MatchUtil {

    private val folderRegexMap = getFolderAssociationMap().mapValues { it.value.toRegex() }

    /**
     * 匹配文件夹 icon
     */
    fun matchFolderIcon(folderName: String): String? {
        folderRegexMap.forEach { (icon, regex) ->
            if (regex.matches(folderName)) {
                return icon
            }
        }
        return null
    }
}