package io.github.ingrowthly.icons

import com.intellij.CodeStyleBundle.BUNDLE
import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

object MyBundle : DynamicBundle(BUNDLE) {

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}