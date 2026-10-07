package com.kleos.sakshi.host

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kleos.sakshi.MainActivity
import com.kleos.sakshi.engine.model.NoteDecision
import com.kleos.sakshi.engine.ports.StateStore
import java.util.concurrent.TimeUnit

/**
 * Posts the quiet weekly note, and only when the engine's decision says so. The text is two constants and nothing about the
 * user's data: no score, no app name, no number. Silent, no badge, no actions, gone after three days (DOC 3 F9).
 */
class WeeklyNoteNotifier(private val context: Context, private val state: StateStore, private val permissions: PermissionGateway) {
    enum class Outcome { POSTED, NOT_DECIDED, NO_WEEK, ALREADY_SENT, NOT_ALLOWED }

    private val manager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Idempotent per week: once a note went out for the week the Mirror is ready for, a repeated decision posts nothing.
     * A blocked post is not recorded as sent, so the next run retries until the engine's own Wednesday cut-off.
     */
    fun maybePost(decision: NoteDecision): Outcome {
        val note = state.note()
        val week = note.mirrorReadyWeek
        when {
            !decision.post -> return Outcome.NOT_DECIDED
            week == null -> return Outcome.NO_WEEK              // nothing to record, so nothing to post (it would repeat forever)
            note.lastNoteWeek == week -> return Outcome.ALREADY_SENT
            !permissions.canPostNotifications() -> return Outcome.NOT_ALLOWED
        }
        ensureChannel()
        manager.notify(NOTIFICATION_ID, build())
        state.saveNote(note.copy(lastNoteWeek = week))
        return Outcome.POSTED
    }

    /** Turning the note off removes one that is still showing. */
    fun cancel() = manager.cancel(NOTIFICATION_ID)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun build(): Notification = Notification.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(TITLE)
        .setContentText(BODY)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setTimeoutAfter(TimeUnit.DAYS.toMillis(TIMEOUT_DAYS))
        .setContentIntent(
            PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java)
                    .putExtra(LakeWidget.EXTRA_ROUTE, LakeWidget.ROUTE_MIRROR).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .build()

    companion object {
        const val CHANNEL_ID = "weekly_mirror"      // LC-9
        const val TITLE = "Sakshi"
        const val BODY = "Your Mirror is ready"
        const val CHANNEL_NAME = "Weekly Mirror"
        const val NOTIFICATION_ID = 4201
        const val TIMEOUT_DAYS = 3L

        /**
         * What "on" means: the user asked for it AND Android allows it, either already or after the request just made.
         * A refusal leaves the setting off.
         */
        fun effective(asked: Boolean, alreadyAllowed: Boolean, grantedByRequest: Boolean?): Boolean =
            asked && (alreadyAllowed || grantedByRequest == true)
    }
}

/** Asks Android for POST_NOTIFICATIONS through the Activity. Null when no Activity is showing (a cold start from the worker). */
fun interface PostNotificationsRequester {
    suspend fun request(): Boolean
}
