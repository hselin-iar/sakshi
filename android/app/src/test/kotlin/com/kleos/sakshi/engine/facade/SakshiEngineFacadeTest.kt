package com.kleos.sakshi.engine.facade

import com.kleos.sakshi.engine.SakshiEngine
import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.DataState
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GoalAnswer
import com.kleos.sakshi.engine.model.HostFacts
import com.kleos.sakshi.engine.model.LakeState
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.model.WorkSetEntry
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.testkit.FakeAppCatalog
import com.kleos.sakshi.engine.testkit.FakeClock
import com.kleos.sakshi.engine.testkit.FakeDerivedStore
import com.kleos.sakshi.engine.testkit.FakeEventStore
import com.kleos.sakshi.engine.testkit.FakeGapStore
import com.kleos.sakshi.engine.testkit.FakeListenerCoverage
import com.kleos.sakshi.engine.testkit.FakeNotifStore
import com.kleos.sakshi.engine.testkit.FakeSayingShelf
import com.kleos.sakshi.engine.testkit.FakeStateStore
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import com.kleos.sakshi.engine.testkit.events
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

/**
 * The façade end to end over the in-memory ports: real events in, real views out. The façade reads the device zone, so the test pins it
 * to Asia/Kolkata, the zone the DSL builds in.
 */
class SakshiEngineFacadeTest {
    private val savedZone = TimeZone.getDefault()
    private val host = HostFacts(usageAccessGranted = true, notificationAccessGranted = true, canPostNotifications = true, lastError = null)
    private val banned = Regex("""\b(focused|distracted|distraction|wasted|waste|failed|failure|streak|lazy|addict\w*|ruin\w*|you (should|must|need to))\b""", RegexOption.IGNORE_CASE)

    // Monday 14 Sep 2026 is the first study day; three full weeks follow, then Tuesday 6 Oct.
    private val firstMonday = LocalDate.of(2026, 9, 14)
    private fun at(date: LocalDate, hm: String) = epochMs(date, LocalTime.parse(hm), Zones.KOLKATA)

    @Before fun pinZone() = TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    @After fun restoreZone() = TimeZone.setDefault(savedZone)

    private fun newPorts(nowDate: LocalDate, nowTime: String, firstRead: EpochMs? = at(firstMonday, "06:00")): Ports {
        val state = FakeStateStore()
        state.replaceApps(listOf(AppMeta(Pkg("A"), "A", null, UserClass.IN_SET, EpochMs(0))))
        if (firstRead != null) state.saveSettings(state.settings().copy(firstReadAt = firstRead, createdAt = firstRead))
        return Ports(
            events = FakeEventStore(), notifs = FakeNotifStore(),
            coverage = FakeListenerCoverage().apply { openSession(at(firstMonday, "00:00")) }, gaps = FakeGapStore(),
            derived = FakeDerivedStore(), state = state, catalog = FakeAppCatalog(), shelf = FakeSayingShelf(),
            clock = FakeClock(at(nowDate, nowTime)), random = SeededRandomness(1L),
        )
    }

    /** One 90-minute evening of A with `pulls` one-minute detours to an app outside the set. */
    private fun studyDay(date: LocalDate, pulls: Int): List<RawEvent> = events(date) {
        at("18:00:00") resume "A"
        var t = 18 * 60 + 5
        repeat(pulls) {
            at("%02d:%02d:00".format(t / 60, t % 60)) pause "A"
            at("%02d:%02d:00".format(t / 60, t % 60)) resume "X"
            at("%02d:%02d:30".format(t / 60, t % 60)) pause "X"
            at("%02d:%02d:30".format(t / 60, t % 60)) resume "A"
            t += 20
        }
        at("19:00:00") pause "A"
        at("19:00:00").nonInteractive()          // the phone lies face down for ten minutes: quiet time, which the baseline needs to be above zero
        at("19:10:00").interactive()
        at("19:10:00") resume "A"
        at("19:30:00") pause "A"
    }

    private fun feed(p: Ports, days: Int, pulls: Int = 2) {
        val all = (0 until days).flatMap { studyDay(firstMonday.plusDays(it.toLong()), pulls) }
        p.events.append(all)
    }

    private val tuesday = LocalDate.of(2026, 10, 6)

    @Test fun `eight valid days freeze the baseline and the Lake leaves NO_DATA`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val report = SakshiEngine(p).processNewEvents(at(tuesday, "10:00:00"))
        assertTrue(report.baselineFrozen)
        assertNotNull(p.state.baseline())
        assertTrue(p.state.lake()!!.state != LakeState.NO_DATA)
        assertTrue(report.lakeChanged)
    }

    @Test fun `a completed week gives a Mirror with parts, steadiness and no banned words`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val engine = SakshiEngine(p); engine.processNewEvents(at(tuesday, "10:00:00"))
        val m = engine.mirror(null, at(tuesday, "10:00:00"))

        assertFalse(m.provisional)
        assertEquals(DataState.OK, m.dataState)
        assertNotNull(m.parts); assertNotNull(m.steadiness)
        assertTrue(m.headline.isNotBlank())
        val every = listOf(m.headline, m.weekLabel) + m.dataLines + m.patterns.map { it.line } + listOfNotNull(m.suggestion?.line, m.stones?.line, m.parts?.lines?.stretch, m.parts?.lines?.ret)
        every.forEach { s -> assertFalse("banned wording in: $s", banned.containsMatchIn(s)); assertFalse("'!' in: $s", s.contains('!')) }
        assertTrue(m.steadiness!!.word in listOf("Wavering", "Steady", "Steadier"))
    }

    @Test fun `viewing a completed week marks it read and a second view says the same thing`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val engine = SakshiEngine(p); engine.processNewEvents(at(tuesday, "10:00:00"))
        val first = engine.mirror(null, at(tuesday, "10:00:00"))
        assertNotNull(p.state.note().mirrorViewedWeek)
        val second = engine.mirror(null, at(tuesday, "10:05:00"))
        assertEquals(first, second)
    }

    @Test fun `a demo engine says so and otherwise works like the real one`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val engine = SakshiEngine(p, isDemo = true); engine.processNewEvents(at(tuesday, "10:00:00"))
        val m = engine.mirror(null, at(tuesday, "10:00:00"))
        assertTrue(m.isDemo)
        assertNotNull(p.state.note().mirrorViewedWeek)
    }

    @Test fun `with only three days it is a First look that does not claim a steadiness`() {
        val p = newPorts(LocalDate.of(2026, 9, 17), "10:00:00"); feed(p, 3)
        val engine = SakshiEngine(p); engine.processNewEvents(at(LocalDate.of(2026, 9, 17), "10:00:00"))
        val m = engine.mirror(null, at(LocalDate.of(2026, 9, 17), "10:00:00"))
        assertTrue(m.provisional)
        assertNull(m.steadiness)
        assertTrue(m.dataLines.any { it.contains("of 8") || it.contains("8") })
        assertNull(m.suggestion)
    }

    @Test fun `gentle mode keeps only the return line, honest data lines and the saying`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val engine = SakshiEngine(p); engine.processNewEvents(at(tuesday, "10:00:00"))
        engine.setGentle(true)
        val m = engine.mirror(null, at(tuesday, "10:00:00"))
        assertTrue(m.gentle || m.steadiness == null)
        assertNull(m.steadiness); assertTrue(m.patterns.isEmpty()); assertNull(m.suggestion); assertNull(m.teacher)
    }

    @Test fun `Today lists finished windows and leaves out the one still running`() {
        val p = newPorts(tuesday, "18:20:00"); feed(p, 21)
        p.events.append(events(tuesday) { at("18:00:00") resume "A" })   // today's window has started and not ended
        val engine = SakshiEngine(p); engine.processNewEvents(at(tuesday, "18:20:00"))
        val live = engine.today(at(tuesday, "18:20:00"))
        assertTrue(live.windows.isEmpty())
        assertEquals("No finished window yet today.", live.line)

        val sunday = engine.today(at(tuesday.minusDays(2), "21:00:00"))   // 4 Oct, the last day fed: one finished window
        assertEquals(1, sunday.windows.size)
        assertEquals("Today so far: 1 finished window.", sunday.line.substringBefore(" You held"))
    }

    @Test fun `a work set over the cap is refused and a good one is saved`() {
        val p = newPorts(tuesday, "10:00:00")
        val engine = SakshiEngine(p)
        val tooMany = (1..13).map { WorkSetEntry(Pkg("app$it"), UserClass.IN_SET) }
        val bad = engine.saveWorkSet(tooMany)
        assertFalse(bad.ok); assertEquals("Pick up to 12. Fewer is better.", bad.userMessage)
        assertEquals(listOf(Pkg("A")), p.state.apps().map { it.pkg })

        val good = engine.saveWorkSet(listOf(WorkSetEntry(Pkg("B"), UserClass.IN_SET)))
        assertTrue(good.ok); assertEquals(1, good.savedCount)
        assertEquals(listOf(Pkg("B")), p.state.apps().map { it.pkg })
    }

    @Test fun `delete everything empties the derived days`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val engine = SakshiEngine(p); engine.processNewEvents(at(tuesday, "10:00:00"))
        assertTrue(p.derived.days(StudyDay(0), StudyDay.of(at(tuesday, "10:00:00"), Zones.KOLKATA)).isNotEmpty())
        engine.deleteEverything()
        assertTrue(p.derived.days(StudyDay(0), StudyDay.of(at(tuesday, "10:00:00"), Zones.KOLKATA)).isEmpty())
    }

    @Test fun `the weekly note is offered once on Monday morning and not after the Mirror is read`() {
        val monday = LocalDate.of(2026, 10, 5)
        val p = newPorts(monday, "09:00:00"); feed(p, 21)
        p.state.saveSettings(p.state.settings().copy(weeklyNoteEnabled = true))
        val engine = SakshiEngine(p); engine.processNewEvents(at(monday, "09:00:00"))
        assertTrue(engine.noteDecision(at(monday, "09:00:00"), host).post)
        engine.mirror(null, at(monday, "09:00:00"))
        val after = engine.noteDecision(at(monday, "09:05:00"), host)
        assertFalse(after.post); assertEquals("ALREADY_VIEWED", after.reason)
    }

    @Test fun `a goal tap is stored against the latest Mirror week`() {
        val p = newPorts(tuesday, "10:00:00"); feed(p, 21)
        val engine = SakshiEngine(p); engine.processNewEvents(at(tuesday, "10:00:00"))
        engine.tapGoal(GoalAnswer.YES, at(tuesday, "10:00:00"))
        assertEquals(1, p.state.goalTaps().size)
        assertEquals(GoalAnswer.YES, engine.mirror(null, at(tuesday, "10:00:00")).goalTap.answer?.let { GoalAnswer.valueOf(it) })
    }

    @Test fun `pausing records a gap and resuming closes it`() {
        val p = newPorts(tuesday, "10:00:00")
        val engine = SakshiEngine(p)
        engine.pause(true, at(tuesday, "10:00:00"))
        assertTrue(p.gaps.overlapping(at(tuesday, "09:00:00"), at(tuesday, "11:00:00")).isNotEmpty())
        engine.pause(false, at(tuesday, "10:30:00"))
        assertTrue(p.gaps.overlapping(at(tuesday, "10:40:00"), at(tuesday, "11:00:00")).isEmpty())
    }
}
