package org.matrix.vector

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import org.lsposed.lspd.util.Utils
import org.matrix.vector.impl.hookers.HandleSystemServerProcessHooker
import org.matrix.vector.impl.hooks.VectorHookBuilder
import io.github.libxposed.api.XposedInterface
import org.matrix.vector.service.BridgeService

/**
 * Handles System-Server side logic for the Parasitic Manager.
 *
 * When a user tries to open the Vector Manager, the system normally wouldn't know how to handle it
 * because it isn't "installed." This class intercepts the activity resolution and tells the system
 * to launch it in a special process.
 */
class ParasiticManagerSystemHooker : HandleSystemServerProcessHooker.Callback {

    companion object {
        @JvmStatic
        fun start() {
            // Register this class as the handler for system_server initialization.
            // This ensures the hook is deferred until the System Server ClassLoader is fully ready.
            HandleSystemServerProcessHooker.callback = ParasiticManagerSystemHooker()
        }
    }

    @SuppressLint("PrivateApi")
    override fun onSystemServerLoaded(classLoader: ClassLoader) {
        runCatching {
                val supervisorClass = resolveSupervisorClass(classLoader)
                val resolveMethod =
                    supervisorClass.declaredMethods.first { it.name == "resolveActivity" }

                VectorHookBuilder(resolveMethod).intercept { chain ->
                    redirectManagerActivity(chain)
                }

                Utils.logD("Successfully hooked Activity Supervisor for Manager redirection.")
            }
            .onFailure { Utils.logE("Failed to hook system server activity resolution", it) }
    }

    /**
     * Resolve the Activity supervisor class across Android 8.1–14+.
     * Class names changed per Android release: am (8-9) → wm (10-11) → wm renamed (12+).
     */
    private fun resolveSupervisorClass(classLoader: ClassLoader): Class<*> {
        return try {
            // Android 12.0 - 14+
            Class.forName("com.android.server.wm.ActivityTaskSupervisor", false, classLoader)
        } catch (e: ClassNotFoundException) {
            try {
                // Android 10 - 11
                Class.forName("com.android.server.wm.ActivityStackSupervisor", false, classLoader)
            } catch (e2: ClassNotFoundException) {
                // Android 8.1 - 9
                Class.forName("com.android.server.am.ActivityStackSupervisor", false, classLoader)
            }
        }
    }

    /** Intercept resolveActivity to redirect the Vector Manager to its dedicated process. */
    private fun redirectManagerActivity(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        val intent = chain.args[0] as? Intent ?: return result

        if (!intent.hasCategory(BuildConfig.ManagerPackageName + ".LAUNCH_MANAGER"))
            return result

        val originalInfo =
            result as? ActivityInfo
                ?: run {
                    Utils.logD(
                        "Redirection: result is not ActivityInfo (was ${result?.javaClass?.name})")
                    return result
                }

        if (originalInfo.packageName != BuildConfig.InjectedPackageName) return result

        val redirectedInfo = buildRedirectedActivityInfo(originalInfo)
        BridgeService.getService()?.preStartManager()
        return redirectedInfo
    }

    private fun buildRedirectedActivityInfo(original: ActivityInfo): ActivityInfo {
        return ActivityInfo(original).apply {
            processName = BuildConfig.ManagerPackageName
            theme = android.R.style.Theme_DeviceDefault_Settings
            flags =
                flags and
                    (ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS or
                            ActivityInfo.FLAG_FINISH_ON_CLOSE_SYSTEM_DIALOGS)
                        .inv()
        }
    }
}
