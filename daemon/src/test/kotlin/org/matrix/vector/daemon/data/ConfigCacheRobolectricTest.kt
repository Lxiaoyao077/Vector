package org.matrix.vector.daemon.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.database.sqlite.SQLiteDatabase
import android.os.UserHandle
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.lsposed.lspd.models.Module
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplicationPackageManager
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConfigCacheRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SQLiteDatabase

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()

        // Create in-memory SQLite DB with the same schema as production
        db = SQLiteDatabase.create(null)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS modules (
                mid integer PRIMARY KEY AUTOINCREMENT,
                module_pkg_name text NOT NULL UNIQUE,
                apk_path text NOT NULL,
                enabled BOOLEAN DEFAULT 0 CHECK (enabled IN (0, 1)),
                auto_include BOOLEAN DEFAULT 0 CHECK (auto_include IN (0, 1))
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS scope (
                mid integer,
                app_pkg_name text NOT NULL,
                user_id integer NOT NULL,
                PRIMARY KEY (mid, app_pkg_name, user_id),
                CONSTRAINT scope_module_constraint
                    FOREIGN KEY (mid) REFERENCES modules (mid) ON DELETE CASCADE
            )
        """)

        // Reset ConfigCache state
        ConfigCache.state = DaemonState()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── ModuleLoadResult ──

    @Test
    fun `ModuleLoadResult empty state`() {
        val result = ConfigCache.ModuleLoadResult(mutableMapOf(), mutableSetOf(), mutableMapOf())
        assertTrue(result.modules.isEmpty())
        assertTrue(result.obsoleteModules.isEmpty())
        assertTrue(result.obsoletePaths.isEmpty())
    }

    @Test
    fun `ModuleLoadResult holds module data`() {
        val mod = Module().apply { packageName = "com.test.mod" }
        val result = ConfigCache.ModuleLoadResult(
            mutableMapOf("com.test.mod" to mod),
            mutableSetOf("com.removed"),
            mutableMapOf("com.moved" to "/data/app/moved/base.apk")
        )
        assertEquals(mod, result.modules["com.test.mod"])
        assertTrue("com.removed" in result.obsoleteModules)
        assertEquals("/data/app/moved/base.apk", result.obsoletePaths["com.moved"])
    }

    // ── buildScopes ──

    @Test
    fun `buildScopes returns empty when no scopes in DB`() {
        val modules = mapOf<String, Module>()
        val scopes = ConfigCache.buildScopes(modules)
        assertTrue(scopes.isEmpty())
    }

    @Test
    fun `buildScopes skips modules not in newModules`() {
        insertModule(1, "com.test.module", "/data/app/mod/base.apk", enabled = true)
        insertScope(1, "com.some.app", userId = 0)

        val scopes = ConfigCache.buildScopes(emptyMap())
        assertTrue(scopes.isEmpty()) // module not in newModules map
    }

    @Test
    fun `buildScopes maps system to system_server scope`() {
        insertModule(1, "com.test.module", "/data/app/mod/base.apk", enabled = true)
        insertScope(1, "system", userId = 0)

        val modules = mapOf(
            "com.test.module" to Module().apply {
                packageName = "com.test.module"
            }
        )

        val scopes = ConfigCache.buildScopes(modules)
        assertEquals(1, scopes.size)
        val key = ProcessScope("system_server", 1000)
        assertTrue(key in scopes)
        assertEquals("com.test.module", scopes[key]!!.single().packageName)
    }

    // ── helpers ──

    private fun insertModule(mid: Long, pkgName: String, apkPath: String, enabled: Boolean) {
        db.execSQL(
            "INSERT OR REPLACE INTO modules (mid, module_pkg_name, apk_path, enabled) VALUES (?, ?, ?, ?)",
            arrayOf(mid, pkgName, apkPath, if (enabled) 1 else 0)
        )
    }

    private fun insertScope(mid: Long, appPkg: String, userId: Int) {
        db.execSQL(
            "INSERT OR REPLACE INTO scope (mid, app_pkg_name, user_id) VALUES (?, ?, ?)",
            arrayOf(mid, appPkg, userId)
        )
    }
}
