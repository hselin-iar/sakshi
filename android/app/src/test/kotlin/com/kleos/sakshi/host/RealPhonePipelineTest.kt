package com.kleos.sakshi.host

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.LakeState
import com.kleos.sakshi.host.gen.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowUsageStatsManager.EventBuilder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * The whole real path with no hand-fed rows: Android's UsageEvents -> UsageEventsSource -> the ingest the background worker runs ->
 * Room -> the engine -> the Pigeon host the screens call, plus the stored Lake row the widget draws. Everything the phone does,
 * except that the events are simulated.
 */
@RunWith(RobolectricTestRunner::class)
class RealPhonePipelineTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val saved = TimeZone.getDefault()
    private val app get() = RuntimeEnvironment.getApplication()
    private val api by lazy { HostApiImpl(app) }
    private lateinit var container: AppContainer

    @Before fun fresh() { TimeZone.setDefault(TimeZone.getTimeZone(zone)); AppContainer.reset(); container = AppContainer.from(app) }
    @After fun restore() { TimeZone.setDefault(saved) }

    private fun off(block: () -> Unit) { var error: Throwable? = null; Thread { try { block() } catch (t: Throwable) { error = t } }.also { it.start(); it.join() }; error?.let { throw it } }
    private fun ms(date: LocalDate, hm: String) = date.atTime(LocalTime.parse(hm)).atZone(zone).toInstant().toEpochMilli()

    private val shadow get() = shadowOf(app.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as UsageStatsManager)
    private fun event(pkg: String, type: Int, at: Long) = shadow.addEvent(EventBuilder.buildEvent().setPackage(pkg).setClass("$pkg.Main").setEventType(type).setTimeStamp(at).build())
    private fun resumed(pkg: String, at: Long) = event(pkg, UsageEvents.Event.ACTIVITY_RESUMED, at)
    private fun paused(pkg: String, at: Long) = event(pkg, UsageEvents.Event.ACTIVITY_PAUSED, at)

    /** One study evening on the phone: the work app for 90 minutes, two pulls to another app, ten minutes face down. */
    private fun evening(date: LocalDate) {
        resumed("study.notes", ms(date, "18:00"))
        paused("study.notes", ms(date, "18:05")); resumed("study.chat", ms(date, "18:05")); paused("study.chat", ms(date, "18:11")); resumed("study.notes", ms(date, "18:11"))
        paused("study.notes", ms(date, "18:30")); resumed("study.chat", ms(date, "18:30")); paused("study.chat", ms(date, "18:36")); resumed("study.notes", ms(date, "18:36"))
        paused("study.notes", ms(date, "19:00"))
        event("android", UsageEvents.Event.SCREEN_NON_INTERACTIVE, ms(date, "19:00"))
        event("android", UsageEvents.Event.SCREEN_INTERACTIVE, ms(date, "19:10"))
        resumed("study.notes", ms(date, "19:10")); paused("study.notes", ms(date, "19:30"))
    }

    @Test fun twentyFourDaysOfAndroidUsageEventsBecomeAMirrorALakeAndATodayLine() = runBlocking {
        runTwentyFourNights()

        // 1. the raw events and notifications are in Room, in the engine's own vocabulary
        val see = api.getWhatISee()
        // raw events are kept 14 days (14 evenings x 14 events); the days derived from them are kept
        assertEquals(14 * 14, see.rawEventCount.toInt())
        assertTrue("derived days ${see.derivedDays}", see.derivedDays >= 22)
        assertNotNull(see.oldestRawEpochMs)
        assertTrue(see.usageAccessGranted)

        // 2. the days were derived and a baseline was frozen once 8 valid days had passed after the first read
        off { assertNotNull(container.state.baseline()) }

        // 3. the Mirror of the last completed week
        val m = api.getMirror(null)
        println("PHONE headline=${m.headline} steadiness=${m.steadiness} provisional=${m.provisional} dataLines=${m.dataLines}")
        assertFalse(m.provisional)
        assertNotNull(m.steadiness); assertNotNull(m.parts); assertTrue(m.headline.isNotBlank())
        assertNotNull(m.stones); assertTrue(m.stones!!.totalStays > 0)

        // 4. the Lake row the widget draws was written by the same run
        off {
            val row = container.state.lake()
            assertNotNull(row); assertTrue(row!!.state != LakeState.NO_DATA)
            val face = LakeWidget.faceFor(row)
            assertTrue(face.phrase.isNotBlank()); assertNotNull(face.asOfText)
        }
        assertEquals(api.getLake().phrase, off2 { container.state.lake()!!.phrase })

        // 5. weeks to browse
        assertTrue(api.listMirrorWeeks().size >= 2)
    }

    /** The periodic worker's rhythm: one run each night at 22:00 of that day, the first being the first read. The work set is chosen after it. */
    private fun runTwentyFourNights() {
        val first = LocalDate.now(zone).minusDays(24)
        for (d in 0 until 24) {
            val date = first.plusDays(d.toLong())
            evening(date)
            off { assertTrue(container.ingest(EpochMs(ms(date, "22:00"))) is IngestReport.Ran) }
            if (d == 0) runBlocking { api.saveWorkSet(listOf(WorkSetEntryDto("study.notes", UserClassDto.IN_SET))) }
        }
    }

    private fun <T> off2(block: () -> T): T { var r: Result<T>? = null; Thread { r = runCatching(block) }.also { it.start(); it.join() }; return r!!.getOrThrow() }

    @Test fun theFirstReadWithOnlyAFewDaysIsAFirstLookNotAnEmptyPage() = runBlocking {
        val today = LocalDate.now(zone)
        runBlocking { api.saveWorkSet(listOf(WorkSetEntryDto("study.notes", UserClassDto.IN_SET))) }
        for (d in 3 downTo 1) evening(today.minusDays(d.toLong()))
        api.syncNow()      // the first run: reads the last few days Android kept
        val m = api.getMirror(null)
        assertTrue(m.provisional); assertNull(m.steadiness)
        assertTrue(m.headline.isNotBlank()); assertNotNull(m.parts)
        assertTrue(m.dataLines.isNotEmpty())
    }

    @Test fun withoutAWorkSetNothingBreaksAndTheMirrorSaysItIsStillLearning() = runBlocking {
        val today = LocalDate.now(zone)
        for (d in 3 downTo 1) evening(today.minusDays(d.toLong()))
        api.syncNow()
        val m = api.getMirror(null)
        assertTrue(m.provisional); assertNull(m.steadiness)
        assertTrue(m.headline.isNotBlank() || m.dataLines.isNotEmpty())
    }

    @Test fun theWeeklyNoteIsDecidedByTheRealEngineAndPostedExactlyOnce() {
        runTwentyFourNights()
        shadowOf(app).grantPermissions("android.permission.POST_NOTIFICATIONS")
        assertTrue(runBlocking { api.setWeeklyNote(true) })
        val notifications = app.getSystemService(android.app.NotificationManager::class.java)

        // a Monday 09:00 after at least one full week of data
        var monday = LocalDate.now(zone).minusDays(1)
        while (monday.dayOfWeek != java.time.DayOfWeek.MONDAY) monday = monday.minusDays(1)
        val at = EpochMs(ms(monday, "09:00"))

        off2 {
            val engine = container.real.engine
            val decision = engine.noteDecision(at, container.hostFacts())
            assertTrue("decision ${decision.reason}", decision.post)
            assertEquals(WeeklyNoteNotifier.Outcome.POSTED, container.noteNotifier.maybePost(decision))
            assertEquals(1, shadowOf(notifications).allNotifications.size)
            // asking again changes nothing: the week is recorded as noted, so the engine itself says no
            val again = engine.noteDecision(at, container.hostFacts())
            assertFalse(again.post); assertEquals("ALREADY_NOTED", again.reason)
            assertEquals(WeeklyNoteNotifier.Outcome.NOT_DECIDED, container.noteNotifier.maybePost(again))
            assertEquals(1, shadowOf(notifications).allNotifications.size)
        }
    }

    @Test fun theWeeklyNoteStaysQuietOnceTheMirrorHasBeenRead() {
        runTwentyFourNights()
        shadowOf(app).grantPermissions("android.permission.POST_NOTIFICATIONS")
        runBlocking { api.setWeeklyNote(true) }
        var monday = LocalDate.now(zone).minusDays(1)
        while (monday.dayOfWeek != java.time.DayOfWeek.MONDAY) monday = monday.minusDays(1)
        val at = EpochMs(ms(monday, "09:00"))
        off2 {
            val engine = container.real.engine
            engine.mirror(null, at)            // reading it is what marks the week as seen
            val decision = engine.noteDecision(at, container.hostFacts())
            assertFalse(decision.post); assertEquals("ALREADY_VIEWED", decision.reason)
        }
    }

    @Test fun theExportOfARealRunIsDerivedOnlyByDefaultAndHasNoTextFields() {
        runTwentyFourNights()
        val asOf = EpochMs(System.currentTimeMillis())
        val plain = off2 { container.exporter.export(false, asOf).file.readText() }
        val withRaw = off2 { container.exporter.export(true, asOf).file.readText() }
        val json = kotlinx.serialization.json.Json.parseToJsonElement(plain).let { it as kotlinx.serialization.json.JsonObject }
        assertEquals("1", json["exportVersion"].toString())
        assertFalse(plain.contains("rawEvents")); assertTrue(withRaw.contains("rawEvents"))
        assertTrue(plain.contains("\"days\"") || plain.contains("derived"))
        // no notification words or titles can be in a file that never had them
        for (word in listOf("\"title\"", "\"text\"", "\"body\"", "\"extras\"", "\"content\"")) assertFalse("found $word", withRaw.contains(word))
    }
}
