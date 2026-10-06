package com.kleos.sakshi.host

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.telecom.TelecomManager
import com.kleos.sakshi.engine.model.AppInfo
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.ports.AppCatalog

/**
 * What Android says about installed apps. It lists and looks up; it classifies nothing.
 * Launcher apps come through the `<queries>` block, not QUERY_ALL_PACKAGES. Label lookup is slow, so callers run
 * `launcherApps()` off the main thread (HostApiImpl does) and the result is kept for later calls.
 */
class AppCatalogImpl(private val context: Context, private val neutral: NeutralPackages) : AppCatalog {
    private val pm: PackageManager get() = context.packageManager

    @Volatile private var cachedLauncherApps: List<AppInfo>? = null

    override fun launcherApps(): List<AppInfo> = cachedLauncherApps ?: loadLauncherApps().also { cachedLauncherApps = it }

    /** Call after an install or uninstall to re-read the list. */
    fun refresh() { cachedLauncherApps = null }

    private fun loadLauncherApps(): List<AppInfo> {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(launcher, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .filter { it != context.packageName }   // Sakshi is never a work-set choice; the engine treats its own package as neutral
            .map { AppInfo(Pkg(it), labelOf(it), category(Pkg(it))) }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * The app's name, or the package name itself when Android will not say (an app seen in usage events but not visible
     * through `<queries>`, a work-profile app, one uninstalled since).
     */
    fun labelOf(pkg: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString().ifBlank { pkg }
    } catch (_: Exception) {
        pkg
    }

    /** ApplicationInfo.category as Android reports it (API 26+), or null when undefined or not visible. */
    override fun category(pkg: Pkg): Int? = try {
        pm.getApplicationInfo(pkg.value, 0).category.takeIf { it != ApplicationInfo.CATEGORY_UNDEFINED }
    } catch (_: Exception) {
        null
    }

    override fun isNeutral(pkg: Pkg): Boolean =
        neutral.contains(pkg.value) || pkg.value == defaultDialer() || pkg.value == defaultLauncher()

    override fun ownPackage(): Pkg = Pkg(context.packageName)

    private fun defaultDialer(): String? = try {
        (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
    } catch (_: Exception) {
        null
    }

    private fun defaultLauncher(): String? = try {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            ?.takeIf { it != "android" }   // "android" is the chooser, meaning no default is set
    } catch (_: Exception) {
        null
    }

    companion object {
        fun create(context: Context): AppCatalogImpl {
            val json = try {
                context.assets.open("sakshi/neutral_packages.json").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                null
            }
            return AppCatalogImpl(context.applicationContext, NeutralPackages.parse(json))
        }
    }
}
