package com.kleos.sakshi.host

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.service.notification.NotificationListenerService

/**
 * Reads real system state every time it is asked; never trusts a boolean returned by a settings page (DOC 2 §2.6.2).
 * No attention logic here: it answers "is this granted" and opens Android's own pages.
 */
class PermissionGateway(private val context: Context) {
    // In memory on purpose: it only has to survive the trip to the settings page and back.
    @Volatile private var notificationSettingsOpened = false

    fun usageAccessGranted(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun notificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val mine = ComponentName(context, NotificationCollector::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    fun canPostNotifications(): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && manager.areNotificationsEnabled()
    }

    /** True when the user was sent to the notification-access page and access is still not granted on return. */
    fun restrictedSettingsSuspected(): Boolean {
        val granted = notificationListenerEnabled()
        if (granted) notificationSettingsOpened = false
        return restrictedSuspected(notificationSettingsOpened, granted)
    }

    /** Access is granted but the system has not bound the listener (an OEM killed it): ask it to reconnect. Called on app open. */
    fun requestRebindIfNeeded() {
        if (notificationListenerEnabled() && !NotificationCollector.bound) {
            try {
                NotificationListenerService.requestRebind(NotificationCollector.componentName(context))
            } catch (_: Exception) {
                // nothing to do: the next app open asks again
            }
        }
    }

    fun openUsageAccessSettings() {
        val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
        if (!launch(withPackage)) launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openNotificationAccessSettings() {
        notificationSettingsOpened = true
        launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    fun openAppInfo() {
        launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        fun restrictedSuspected(openedSettingsPage: Boolean, granted: Boolean): Boolean = openedSettingsPage && !granted
    }
}
