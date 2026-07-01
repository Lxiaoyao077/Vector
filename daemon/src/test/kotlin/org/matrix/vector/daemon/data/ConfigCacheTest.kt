package org.matrix.vector.daemon.data

import org.junit.Assert.*
import org.junit.Test
import org.lsposed.lspd.models.Module

class ConfigCacheTest {

    @Test
    fun `canReuseOldModule returns false when oldModule is null`() {
        assertFalse(ConfigCache.canReuseOldModule(null, null, null))
    }

    @Test
    fun `canReuseOldModule returns false when appInfo sourceDir is null`() {
        val oldModule = Module().apply { apkPath = "/data/app/test/base.apk" }
        assertFalse(ConfigCache.canReuseOldModule(oldModule, null, "/data/app/test/base.apk"))
    }

    @Test
    fun `canReuseOldModule returns false when apkPath param is null`() {
        val oldModule = Module().apply { apkPath = "/data/app/test/base.apk" }
        assertFalse(ConfigCache.canReuseOldModule(oldModule, null, null))
    }

    @Test
    fun `canReuseOldModule returns false when oldModule apkPath is null`() {
        val oldModule = Module()
        assertFalse(ConfigCache.canReuseOldModule(oldModule, null, "/data/app/test/base.apk"))
    }

    @Test
    fun `canReuseOldModule returns false when apkPath differs from oldModule apkPath`() {
        val oldModule = Module().apply { apkPath = "/data/app/test/base.apk" }
        // apkPath param differs — cannot reuse
        assertFalse(
            ConfigCache.canReuseOldModule(oldModule, null, "/data/app/test-other/base.apk"))
    }

    @Test
    fun `ModuleLoadResult empty collections by default`() {
        val result = ConfigCache.ModuleLoadResult(
            mutableMapOf(), mutableSetOf(), mutableMapOf())
        assertTrue(result.modules.isEmpty())
        assertTrue(result.obsoleteModules.isEmpty())
        assertTrue(result.obsoletePaths.isEmpty())
    }

    @Test
    fun `ModuleLoadResult holds correct values`() {
        val mod = Module().apply { packageName = "com.test" }
        val result = ConfigCache.ModuleLoadResult(
            mutableMapOf("com.test" to mod),
            mutableSetOf("com.old"),
            mutableMapOf("com.moved" to "/data/app/com.moved/base.apk"))
        assertEquals(mod, result.modules["com.test"])
        assertTrue(result.obsoleteModules.contains("com.old"))
        assertEquals("/data/app/com.moved/base.apk", result.obsoletePaths["com.moved"])
    }
}
