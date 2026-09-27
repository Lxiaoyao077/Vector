package org.matrix.vector.manager.data.model

import android.content.pm.PackageInfo

/**
 * A package's version code.
 *
 * `PackageInfo.getLongVersionCode` is the only form that exists on a supported device: the `int`
 * field it replaces was deprecated in the same release that introduced it, and the high half it
 * drops (`versionCodeMajor`) cannot be set by a package installed on a release that has no concept
 * of it.
 */
val PackageInfo.versionCodeCompat: Long
    get() = longVersionCode
