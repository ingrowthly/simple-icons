package io.github.ingrowthly.icons.util

import java.io.InputStream
import java.nio.charset.StandardCharsets

object FileUtil {

    /**
     * 读取文件
     */
    fun readResourceFile(filename: String): String {
        val resourceStream: InputStream? = this.javaClass.classLoader.getResourceAsStream(filename)
        return resourceStream?.bufferedReader(StandardCharsets.UTF_8).use { it?.readText() }
            ?: throw IllegalStateException("File not found.")
    }
}