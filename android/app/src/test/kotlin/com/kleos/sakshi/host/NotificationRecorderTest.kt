package com.kleos.sakshi.host

import android.app.Notification
import com.kleos.sakshi.data.*
import com.kleos.sakshi.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class NotificationRecorderTest : DbTest() {
    private var clock = 10_000L
    private val notifs get() = RoomNotifStore(db)
    private fun recorder() = NotificationRecorder(notifs, notifs, RoomStateStore(db), ownPackage = "com.kleos.sakshi") { t(clock) }
    private fun facts(pkg: String = "chat", ongoing: Boolean = false) = NotificationFacts(pkg, 5_000, "msg", ongoing)

    @Test fun aPostedNotificationStoresPackageTimeCategoryAndOngoingOnly() {
        recorder().onPosted(facts())
        assertEquals(NotifEvent(t(5_000), Pkg("chat"), "msg", NotifKind.POSTED, null, false), notifs.range(t(0), t(99_999)).single())
    }

    @Test fun aTapRemovesWithClickAndAnythingElseWithOther() {
        val r = recorder()
        r.onRemoved(facts("a"), clicked = true); r.onRemoved(facts("b"), clicked = false)
        val rows = notifs.range(t(0), t(99_999)).associateBy { it.pkg.value }
        assertEquals(RemovalKind.CLICK, rows.getValue("a").removal)
        assertEquals(RemovalKind.OTHER, rows.getValue("b").removal)
        assertEquals(NotifKind.REMOVED, rows.getValue("a").kind)
        assertEquals(10_000L, rows.getValue("a").ts.value)   // removal time is when it happened
    }

    @Test fun ongoingNotificationsAreStoredFlaggedSoTheEngineCanIgnoreThem() {
        recorder().onPosted(facts(ongoing = true))
        assertTrue(notifs.range(t(0), t(99_999)).single().ongoing)
    }

    @Test fun ownPackageIsIgnored() {
        val r = recorder()
        r.onPosted(facts("com.kleos.sakshi")); r.onRemoved(facts("com.kleos.sakshi"), clicked = true)
        assertEquals(0, notifs.count())
    }

    @Test fun nothingIsStoredWhilePaused() {
        RoomStateStore(db).let { it.saveIngest(it.ingest().copy(paused = true)) }
        val r = recorder()
        r.onPosted(facts()); r.onRemoved(facts(), clicked = true)
        assertEquals(0, notifs.count())
        RoomStateStore(db).let { it.saveIngest(it.ingest().copy(paused = false)) }
        r.onPosted(facts())
        assertEquals(1, notifs.count())
    }

    @Test fun connectAndDisconnectOpenAndCloseOneSession() {
        val r = recorder()
        r.onConnected(); clock = 20_000; r.onDisconnected()
        val s = notifs.sessions(t(0), t(99_999)).single()
        assertEquals(10_000L to 20_000L, s.connectedAt.value to s.disconnectedAt!!.value)
    }

    @Test fun aStaleOpenSessionIsClosedAtTheLastNotificationHeardBeforeANewOneOpens() {
        val r = recorder()
        r.onConnected()                       // opens at 10_000, then the process is killed: no disconnect
        r.onPosted(facts().copy(postTime = 12_000))
        clock = 50_000
        r.onConnected()                       // the system rebinds
        val sessions = notifs.sessions(t(0), t(99_999))
        assertEquals(2, sessions.size)
        assertEquals(12_000L, sessions[0].disconnectedAt!!.value)    // not 50_000: the gap was not heard
        assertNull(sessions[1].disconnectedAt)
        assertEquals(50_000L, sessions[1].connectedAt.value)
        assertFalse(notifs.coversInterval(t(20_000), t(40_000)))
    }

    @Test fun aStaleSessionWithNoNotificationsClosesAtItsOwnStart() {
        val r = recorder()
        r.onConnected(); clock = 50_000; r.onConnected()
        assertEquals(10_000L, notifs.sessions(t(0), t(99_999)).first().disconnectedAt!!.value)
    }

    @Test fun aFailingCallbackIsSwallowedAndCounted() {
        db.close()                            // every store call now throws
        val r = recorder()
        r.onPosted(facts()); r.onConnected(); r.onDisconnected()
        assertEquals(3, r.errorCount)
        db = SakshiDatabase.inMemory(RuntimeEnvironment.getApplication())   // so @After can close it
    }

    @Test fun aNotificationCarryingTitleAndTextProducesARowWithNeither() {
        val secretTitle = "SECRET-TITLE-9f3"; val secretText = "secret body text 77x"
        val notification = Notification.Builder(RuntimeEnvironment.getApplication(), "channel-name")
            .setContentTitle(secretTitle).setContentText(secretText).setSubText("sub-secret")
            .setCategory(Notification.CATEGORY_MESSAGE).build()
        assertEquals(secretTitle, notification.extras.getString(Notification.EXTRA_TITLE))   // the content really is there

        recorder().onPosted(NotificationCollector.factsFrom("chat", 5_000, notification))

        val row = notifs.range(t(0), t(99_999)).single()
        assertEquals("msg".let { Notification.CATEGORY_MESSAGE }, row.category)
        // every column of every table, as text: the secrets appear nowhere
        val everything = db.openHelper.readableDatabase.let { sql ->
            sql.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'")
                .use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList() }
                .flatMap { table -> sql.query("SELECT * FROM `$table`").use { c ->
                    generateSequence { if (c.moveToNext()) (0 until c.columnCount).joinToString("|") { i -> c.getString(i) ?: "" } else null }.toList() } }
        }.joinToString("\n")
        listOf(secretTitle, secretText, "sub-secret", "channel-name").forEach { assertFalse("leaked: $it", everything.contains(it)) }
    }

    @Test fun theColumnsOfNotifEventHoldNoContent() {
        val columns = db.openHelper.readableDatabase.query("SELECT * FROM notif_event").use { it.columnNames.toSet() }
        assertEquals(setOf("id", "ts", "pkg", "category", "kind", "removal", "ongoing"), columns)
    }

    @Test fun factsFromReadsOnlyCategoryAndTheOngoingFlags() {
        val n = Notification.Builder(RuntimeEnvironment.getApplication(), "c").setOngoing(true).setCategory(Notification.CATEGORY_PROGRESS).build()
        assertEquals(NotificationFacts("p", 1, Notification.CATEGORY_PROGRESS, true), NotificationCollector.factsFrom("p", 1, n))
        assertFalse(NotificationCollector.factsFrom("p", 1, Notification.Builder(RuntimeEnvironment.getApplication(), "c").build()).ongoing)
    }
}
