package com.kleos.sakshi.engine.facade

import com.kleos.sakshi.engine.metrics.Weeks
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WorkSetEntry
import com.kleos.sakshi.engine.note.NoteContext
import com.kleos.sakshi.engine.note.WeeklyNote
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
import com.kleos.sakshi.engine.usecases.ChooseSayings
import com.kleos.sakshi.engine.usecases.PickSaying
import com.kleos.sakshi.engine.usecases.Reanchor
import com.kleos.sakshi.engine.usecases.SayingTags
import com.kleos.sakshi.engine.usecases.SettingsUseCases
import com.kleos.sakshi.engine.usecases.UpdateBaseline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class T215PiecesTest {
    private val zone = Zones.KOLKATA
    private fun at(date: LocalDate, hm: String) = epochMs(date, LocalTime.parse(hm), zone)

    // ---- F9: the weekly note rules, one row per reason ----
    private val week = Weeks.of(at(LocalDate.of(2026, 9, 28), "12:00"), zone)          // Mon 28 Sep, completed on Mon 5 Oct 04:00
    private val monday = LocalDate.of(2026, 10, 5)
    private fun note(asOf: EpochMs, change: (NoteContext) -> NoteContext = { it }) = WeeklyNote.decide(
        change(NoteContext(asOf, enabled = true, canPost = true, lastCompletedWeek = week, validDaysInThatWeek = 5, lastNoteWeek = null, mirrorViewedWeek = null, inWindowNow = false, zone = zone)),
    )

    @Test fun `note posts from Monday 08-00 and says why when it does not`() {
        assertTrue(note(at(monday, "08:00")).post)
        assertEquals("TOO_EARLY", note(at(monday, "07:59")).reason)
        assertEquals("DISABLED", note(at(monday, "09:00")) { it.copy(enabled = false) }.reason)
        assertEquals("NO_PERMISSION", note(at(monday, "09:00")) { it.copy(canPost = false) }.reason)
        assertEquals("NO_WEEK", note(at(monday, "09:00")) { it.copy(lastCompletedWeek = null) }.reason)
        assertEquals("TOO_FEW_DAYS", note(at(monday, "09:00")) { it.copy(validDaysInThatWeek = 2) }.reason)
        assertEquals("ALREADY_NOTED", note(at(monday, "09:00")) { it.copy(lastNoteWeek = week) }.reason)
        assertEquals("ALREADY_VIEWED", note(at(monday, "09:00")) { it.copy(mirrorViewedWeek = week) }.reason)
        assertEquals("IN_WINDOW", note(at(monday, "09:00")) { it.copy(inWindowNow = true) }.reason)
    }

    @Test fun `note is still allowed late on Wednesday and skipped for good from Thursday`() {
        assertTrue(note(at(monday.plusDays(2), "23:59")).post)
        assertEquals("TOO_LATE", note(at(monday.plusDays(3), "00:00")).reason)
    }

    // ---- F13: sayings ----
    private fun shelf(n: Int, usedFor: (Int) -> String = { "" }) = (1..n).map { Saying("S%02d".format(it), "Q1", "text $it", "src", 'A', usedFor(it)) }
    private fun ports(sayings: List<Saying> = emptyList()) = Ports(
        FakeEventStore(), FakeNotifStore(), FakeListenerCoverage(), FakeGapStore(), FakeDerivedStore(), FakeStateStore(), FakeAppCatalog(),
        FakeSayingShelf(sayings), FakeClock(at(monday, "10:00")), SeededRandomness(1L),
    )

    @Test fun `no saying is offered before week 3, then exactly three, the same three for the same week`() {
        val p = ports(shelf(15))
        assertTrue(ChooseSayings.run(p, emptySet(), 2, at(monday, "10:00")).isEmpty())
        val a = ChooseSayings.run(p, emptySet(), 3, at(monday, "10:00"))
        assertEquals(3, a.size)
        assertEquals(a, ChooseSayings.run(p, emptySet(), 3, at(monday, "11:00")))
    }

    @Test fun `a matching tag decides which sayings are offered`() {
        val p = ports(shelf(15) { if (it in 4..6) "Return; Wins" else "" })
        val offered = ChooseSayings.run(p, setOf(SayingTags.RETURN), 3, at(monday, "10:00")).map { it.id }.toSet()
        assertEquals(setOf("S04", "S05", "S06"), offered)
    }

    @Test fun `after a pick nothing is offered for three weeks, and a pick never repeats soon`() {
        val p = ports(shelf(15))
        PickSaying.run(p, "S01", at(monday, "10:00"))
        assertTrue(ChooseSayings.run(p, emptySet(), 4, at(monday.plusDays(14), "10:00")).isEmpty())
        val later = ChooseSayings.run(p, emptySet(), 6, at(monday.plusDays(21), "10:00"))
        assertEquals(3, later.size)
        assertFalse(later.any { it.id == "S01" })
    }

    @Test fun `picking an id that is not on the shelf is ignored`() {
        val p = ports(shelf(5)); PickSaying.run(p, "NOPE", at(monday, "10:00"))
        assertTrue(p.state.sayingPicks().isEmpty())
    }

    // ---- F5: re-anchor refusals ----
    private fun baseline(id: Long, frozenAt: EpochMs, active: Boolean = true) = Baseline(id, frozenAt, 20.0, 2.0, 5.0, 0.1, 8, active)

    @Test fun `re-anchor says why it cannot`() {
        val now = at(monday, "10:00")
        val p = ports()
        assertEquals(Reanchor.REASON_NO_BASELINE, Reanchor.run(p, now, zone).reason)
        p.state.saveBaseline(baseline(UpdateBaseline.FIRST_BASELINE_ID, at(monday.minusDays(7), "04:00")))
        assertEquals(Reanchor.REASON_TOO_EARLY, Reanchor.run(p, now, zone).reason)
        p.state.saveBaseline(baseline(UpdateBaseline.REANCHORED_BASELINE_ID, at(monday.minusDays(60), "04:00")))
        assertEquals(Reanchor.REASON_ALREADY_USED, Reanchor.run(p, now, zone).reason)
    }

    @Test fun `old enough baseline with no days to read says there is too little data`() {
        val p = ports()
        p.state.saveBaseline(baseline(UpdateBaseline.FIRST_BASELINE_ID, at(monday.minusDays(40), "04:00")))
        assertEquals(Reanchor.REASON_TOO_LITTLE, Reanchor.run(p, at(monday, "10:00"), zone).reason)
    }

    // ---- F1/F2: first contact and the work set ----
    @Test fun `first contact sets first-read once and never moves it`() {
        val p = ports()
        SettingsUseCases.ensureInitialised(p, at(monday, "10:00"))
        val first = p.state.settings().firstReadAt
        assertEquals(at(monday, "10:00"), first)
        SettingsUseCases.ensureInitialised(p, at(monday.plusDays(3), "10:00"))
        assertEquals(first, p.state.settings().firstReadAt)
    }

    @Test fun `entries marked NONE do not count toward the cap and duplicates count once`() {
        val p = ports()
        val ok = (1..12).map { WorkSetEntry(Pkg("a$it"), UserClass.IN_SET) } + (1..5).map { WorkSetEntry(Pkg("n$it"), UserClass.NONE) } + WorkSetEntry(Pkg("a1"), UserClass.DEPENDS)
        val r = SettingsUseCases.saveWorkSet(p, ok, at(monday, "10:00"))
        assertTrue(r.ok); assertEquals(12, r.savedCount)
    }
}
