package com.kleos.sakshi.host

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kleos.sakshi.engine.model.EpochMs

/**
 * Notification access. Each callback reduces the notification to NotificationFacts and hands it on. The title, text, extras,
 * icon and intent are never read, here or in logs (DOC 2 §2.6.1).
 */
class NotificationCollector : NotificationListenerService() {
    private val recorder: NotificationRecorder by lazy {
        val container = AppContainer.from(applicationContext)
        NotificationRecorder(container.notifs, container.notifs, container.state, packageName) { EpochMs(System.currentTimeMillis()) }
    }

    override fun onListenerConnected() {
        bound = true
        recorder.onConnected()
    }

    override fun onListenerDisconnected() {
        bound = false
        recorder.onDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        factsOf(sbn)?.let(recorder::onPosted)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        factsOf(sbn)?.let { recorder.onRemoved(it, clicked = reason == REASON_CLICK) }
    }

    companion object {
        /** True between onListenerConnected and onListenerDisconnected; PermissionGateway reads it to decide on a rebind. */
        @Volatile var bound: Boolean = false
            private set

        private fun factsOf(sbn: StatusBarNotification?): NotificationFacts? =
            sbn?.let { factsFrom(it.packageName, it.postTime, it.notification) }

        /** The whole extraction. It touches `category` and `flags` only. */
        fun factsFrom(pkg: String, postTime: Long, notification: Notification): NotificationFacts {
            val ongoing = notification.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_GROUP_SUMMARY) != 0
            return NotificationFacts(pkg, postTime, notification.category, ongoing)
        }

        fun componentName(context: android.content.Context) = ComponentName(context, NotificationCollector::class.java)
    }
}
