package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.judging.JudgeFixtures
import com.kleos.sakshi.engine.judging.JudgeFixtures.DAY_MS
import com.kleos.sakshi.engine.judging.JudgeFixtures.afterStarts
import com.kleos.sakshi.engine.judging.JudgeFixtures.baseline
import com.kleos.sakshi.engine.judging.JudgeFixtures.beforeStarts
import com.kleos.sakshi.engine.judging.JudgeFixtures.end
import com.kleos.sakshi.engine.judging.JudgeFixtures.experiment
import com.kleos.sakshi.engine.judging.JudgeFixtures.monday
import com.kleos.sakshi.engine.judging.JudgeFixtures.started
import com.kleos.sakshi.engine.judging.JudgeFixtures.subject
import com.kleos.sakshi.engine.judging.JudgeFixtures.week
import com.kleos.sakshi.engine.judging.JudgeFixtures.window
import com.kleos.sakshi.engine.judging.JudgeFixtures.windows
import com.kleos.sakshi.engine.judging.JudgeFixtures.zone
import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.AppInfo
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.DayDerivation
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StartReason
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WindowWithDetail
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentUseCasesTest {
    private val state = FakeStateStore()
    private val derived = FakeDerivedStore()
    private val catalog = FakeAppCatalog()
    private val ports = Ports(
        events = FakeEventStore(), notifs = FakeNotifStore(), coverage = FakeListenerCoverage(), gaps = FakeGapStore(),
        derived = derived, state = state, catalog = catalog, shelf = FakeSayingShelf(), clock = FakeClock(started),
        random = SeededRandomness(1L),
    )

    private val now = started
    private val s2Task = Candidate(SuggestionKind.S2, subject, 0.4, TargetMetric.STAYS_PER_HOUR_FROM_PKG, emptyMap(), ActionType.MOVE_ICON)

    // ---------- TapTryThis ----------

    @Test fun `a tap on the current suggestion starts a TAP experiment that runs 14 days`() {
        assertEquals(TapOutcome.STARTED, TapTryThis.run(ports, SuggestionKind.S2, subject, s2Task, now))
        val e = state.experiments().single()
        assertEquals(StartReason.TAP, e.startReason); assertEquals(SuggestionKind.S2, e.kind); assertEquals(subject, e.subject)
        assertEquals(now, e.startedAt); assertEquals(TargetMetric.STAYS_PER_HOUR_FROM_PKG, e.target)
        assertEquals(EpochMs(now.value + 14 * DAY_MS), e.windowEnd)
        assertEquals(Verdict.PENDING, e.verdict); assertNull(e.beforeValue); assertNull(e.afterValue); assertFalse(e.approxMix)
    }

    @Test fun `a stale tap returns STALE and creates nothing`() {
        assertEquals(TapOutcome.STALE, TapTryThis.run(ports, SuggestionKind.S2, subject, null, now))                       // nothing is current
        assertEquals(TapOutcome.STALE, TapTryThis.run(ports, SuggestionKind.S3, null, s2Task, now))                        // a different kind
        assertEquals(TapOutcome.STALE, TapTryThis.run(ports, SuggestionKind.S2, Pkg("com.other"), s2Task, now))            // a different subject
        val observation = s2Task.copy(kind = SuggestionKind.S12, target = null, subject = null)
        assertEquals(TapOutcome.STALE, TapTryThis.run(ports, SuggestionKind.S12, null, observation, now))                  // observations have no experiment
        assertTrue(state.experiments().isEmpty())
    }

    @Test fun `a second tap while an experiment is live starts no second one`() {
        TapTryThis.run(ports, SuggestionKind.S2, subject, s2Task, now)
        assertEquals(TapOutcome.ALREADY_RUNNING, TapTryThis.run(ports, SuggestionKind.S2, subject, s2Task, EpochMs(now.value + 1000)))
        assertEquals(1, state.experiments().size)
    }

    @Test fun `experiments get distinct ids so one never replaces another`() {
        TapTryThis.run(ports, SuggestionKind.S2, subject, s2Task, now)
        val first = state.experiments().single()
        state.saveExperiment(first.copy(verdict = Verdict.MOVED))                                                          // judged: no longer live
        TapTryThis.run(ports, SuggestionKind.S2, subject, s2Task, EpochMs(now.value + 30 * DAY_MS))
        assertEquals(2, state.experiments().size)
        assertEquals(2, state.experiments().map { it.id }.toSet().size)
    }

    // ---------- DismissSuggestion ----------

    @Test fun `dismissing a suggestion silences that kind and subject for four weeks and nothing else`() {
        DismissSuggestion.run(ports, SuggestionKind.S2, subject, now)
        val dismissed = state.suggestionStates().single { it.kind == SuggestionKind.S2 && it.subject == subject }
        assertEquals(EpochMs(now.value + 28 * DAY_MS), dismissed.dismissedUntil)
        assertTrue(state.suggestionStates().none { it.subject == Pkg("com.other") })                                       // G-G5: S2/pkgY unaffected
    }

    @Test fun `S7 never ask again about this app means dismissed forever for that subject`() {
        DismissSuggestion.run(ports, SuggestionKind.S7, subject, now)
        assertEquals(EpochMs(Long.MAX_VALUE), state.suggestionStates().single().dismissedUntil)
    }

    @Test fun `dismissing keeps what the state already recorded`() {
        state.saveSuggestionState(SuggestionState(SuggestionKind.S2, subject, EpochMs(5), EpochMs(7), null, null, null, "shown"))
        DismissSuggestion.run(ports, SuggestionKind.S2, subject, now)
        val s = state.suggestionStates().single()
        assertEquals(EpochMs(5), s.firstEligibleAt); assertEquals(EpochMs(7), s.lastShownAt); assertNotNull(s.dismissedUntil)
    }

    // ---------- JudgeExperiments ----------

    private fun storePeriod(starts: List<EpochMs>, totalStays: Int, stretch: Double = 10.0): List<WindowWithDetail> {
        val ws = windows(starts, perWindow = { i -> totalStays / starts.size + if (i < totalStays % starts.size) 1 else 0 }) { s, k -> window(s, k, stretchMin = stretch) }
        ws.groupBy { it.window.day }.forEach { (day, list) -> derived.replaceDay(day, DayDerivation(list, JudgeFixtures.validDay(day))) }
        return ws
    }

    private fun live(e: com.kleos.sakshi.engine.model.Experiment = experiment()) = e.also { state.saveExperiment(it) }

    @Test fun `an experiment whose 14 days passed is judged and the verdict is written with its numbers`() {
        catalog.setLauncherApps(listOf(AppInfo(subject, "Chat", null)))
        storePeriod(beforeStarts, 41); storePeriod(afterStarts, 30)
        live()
        val judged = JudgeExperiments.run(ports, end, zone)
        val written = state.experiments().single()
        assertEquals(listOf(Verdict.MOVED), judged.map { it.verdict })
        assertEquals(Verdict.MOVED, written.verdict)
        assertEquals(4.1, written.beforeValue!!, 1e-9); assertEquals(3.0, written.afterValue!!, 1e-9)
    }

    @Test fun `an experiment still inside its 14 days is left alone`() {
        catalog.setLauncherApps(listOf(AppInfo(subject, "Chat", null)))
        storePeriod(beforeStarts, 41); storePeriod(afterStarts, 30)
        live()
        assertTrue(JudgeExperiments.run(ports, EpochMs(end.value - 1), zone).isEmpty())
        assertEquals(Verdict.PENDING, state.experiments().single().verdict)
    }

    @Test fun `a verdict is never rewritten by a later run`() {
        catalog.setLauncherApps(listOf(AppInfo(subject, "Chat", null)))
        storePeriod(beforeStarts, 41); storePeriod(afterStarts, 30)
        live()
        JudgeExperiments.run(ports, end, zone)
        val first = state.experiments().single()
        derived.clearDerived()                                       // even if the data underneath changes completely
        assertTrue(JudgeExperiments.run(ports, EpochMs(end.value + 5 * DAY_MS), zone).isEmpty())
        assertEquals(first, state.experiments().single())
    }

    @Test fun `NO_CHANGE retires the kind for eight weeks`() {
        catalog.setLauncherApps(listOf(AppInfo(subject, "Chat", null)))
        storePeriod(beforeStarts, 31); storePeriod(afterStarts, 27)
        live(); JudgeExperiments.run(ports, end, zone)
        assertEquals(Verdict.NO_CHANGE, state.experiments().single().verdict)
        assertEquals(EpochMs(end.value + 56 * DAY_MS), state.suggestionStates().single { it.kind == SuggestionKind.S2 }.retiredUntil)
    }

    @Test fun `TOO_LITTLE and UNCLEAR make no claim and free the kind again after four weeks`() {
        catalog.setLauncherApps(listOf(AppInfo(subject, "Chat", null)))
        storePeriod(beforeStarts, 41); storePeriod(afterStarts.take(7), 21)
        live(); JudgeExperiments.run(ports, end, zone)
        assertEquals(Verdict.TOO_LITTLE, state.experiments().single().verdict)
        assertEquals(EpochMs(end.value + 28 * DAY_MS), state.suggestionStates().single().retiredUntil)

        val other = FakeStateStore(); val ports2 = ports.copy(state = other)
        other.saveExperiment(experiment(id = 7)); other.saveBaseline(baseline)
        derived.upsertWeek(week(monday, unusual = true))
        JudgeExperiments.run(ports2, end, zone)
        assertEquals(Verdict.UNCLEAR, other.experiments().single().verdict)
    }

    @Test fun `MOVED sets no retirement`() {
        catalog.setLauncherApps(listOf(AppInfo(subject, "Chat", null)))
        storePeriod(beforeStarts, 41); storePeriod(afterStarts, 30)
        live(); JudgeExperiments.run(ports, end, zone)
        assertTrue(state.suggestionStates().none { it.retiredUntil != null })
    }

    @Test fun `an app that is gone from the launcher makes the experiment UNCLEAR`() {
        storePeriod(beforeStarts, 41); storePeriod(afterStarts, 30)               // the catalog is empty: the subject is uninstalled
        live(); JudgeExperiments.run(ports, end, zone)
        assertEquals(Verdict.UNCLEAR, state.experiments().single().verdict)
    }

    // ---------- footprint start (S2, S4) ----------

    private fun shown(kind: SuggestionKind = SuggestionKind.S2) =
        state.saveSuggestionState(SuggestionState(kind, subject, EpochMs(0), EpochMs(1), null, null, null, "shown"))

    /** Seven-day windows ending at `at`: `prior` stays/window for the earlier week, `last` for the later one, n windows each. */
    private fun twoWeeks(at: EpochMs, prior: Int, last: Int, n: Int = 4) {
        val day = 7 * DAY_MS
        fun starts(from: Long) = (0 until n).map { EpochMs(from + it * DAY_MS + 16 * 3_600_000L) }
        fun put(starts: List<EpochMs>, perWindow: Int) = starts.map { window(it, perWindow) }.let { ws ->
            ws.groupBy { it.window.day }.forEach { (d, l) -> derived.replaceDay(d, DayDerivation(l, JudgeFixtures.validDay(d))) }
        }
        put(starts(at.value - 2 * day), prior); put(starts(at.value - day), last)
    }

    @Test fun `footprint - a shown suggestion whose subject rate halved in a week starts a FOOTPRINT experiment backdated seven days`() {
        shown(); val asOf = EpochMs(started.value + 20 * DAY_MS)
        twoWeeks(asOf, prior = 4, last = 2)                                          // 4/h -> 2/h: exactly half
        val e = FootprintStart.run(ports, asOf, zone)!!
        assertEquals(StartReason.FOOTPRINT, e.startReason); assertEquals(EpochMs(asOf.value - 7 * DAY_MS), e.startedAt)
        assertEquals(TargetMetric.STAYS_PER_HOUR_FROM_PKG, e.target); assertEquals(Verdict.PENDING, e.verdict)
        assertEquals(EpochMs(e.startedAt.value + 14 * DAY_MS), e.windowEnd)
        assertEquals(listOf(e), state.experiments())
    }

    @Test fun `footprint - not enough of a drop, too few windows, an experiment already live, or never shown all start nothing`() {
        val asOf = EpochMs(started.value + 20 * DAY_MS)
        shown(); twoWeeks(asOf, prior = 4, last = 3)                                 // 25 percent drop only
        assertNull(FootprintStart.run(ports, asOf, zone))
        derived.clearDerived(); twoWeeks(asOf, prior = 4, last = 1, n = 2)           // big drop but only 2 windows each week
        assertNull(FootprintStart.run(ports, asOf, zone))
        derived.clearDerived(); twoWeeks(asOf, prior = 4, last = 1)
        state.saveExperiment(experiment(id = 3))                                     // one is already live
        assertNull(FootprintStart.run(ports, asOf, zone))
        val fresh = FakeStateStore(); val ports2 = ports.copy(state = fresh)         // the suggestion was never shown
        assertNull(FootprintStart.run(ports2, asOf, zone))
    }

    @Test fun `footprint - S4 watches pings from the subject`() {
        shown(SuggestionKind.S4); val asOf = EpochMs(started.value + 20 * DAY_MS)
        fun pinged(startMs: EpochMs, k: Int) = window(startMs, 0, otherStays = (0 until k).map { i -> { id: Long -> JudgeFixtures.stay(id, startMs.value + i * 120_000L, origin = com.kleos.sakshi.engine.model.Origin.STONE, stone = subject) } })
        val day = 7 * DAY_MS
        fun put(from: Long, k: Int) = (0 until 4).map { pinged(EpochMs(from + it * DAY_MS + 16 * 3_600_000L), k) }.let { ws ->
            ws.groupBy { it.window.day }.forEach { (d, l) -> derived.replaceDay(d, DayDerivation(l, JudgeFixtures.validDay(d))) }
        }
        put(asOf.value - 2 * day, 4); put(asOf.value - day, 2)
        assertEquals(TargetMetric.PINGS_FROM_PKG_IN_WINDOWS, FootprintStart.run(ports, asOf, zone)!!.target)
    }
}
