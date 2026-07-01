package org.matrix.vector.daemon.data

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.lsposed.lspd.models.Module
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConfigCacheRobolectricTest {

    private lateinit var dbPath: File
    private lateinit var testHelper: SQLiteOpenHelper
    private lateinit var fsMock: MockedStatic<FileSystem>

    @Before
    fun setUp() {
        // Create temp DB file
        dbPath = File.createTempFile("test_modules", ".db")
        dbPath.deleteOnExit()

        // Mock FileSystem
        fsMock = mockStatic(FileSystem::class.java)
        fsMock.`when`<Any> { FileSystem.dbPath }.thenReturn(dbPath)
        fsMock.`when`<Any> { FileSystem.toGlobalNamespace(anyString()) }
            .thenAnswer { inv -> File(inv.getArgument(0)) }

        // Replace ConfigCache.dbHelper with a test helper using temp path
        testHelper = object : SQLiteOpenHelper(null, dbPath.absolutePath, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS modules (
                        mid integer PRIMARY KEY AUTOINCREMENT,
                        module_pkg_name text NOT NULL UNIQUE,
                        apk_path text NOT NULL,
                        enabled BOOLEAN DEFAULT 0,
                        auto_include BOOLEAN DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scope (
                        mid integer,
                        app_pkg_name text NOT NULL,
                        user_id integer NOT NULL,
                        PRIMARY KEY (mid, app_pkg_name, user_id),
                        FOREIGN KEY (mid) REFERENCES modules (mid) ON DELETE CASCADE
                    )
                """)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {}
        }

        // Inject via reflection
        val field = ConfigCache::class.memberProperties
            .first { it.name == "dbHelper" }
            .apply { isAccessible = true }
        (field as kotlin.reflect.KMutableProperty<*>).setter.call(ConfigCache, testHelper)

        // Reset state
        val stateField = ConfigCache::class.memberProperties
            .first { it.name == "state" }
            .apply { isAccessible = true }
        (stateField as kotlin.reflect.KMutableProperty<*>).setter.call(ConfigCache, DaemonState())
    }

    @After
    fun tearDown() {
        fsMock.close()
        dbPath.delete()
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
    fun `ModuleLoadResult holds correct data`() {
        val mod = Module().apply { packageName = "com.test.mod" }
        val result = ConfigCache.ModuleLoadResult(
            mutableMapOf("com.test.mod" to mod),
            mutableSetOf("com.old"),
            mutableMapOf("com.moved" to "/data/app/moved/base.apk")
        )
        assertEquals(mod, result.modules["com.test.mod"])
        assertTrue("com.old" in result.obsoleteModules)
        assertEquals("/data/app/moved/base.apk", result.obsoletePaths["com.moved"])
    }

    // ── buildScopes ──

    @Test
    fun `buildScopes empty when no scopes in DB`() {
        val scopes = ConfigCache.buildScopes(emptyMap())
        assertTrue(scopes.isEmpty())
    }

    @Test
    fun `buildScopes skips modules not in newModules map`() {
        insertModule(1, "com.missing.module", "/fake/base.apk", enabled = true)
        insertScope(1, "com.some.app", userId = 0)

        val scopes = ConfigCache.buildScopes(emptyMap())
        assertTrue(scopes.isEmpty())
    }

    @Test
    fun `buildScopes maps system app to system_server scope`() {
        insertModule(1, "com.test.module", "/fake/base.apk", enabled = true)
        insertScope(1, "system", userId = 0)

        val modules = mapOf(
            "com.test.module" to Module().apply { packageName = "com.test.module" }
        )

        val scopes = ConfigCache.buildScopes(modules)
        assertEquals(1, scopes.size)
        val scope = ProcessScope("system_server", 1000)
        assertTrue(scope in scopes)
        assertEquals(1, scopes[scope]!!.size)
        assertEquals("com.test.module", scopes[scope]!!.single().packageName)
    }

    @Test
    fun `buildScopes handles multiple scopes per module`() {
        insertModule(2, "com.mod.a", "/fake/a.apk", enabled = true)
        insertScope(2, "system", userId = 0)
        insertScope(2, "com.app.b", userId = 0)

        val modules = mapOf(
            "com.mod.a" to Module().apply { packageName = "com.mod.a" }
        )

        val scopes = ConfigCache.buildScopes(modules)
        // 1 for system_server, but com.app.b requires PackageManager to resolve process
        assertTrue(ProcessScope("system_server", 1000) in scopes)
    }

    // ── helpers ──

    private fun insertModule(mid: Long, pkgName: String, apkPath: String, enabled: Boolean) {
        testHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO modules (mid, module_pkg_name, apk_path, enabled) VALUES (?, ?, ?, ?)",
            arrayOf(mid, pkgName, apkPath, if (enabled) 1 else 0)
        )
    }

    private fun insertScope(mid: Long, appPkg: String, userId: Int) {
        testHelper.writableDatabase.execSQL(
            "INSERT OR IGNORE INTO scope (mid, app_pkg_name, user_id) VALUES (?, ?, ?)",
            arrayOf(mid, appPkg, userId)
        )
    }
}
