package com.kleos.sakshi.arch

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The manifest is the promise (DOC 2 §2.6.1). Scans the merged release manifest, library manifests included. */
class ReleaseManifestTest {
    private val manifest: String = File(System.getProperty("merged.release.manifest")).also {
        assertTrue("merged release manifest not found at ${it.absolutePath}", it.isFile)
    }.readText()

    private val permissions: List<String> =
        Regex("""<uses-permission[^>]*android:name="([^"]+)"""").findAll(manifest).map { it.groupValues[1] }.toList()

    private fun forbidden(vararg prefixes: String) = permissions.filter { p -> prefixes.any { p.startsWith(it) } }

    @Test
    fun noInternet() = assertEquals(emptyList<String>(), forbidden("android.permission.INTERNET"))

    @Test
    fun noOverlayBatteryExemptionOrAllPackages() = assertEquals(
        emptyList<String>(),
        forbidden(
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.QUERY_ALL_PACKAGES"))

    @Test
    fun noForegroundServicePermissionOrService() {
        assertEquals(emptyList<String>(), forbidden("android.permission.FOREGROUND_SERVICE"))
        assertTrue("a foreground service is declared", !manifest.contains("SystemForegroundService"))
        assertTrue("a foregroundServiceType is declared", !manifest.contains("foregroundServiceType"))
    }

    @Test
    fun noAccessibilityOrDeviceAdmin() {
        assertTrue(!manifest.contains("BIND_ACCESSIBILITY_SERVICE"))
        assertTrue(!manifest.contains("android.accessibilityservice.AccessibilityService"))
        assertTrue(!manifest.contains("BIND_DEVICE_ADMIN"))
        assertTrue(!manifest.contains("android.app.action.DEVICE_ADMIN_ENABLED"))
    }

    @Test
    fun noContactsLocationCameraMicrophoneOrSms() = assertEquals(
        emptyList<String>(),
        forbidden(
            "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS",
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
            "android.permission.READ_SMS", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS"))

    @Test
    fun declaresTheDocumentedPermissionsAndTheListenerService() {
        assertTrue(permissions.contains("android.permission.PACKAGE_USAGE_STATS"))
        assertTrue(permissions.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifest.contains("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"))
        assertTrue(manifest.contains("com.kleos.sakshi.host.NotificationCollector"))
    }

    @Test
    fun backupIsOffAndExtractionRulesExcludeEverything() {
        assertTrue(manifest.contains("""android:allowBackup="false""""))
        assertTrue(manifest.contains("@xml/data_extraction_rules"))
    }
}
