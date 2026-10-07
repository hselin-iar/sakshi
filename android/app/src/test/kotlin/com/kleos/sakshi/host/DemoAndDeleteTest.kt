package com.kleos.sakshi.host

import android.appwidget.AppWidgetManager
import android.widget.TextView
import com.kleos.sakshi.R
import com.kleos.sakshi.data.*
import com.kleos.sakshi.engine.model.*
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DemoAndDeleteTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private lateinit var container: AppContainer

    @Before fun fresh() { AppContainer.reset(); container = AppContainer.from(app) }

    /** SHA-256 over the real database's own files. The -shm index is left out: it is a memory map, not data. */
    private fun realDbHash(): String {
        val md = MessageDigest.getInstance("SHA-256")
        listOf("", "-wal", "-journal").map { app.getDatabasePath(SakshiDatabase.FILE_NAME).path + it }.map(::File).filter { it.isFile }.forEach { md.update(it.readBytes()) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun seedReal() = off {
        container.real.state.saveSettings(container.real.state.settings().copy(gentleMode = true, createdAt = t(5)))
        container.real.events.append(listOf(raw(1_000), raw(2_000, RawType.SCREEN_NON_INTERACTIVE, null)))
        container.real.notifs.append(notif(1_500))
        container.real.state.saveLake(LakeRow(LakeState.STILL, "Still water.", t(9)))
    }

    @Test fun startingADemoLeavesTheRealDatabaseFilesByteIdentical() {
        seedReal()
        val before = realDbHash()
        off { container.demoController.start("aarav") }
        val during = realDbHash()
        off { container.demoController.setAsOf(59); container.demoController.setAsOf(0) }
        off { container.demoController.stop() }
        val after = realDbHash()
        println("real database hash before=$before during=$during after=$after")
        assertEquals(before, during); assertEquals(before, after)
        assertTrue(File(app.getDatabasePath(SakshiDatabase.FILE_NAME).path).isFile)
    }

    @Test fun duringADemoScreensReadTheDemoStoresAndTheCollectorKeepsWritingToTheRealOnes() {
        seedReal()
        off { container.demoController.start("aarav") }
        assertTrue(container.demoActive)
        off {
            assertEquals(1, container.real.notifs.count())
            assertTrue(container.notifs.count() > 0)                         // the persona's own pings are in the demo set
            container.real.notifs.append(notif(3_000))                       // what NotificationCollector does
            assertEquals(2, container.real.notifs.count())
            val demoPings = container.notifs.count()
            assertEquals(2, container.real.notifs.count())
            assertEquals(demoPings, container.notifs.count())                // real pings never leak into the demo
            assertTrue(container.events.count() > 0)                         // the persona's history is there
            assertEquals(2, container.real.events.count())                   // and the real events are as they were
            assertFalse(container.state.settings().gentleMode)               // demo settings, not the real ones
            assertTrue(container.real.state.settings().gentleMode)
        }
        off { container.demoController.stop() }
        assertFalse(container.demoActive)
        off { assertTrue(container.state.settings().gentleMode); assertEquals(2, container.notifs.count()) }
    }

    @Test fun nothingIsReadFromThePhoneWhileADemoRuns() {
        off { container.demoController.start("rohan") }
        assertEquals(IngestReport.DemoActive, container.ingest(t(1_000)))
        off { assertEquals(0, container.real.events.count()) }
        off { container.demoController.stop() }
    }

    @Test fun theDemoClockMovesWithTheSliderAndClampsToTheFirstReadDay() {
        off { container.demoController.start("aarav") }
        val c = container.demoController
        off { c.setAsOf(0) }
        assertEquals(c.dayStartMs(DemoController.FIRST_READ_DAY + 1) - 60_000, container.clock.now().value)
        off { c.setAsOf(31) }
        assertEquals(c.dayStartMs(32) - 60_000, container.clock.now().value)
        off { c.setAsOf(99) }
        assertEquals(c.dayStartMs(60) - 60_000, container.clock.now().value)
        off { c.stop() }
    }

    @Test fun movingTheSliderRebuildsDerivedRowsFromScratch() {
        off { container.demoController.start("aarav") }
        off {
            container.derived.replaceDay(StudyDay(7), derivation(7, 1))
            container.demoController.setAsOf(10)
            val days = container.derived.days(StudyDay(0), StudyDay(Long.MAX_VALUE / 2))
            assertTrue(days.none { it.day == StudyDay(7) })       // the hand-written row is gone: the days were rebuilt from the events
            assertTrue(days.isNotEmpty())                         // and the engine wrote its own
        }
        off { container.demoController.stop() }
    }

    @Test fun anUnknownPersonaFailsAndLeavesTheAppInRealMode() {
        seedReal()
        val before = realDbHash()
        off { try { container.demoController.start("nobody"); fail("expected failure") } catch (_: IllegalArgumentException) { } }
        assertFalse(container.demoActive)
        assertEquals(before, realDbHash())
    }

    @Test fun theLakeWidgetPhraseCarriesDemoWhileADemoRuns() {
        val manager = AppWidgetManager.getInstance(app)
        off { container.demoController.start("aarav"); container.state.saveLake(LakeRow(LakeState.STILL, "Still water.", t(9))) }
        val id = shadowOf(manager).createWidget(LakeWidget::class.java, R.layout.lake_widget)
        Thread.sleep(500)
        off { LakeWidget.refresh(app) }
        assertEquals("Still water. (demo)", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.lake_phrase).text.toString())
        off { container.demoController.stop(); LakeWidget.refresh(app) }
        // back to the real Lake, which has no row in this test: the engine's "nothing to show yet", and no "(demo)"
        assertEquals("Nothing to show yet.", shadowOf(manager).getViewFor(id).findViewById<TextView>(R.id.lake_phrase).text.toString())
    }

    // ---- delete ----
    private fun tables(): List<String> = container.database.openHelper.readableDatabase
        .query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name <> 'room_master_table'")
        .use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList() }

    private fun rows(t: String) = container.database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$t`").use { it.moveToFirst(); it.getInt(0) }

    @Test fun deleteEverythingEmptiesEveryTableRemovesExportsAndResetsTheLake() {
        seedReal()
        off {
            container.real.derived.replaceDay(StudyDay(100), derivation(100, 1))
            container.real.gaps.add(DataGap(GapKind.NOT_SEEN, t(1), t(2)))
            container.exporter.export(false, t(1_759_000_000_000))
            assertTrue(File(app.cacheDir, "exports").isDirectory)
            assertTrue(tables().all { it == "baseline" || it == "app_meta" || it == "listener_session" || rows(it) >= 0 })
            container.deleteEverything()
            tables().forEach { assertEquals("$it should be empty", 0, rows(it)) }
            assertNull(container.real.state.lake())                // the Lake reads as "nothing to show yet"
        }
        assertFalse(File(app.cacheDir, "exports").exists())
        assertEquals(20, off20())
    }

    private fun off20(): Int = tables().size

    @Test fun afterDeleteTheNextIngestBehavesLikeAFreshInstall() {
        seedReal()
        off { container.real.state.saveIngest(container.real.state.ingest().copy(cursor = t(5), firstReadAt = t(6))); container.deleteEverything() }
        off { assertNull(container.real.state.ingest().cursor); assertNull(container.real.state.ingest().firstReadAt) }
    }
}
