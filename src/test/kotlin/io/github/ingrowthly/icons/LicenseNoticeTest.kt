package io.github.ingrowthly.icons

import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseNoticeTest {
    @Test
    fun licenseNoticesArePackaged() {
        assertResourceContains("META-INF/LICENSE.txt", "Copyright (c) 2024-2026 ingrowthly")
        assertResourceContains(
            "META-INF/THIRD_PARTY_NOTICES.txt",
            "Copyright (c) 2017-2019 Elior Boukhobza",
        )
        assertResourceContains(
            "META-INF/THIRD_PARTY_NOTICES.txt",
            "Copyright (c) 2017-2022 Elior \"Mallowigi\" Boukhobza",
        )
    }

    private fun assertResourceContains(path: String, expected: String) {
        val content = requireNotNull(javaClass.classLoader.getResourceAsStream(path)) {
            "Missing packaged resource: $path"
        }.bufferedReader().use { it.readText() }

        assertTrue("Expected $path to contain: $expected", content.contains(expected))
    }
}
