package io.github.ingrowthly.icons.services

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import io.github.ingrowthly.icons.cores.FolderAssociation
import io.github.ingrowthly.icons.util.FileUtil.readResourceFile

@Service(Service.Level.APP)
class FolderAssociationService() {

    private var folderAssociations: List<FolderAssociation> = emptyList()
    private val gson = Gson()
    private var folderAssociationMap: Map<String, String> = emptyMap()

    init {
        loadConfiguration()
        folderAssociationMap = folderAssociations.associate { it.icon to it.pattern }
    }


    private fun loadConfiguration() {
        val jsonString = readResourceFile("folder_associations.json")
        folderAssociations =
            gson.fromJson(jsonString, Array<FolderAssociation>::class.java).toList()
    }

    companion object {
        @JvmStatic
        fun getFolderAssociationMap(): Map<String, String> {
            return FolderAssociationService().folderAssociationMap
        }
    }
}
