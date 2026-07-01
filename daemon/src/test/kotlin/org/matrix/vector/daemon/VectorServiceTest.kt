package org.matrix.vector.daemon

import org.junit.Assert.*
import org.junit.Test

class VectorServiceTest {

    @Test
    fun `isXposedModule returns false for null moduleName`() {
        assertFalse(VectorService.isXposedModule(null))
    }

    @Test
    fun `isXposedModule returns false for empty moduleName`() {
        assertFalse(VectorService.isXposedModule(""))
    }

    @Test
    fun `isXposedModule returns false for non-existent package`() {
        // Package manager returns null for unknown packages
        assertFalse(VectorService.isXposedModule("com.nonexistent.module"))
    }

    @Test
    fun `handleFullyRemoved no-ops on null moduleName`() {
        // Should not throw
        VectorService.handleFullyRemoved(null, 0, false)
    }

    @Test
    fun `handleFullyRemoved no-ops on null moduleName with allUsers`() {
        VectorService.handleFullyRemoved(null, 0, true)
    }

    @Test
    fun `autoIncludeModule no-ops when no auto-include modules configured`() {
        // Should not throw when getAutoIncludeModules returns empty
        VectorService.autoIncludeModule("com.some.app", 0)
    }
}
