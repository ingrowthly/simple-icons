package io.github.ingrowthly.icons.cores

/**
 * 文件夹 icon 关联
 */
data class FolderAssociation(
    val folderNames: String,
    val name: String,
    val folderColor: String,
    val folderIconColor: String,
    val priority: Int,
    val iconType: String,
    val pattern: String,
    val icon: String
)
