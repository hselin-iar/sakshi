package com.kleos.sakshi.host

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import com.kleos.sakshi.data.DbTest
import com.kleos.sakshi.data.RoomStateStore
import com.kleos.sakshi.engine.model.NoteDecision
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

class WeeklyNoteNotifierTest : DbTest() {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private val state get() = RoomStateStore(db)
    private val notifier get() = WeeklyNoteNotifier(context, state, PermissionGateway(context))
    private val week = WeekStart(StudyDay(20_000))
    private val post = NoteDecision(post = true, reason = null)

    @Before fun allowNotifications() { shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS) }
    private fun readyFor(w: WeekStart?) { state.saveNote(state.note().copy(mirrorReadyWeek = w)) }
    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    @Test fun aDecisionToPostPostsTheConstantNoteOnce() {
        readyFor(week)
        assertEquals(WeeklyNoteNotifier.Outcome.POSTED, notifier.maybePost(post))
        val n = posted().single()
        assertEquals("Sakshi", n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Your Mirror is ready", n.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(week, state.note().lastNoteWeek)
    }

    @Test fun aSecondDecisionForTheSameWeekPostsNothing() {
        readyFor(week)
        notifier.maybePost(post)
        assertEquals(WeeklyNoteNotifier.Outcome.ALREADY_SENT, notifier.maybePost(post))
        assertEquals(1, posted().size)
        // and a later week is a new note
        readyFor(WeekStart(StudyDay(20_007)))
        assertEquals(WeeklyNoteNotifier.Outcome.POSTED, notifier.maybePost(post))
    }

    @Test fun noDecisionNoWeekOrNoPermissionPostsNothingAndDoesNotRecordASend() {
        readyFor(week)
        assertEquals(WeeklyNoteNotifier.Outcome.NOT_DECIDED, notifier.maybePost(NoteDecision(false, "too early")))
        readyFor(null)
        assertEquals(WeeklyNoteNotifier.Outcome.NO_WEEK, notifier.maybePost(post))
        readyFor(week)
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(WeeklyNoteNotifier.Outcome.NOT_ALLOWED, notifier.maybePost(post))
        assertTrue(posted().isEmpty())
        assertNull(state.note().lastNoteWeek)            // blocked, not sent: the next run retries
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(WeeklyNoteNotifier.Outcome.POSTED, notifier.maybePost(post))
    }

    @Test fun theNoteIsSilentHasNoBadgeNoActionsAndExpiresAfterThreeDays() {
        readyFor(week); notifier.maybePost(post)
        val channel = manager.getNotificationChannel("weekly_mirror")
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
        assertNull(channel.sound); assertFalse(channel.shouldVibrate()); assertFalse(channel.canShowBadge())
        val n = posted().single()
        assertNull(n.actions)
        assertEquals(TimeUnit.DAYS.toMillis(3), n.timeoutAfter)
        assertEquals("weekly_mirror", n.channelId)
        assertNotNull(n.contentIntent)
    }

    @Test fun theOnlyTextsAreTheConstantsAndCarryNothingAboutTheUser() {
        readyFor(week); notifier.maybePost(post)
        val extras = posted().single().extras
        val strings = extras.keySet().mapNotNull { extras.get(it) as? CharSequence }.map { it.toString() }
        assertEquals(setOf("Sakshi", "Your Mirror is ready"), strings.toSet())   // no subtext, big text, info text or people
        strings.forEach { assertFalse(it.any(Char::isDigit)); assertFalse(it.contains("!")) }
    }

    @Test fun cancelRemovesAShowingNote() {
        readyFor(week); notifier.maybePost(post)
        notifier.cancel()
        assertTrue(posted().isEmpty())
    }

    @Test fun onlyAnAllowedRequestTurnsTheNoteOnAndARefusalLeavesItOff() {
        assertTrue(WeeklyNoteNotifier.effective(asked = true, alreadyAllowed = true, grantedByRequest = null))
        assertTrue(WeeklyNoteNotifier.effective(asked = true, alreadyAllowed = false, grantedByRequest = true))
        assertFalse(WeeklyNoteNotifier.effective(asked = true, alreadyAllowed = false, grantedByRequest = false))
        assertFalse(WeeklyNoteNotifier.effective(asked = true, alreadyAllowed = false, grantedByRequest = null))
        assertFalse(WeeklyNoteNotifier.effective(asked = false, alreadyAllowed = true, grantedByRequest = null))
    }
}
