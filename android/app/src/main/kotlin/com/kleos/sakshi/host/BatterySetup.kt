package com.kleos.sakshi.host

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * The optional battery helper (DOC 2 §2.7.4). It opens Android's own pages and nothing else: it never shows the system
 * exemption dialog, never holds the exemption permission and changes no setting. The decision stays with the user, once.
 */
class BatterySetup(private val context: Context) {
    /** The page that was opened, for the log and the tests. */
    enum class Opened { BATTERY_OPTIMIZATION_LIST, BATTERY_SAVER, APP_INFO, NONE }

    fun open(): Opened {
        val attempts = listOf(
            Opened.BATTERY_OPTIMIZATION_LIST to Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),   // the list page, not the request dialog
            Opened.BATTERY_SAVER to Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            Opened.APP_INFO to Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        for ((opened, intent) in attempts) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return opened
            } catch (_: ActivityNotFoundException) {
                // this phone has no such page; try the next
            } catch (_: SecurityException) {
                // same
            }
        }
        return Opened.NONE
    }
}
