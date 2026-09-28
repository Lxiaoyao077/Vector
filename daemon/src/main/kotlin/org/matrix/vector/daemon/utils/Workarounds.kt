package org.matrix.vector.daemon.utils

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.UserInfo
import android.os.Build
import android.os.IBinder
import android.os.IUserManager
import android.util.Log
import java.lang.ClassNotFoundException
import org.matrix.vector.daemon.system.*

private const val TAG = "VectorWorkarounds"
private val isLenovo = Build.MANUFACTURER.equals("lenovo", ignoreCase = true)
private val isXiaomi = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)

fun IUserManager.getRealUsers(): List<UserInfo> {
  val users =
      runCatching { getUsers(true, true, true) }
          .recoverCatching { t -> if (t is NoSuchMethodError) getUsers(true) else throw t }
          .onFailure { Log.e(TAG, "All user retrieval attempts failed", it) }
          .getOrDefault(emptyList())
          .toMutableList()

  if (isLenovo) {
    val existingIds = users.map { it.id }.toSet()
    for (i in 900..909) {
      if (i !in existingIds) {
        runCatching { getUserInfo(i) }
            .onFailure { Log.e(TAG, "Failed to apply Lenovo's app cloning workaround", it) }
            .getOrNull()
            ?.let { users.add(it) }
      }
    }
  }
  return users
}

/**
 * Notification.Builder workaround for the SystemUI feature flags.
 *
 * Android 16 reads an aconfig flag from the `systemui` container while constructing a
 * `Notification`, and the generated `FeatureFlagsImpl` only reads it once, guarded by
 * `systemui_is_cached`. The read goes out to `content://settings/config`, which the daemon cannot
 * reach: it has an ActivityThread but no application record, so the system refuses it a provider
 * and the constructor throws `SecurityException`. Setting the guard leaves the flags at their
 * defaults and skips the read entirely.
 *
 * The read belongs to Android 16 and is gone again in Android 17, where the constructor sets the
 * fields unconditionally. It reaches Android 15 all the same, on vendor builds that took the change
 * without taking the SDK level: #96 and #880 are both Xiaomi HyperOS on 15. The test therefore
 * spans the versions where the field can exist rather than naming one, and a device that never had
 * it fails with a `ClassNotFoundException` that is ignored.
 */
fun applyNotificationWorkaround() {
  if (Build.VERSION.SDK_INT in Build.VERSION_CODES.VANILLA_ICE_CREAM..36) {
    runCatching {
          val feature = Class.forName("android.app.FeatureFlagsImpl")
          val field = feature.getDeclaredField("systemui_is_cached").apply { isAccessible = true }
          field.set(null, true)
        }
        .onFailure {
          if (it !is ClassNotFoundException)
              Log.e(TAG, "Failed to bypass systemui_is_cached flag", it)
        }
  }

  runCatching { Notification.Builder(FakeContext(), "notification_workaround").build() }
      .onFailure {
        if (it is AbstractMethodError) {
          FakeContext.nullProvider = !FakeContext.nullProvider
        } else {
          Log.e(TAG, "Failed to build dummy notification", it)
        }
      }
}

/**
 * The daemon's own connection for the X-space bind. Its callbacks are empty on purpose - the
 * point of the bind is its side effect, keeping MIUI's securitycore process and the X-space
 * service inside it alive - and it is never unbound: the service is meant to live as long as
 * the daemon does. Both callback shapes are declared because system_server dispatches the
 * three-argument one up to Android 16 and the four-argument one from Android 17.
 */
private val xspaceConnection =
    object : android.app.IServiceConnection.Stub() {
      override fun connected(name: ComponentName?, service: IBinder?, dead: Boolean) {}

      override fun connected(
          name: ComponentName?,
          service: IBinder?,
          session: android.app.IBinderSession?,
          dead: Boolean
      ) {}
    }

/**
 * Keeps MIUI's X-space service bound for the daemon's whole life.
 *
 * Binding [com.miui.securitycore]'s `XSpaceService` with BIND_AUTO_CREATE is what keeps the
 * process that runs second-space applications alive and injectable. The bind used to hang off
 * the manager's guard and was unbound the moment the manager's binder died - which for a
 * standalone manager is routine - and MIUI answered losing the service by re-sealing the second
 * space: the phone dropped back into its unlock, re-entered the space, and refused the password.
 * The service therefore belongs to the daemon now, which outlives any manager session.
 */
fun applyXspaceWorkaround() {
  if (isXiaomi) {
    val intent =
        Intent().apply {
          component =
              ComponentName.unflattenFromString(
                  "com.miui.securitycore/com.miui.xspace.service.XSpaceService")
        }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      activityManager?.bindService(
          SystemContext.appThread,
          SystemContext.token,
          intent,
          intent.type,
          xspaceConnection,
          Context.BIND_AUTO_CREATE.toLong(),
          "android",
          0)
    } else {
      activityManager?.bindService(
          SystemContext.appThread,
          SystemContext.token,
          intent,
          intent.type,
          xspaceConnection,
          Context.BIND_AUTO_CREATE,
          "android",
          0)
    }
  }
}

fun applySqliteHelperWorkaround() {
  // OnePlus compare current package with BenchAppList to decide sync mode
  runCatching {
        val globalClass = Class.forName("android.database.sqlite.SQLiteGlobal")
        val syncModeField = globalClass.getDeclaredField("sDefaultSyncMode")
        syncModeField.isAccessible = true

        // Prevents from calling getPkgs()
        if (syncModeField.get(null) == null) {
          syncModeField.set(null, "NORMAL")
          Log.i(TAG, "SQLiteGlobal.sDefaultSyncMode initialized to NORMAL.")
        }
      }
      .onFailure { Log.v(TAG, "SQLiteGlobal workaround not applied: ${it.message}") }

  // Fix AOSP Settings.Global dependency (API 28+ but not recent Android versions)
  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
    runCatching {
          val walClass = Class.forName("android.database.sqlite.SQLiteCompatibilityWalFlags")

          // Mark as initialized so initIfNeeded() returns immediately
          walClass.getDeclaredField("sInitialized").apply {
            isAccessible = true
            set(null, true)
          }

          // Mark as 'Currently Calling' as a secondary recursion guard
          walClass.getDeclaredField("sCallingGlobalSettings").apply {
            isAccessible = true
            set(null, true)
          }
          Log.i(TAG, "SQLiteCompatibilityWalFlags successfully bypassed.")
        }
        .onFailure { Log.v(TAG, "Could not disable SQLiteCompatibilityWalFlags: ${it.message}") }
  }
}
