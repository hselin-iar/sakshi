package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.model.WindowSource
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
import com.kleos.sakshi.engine.testkit.GOLDEN_DATE
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import com.kleos.sakshi.engine.testkit.epochMsAt
import com.kleos.sakshi.engine.testkit.events
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * T2.8's five golden fixtures (DOC4's agent prompt hint names them): a clean
 * evening, a ping-broken evening, a midnight crossing, split-screen/PiP, and
 * a listener-gap day. Each is hand-verified below in a comment before being
 * accepted as golden (DOC4's "your job alongside").
 */
class RecomputeDayTest {
    private val zone = Zones.KOLKATA
    private val day = StudyDay(GOLDEN_DATE.toEpochDay())
    private val asOf = epochMsAt("23:00:00") // well past every fixture's window, so finalised=true throughout

    private fun ports(
        rawEvents: List<RawEvent> = emptyList(),
        notifs: List<NotifEvent> = emptyList(),
        coverage: FakeListenerCoverage = FakeListenerCoverage().apply { openSession(epochMsAt("00:00:00")) },
        inSetPkgs: List<String> = listOf("A"), // B defaults to unclassified -> OFF_SET, needed by fixtures 2 and 5
    ): Ports {
        val events = FakeEventStore().apply { append(rawEvents) }
        val notifStore = FakeNotifStore().apply { notifs.forEach { append(it) } }
        val state = FakeStateStore()
        state.replaceApps(inSetPkgs.map { AppMeta(Pkg(it), it, null, UserClass.IN_SET, EpochMs(0)) })
        return Ports(
            events = events, notifs = notifStore, coverage = coverage, gaps = FakeGapStore(),
            derived = FakeDerivedStore(), state = state, catalog = FakeAppCatalog(),
            shelf = FakeSayingShelf(), clock = FakeClock(asOf),
            random = SeededRandomness(1L),
        )
    }

    @Test
    fun `fixture 1 - a clean evening is one window, one WINDOW_END stretch, no stays`() {
        // A in-set 20:00-21:00, nothing else. Hand check: run [20:00,21:00] (60min >= 5min) pads to
        // [19:50,21:10] (80min). No off-set time at all -> no stays, one stretch = the whole window.
        val stream = events { at("20:00:00") resume ("A"); at("21:00:00") pause ("A") }
        val p = ports(rawEvents = stream)

        val derivation = RecomputeDay.recompute(day, asOf, p, zone)

        assertEquals(1, derivation.windows.size)
        val wd = derivation.windows.single()
        assertEquals(epochMsAt("19:50:00"), wd.window.start)
        assertEquals(epochMsAt("21:10:00"), wd.window.end)
        assertEquals(WindowSource.INFERRED, wd.window.source)
        assertTrue(wd.window.finalised)
        assertFalse(wd.window.partial)
        assertEquals(0, wd.stays.size)
        assertEquals(1, wd.stretches.size)
        assertEquals(EndedBy.WINDOW_END, wd.stretches.single().endedBy)
        assertEquals(80.0, wd.stretches.single().minutes, 0.0001)

        assertEquals(80.0, derivation.summary.windowMinutes, 0.0001)
        assertEquals(80.0, derivation.summary.inSetMinutes, 0.0001)
        assertEquals(0.0, derivation.summary.quietMinutes, 0.0001)
        assertTrue(derivation.summary.valid) // 80 >= VALID_DAY_WINDOW_MIN (45)
    }

    @Test
    fun `fixture 2 - a ping-broken evening is STONE with a 1-minute return`() {
        // A 20:00-20:30 (30min, IN_SET), B 20:30-20:31 (60s, OFF_SET -> stay, since >=30s),
        // A 20:31-21:00 (29min, IN_SET). Gap between the two A runs is 1min <= 3min -> one
        // chain, span [20:00,21:00] padded to [19:50,21:10]. The 60s off-set run is a stay
        // (merge doesn't apply, only one run). POSTED(B) at 20:29:45 is 15s before stay.start
        // (within the 30s look-back) -> STONE, stonePkg=B. Returns: A resumes at 20:31 and
        // holds >=30s -> return starts at 20:31, i.e. 1.0 minute after stay.start (20:30).
        val stream = events {
            at("20:00:00") resume ("A")
            at("20:30:00") pause ("A")
            at("20:30:00") resume ("B")
            at("20:31:00") pause ("B")
            at("20:31:00") resume ("A")
            at("21:00:00") pause ("A")
        }
        val notifs = listOf(
            NotifEvent(epochMsAt("20:29:45"), Pkg("B"), category = null, kind = NotifKind.POSTED, removal = null, ongoing = false),
        )
        val p = ports(rawEvents = stream, notifs = notifs)

        val derivation = RecomputeDay.recompute(day, asOf, p, zone)

        val wd = derivation.windows.single()
        assertEquals(epochMsAt("19:50:00"), wd.window.start)
        assertEquals(epochMsAt("21:10:00"), wd.window.end)
        assertEquals(1, wd.stays.size)
        val stay = wd.stays.single()
        assertEquals(epochMsAt("20:30:00"), stay.start)
        assertEquals(epochMsAt("20:31:00"), stay.end)
        assertEquals(Pkg("B"), stay.firstPkg)
        assertEquals(Pkg("B"), stay.pkgMain)
        assertEquals(Origin.STONE, stay.origin)
        assertEquals(Pkg("B"), stay.stonePkg)
        assertFalse(stay.notifClicked)
        assertEquals(1.0, stay.returnMinutes!!, 0.0001)

        assertEquals(2, wd.stretches.size)
        assertEquals(EndedBy.STAY, wd.stretches[0].endedBy)
        assertEquals(40.0, wd.stretches[0].minutes, 0.0001) // 19:50 -> 20:30
        assertEquals(EndedBy.WINDOW_END, wd.stretches[1].endedBy)
        assertEquals(39.0, wd.stretches[1].minutes, 0.0001) // 20:31 -> 21:10

        assertEquals(79.0, derivation.summary.inSetMinutes, 0.0001) // 80 total - the 1-minute stay
    }

    @Test
    fun `fixture 3 - a midnight crossing is attributed to the day it started in`() {
        // Day = the study day BEFORE GOLDEN_DATE (so it spans GOLDEN_DATE-1 04:00 to GOLDEN_DATE
        // 04:00). An interval starting at GOLDEN_DATE 03:30 (still within that earlier study day,
        // since 03:30 < 04:00) running to GOLDEN_DATE 04:30 (past the boundary) belongs WHOLLY to
        // the earlier day, per DOC 3: "a window is attributed to the study day it started in (even
        // if it crosses 04:00)". Hand check: run [03:30,04:30] (60min) pads to [03:20,04:40].
        val previousDay = StudyDay(GOLDEN_DATE.toEpochDay() - 1)
        val start = epochMs(GOLDEN_DATE, LocalTime.of(3, 30), zone)
        val end = epochMs(GOLDEN_DATE, LocalTime.of(4, 30), zone)
        val stream = listOf(
            RawEvent(start, RawType.ACTIVITY_RESUMED, Pkg("A")),
            RawEvent(end, RawType.ACTIVITY_PAUSED, Pkg("A")),
        )
        val p = ports(rawEvents = stream)

        val derivation = RecomputeDay.recompute(previousDay, asOf, p, zone)

        val wd = derivation.windows.single()
        assertEquals(epochMs(GOLDEN_DATE, LocalTime.of(3, 20), zone), wd.window.start)
        assertEquals(epochMs(GOLDEN_DATE, LocalTime.of(4, 40), zone), wd.window.end)
        assertEquals(previousDay, wd.window.day)
        assertEquals(80.0, derivation.summary.windowMinutes, 0.0001)
    }

    @Test
    fun `fixture 4 - split-screen (RESUMED without a PAUSED between) stays one continuous window`() {
        // RES A at 19:30; RES B at 20:00 with NO PAUSED A between -- R1 closes A at B's start
        // (the split-screen rule). PAUSED B at 20:05. Both A and B are IN_SET. Hand check:
        // intervals [A 19:30-20:00 (30min), B 20:00-20:05 (5min)], gap between them is 0 -> one
        // chain, 35min total >= 5min, span [19:30,20:05] pads to [19:20,20:15] (55min).
        val stream = listOf(
            RawEvent(epochMsAt("19:30:00"), RawType.ACTIVITY_RESUMED, Pkg("A")),
            RawEvent(epochMsAt("20:00:00"), RawType.ACTIVITY_RESUMED, Pkg("B")),
            RawEvent(epochMsAt("20:05:00"), RawType.ACTIVITY_PAUSED, Pkg("B")),
        )
        val p = ports(rawEvents = stream, inSetPkgs = listOf("A", "B"))

        val derivation = RecomputeDay.recompute(day, asOf, p, zone)

        assertEquals(1, derivation.windows.size)
        val wd = derivation.windows.single()
        assertEquals(epochMsAt("19:20:00"), wd.window.start)
        assertEquals(epochMsAt("20:15:00"), wd.window.end)
        assertEquals(0, wd.stays.size)
        assertEquals(55.0, derivation.summary.windowMinutes, 0.0001)
        assertEquals(55.0, derivation.summary.inSetMinutes, 0.0001)
    }

    @Test
    fun `fixture 5 - a listener-gap day leaves the stay UNKNOWN despite a matching ping`() {
        // Same shape as fixture 2 (ping from B 15s before a stay starting at 20:30), but listener
        // coverage disconnects at 18:00 and only reconnects at 20:30:00 -- a gap that entirely
        // swallows the stay's 30s look-back [20:29:30,20:30:00]. StoneWave must return UNKNOWN,
        // not STONE, regardless of the ping. Day coverage = (86400 - 9000) / 86400 = 0.895833...
        // (9000s = 2.5 hours uncovered out of a full day).
        val stream = events {
            at("20:00:00") resume ("A")
            at("20:30:00") pause ("A")
            at("20:30:00") resume ("B")
            at("20:31:00") pause ("B")
            at("20:31:00") resume ("A")
            at("21:00:00") pause ("A")
        }
        val notifs = listOf(
            NotifEvent(epochMsAt("20:29:45"), Pkg("B"), category = null, kind = NotifKind.POSTED, removal = null, ongoing = false),
        )
        val coverage = FakeListenerCoverage().apply {
            openSession(epochMsAt("00:00:00"))
            closeSession(epochMsAt("18:00:00"))
            openSession(epochMsAt("20:30:00"))
        }
        val p = ports(rawEvents = stream, notifs = notifs, coverage = coverage)

        val derivation = RecomputeDay.recompute(day, asOf, p, zone)

        val stay = derivation.windows.single().stays.single()
        assertEquals(Origin.UNKNOWN, stay.origin)
        assertEquals(null, stay.stonePkg)
        assertFalse(stay.notifClicked)
        assertEquals(77_400.0 / 86_400.0, derivation.summary.coverage, 0.0001)
    }

    // Properties (DOC4's Done-when): recomputing a day twice gives identical rows; duplicating or
    // reordering raw events changes nothing; events after asOf change nothing.

    @Test
    fun `recomputing the same day twice gives identical rows`() {
        val stream = events { at("20:00:00") resume ("A"); at("21:00:00") pause ("A") }
        val p = ports(rawEvents = stream)

        assertEquals(RecomputeDay.recompute(day, asOf, p, zone), RecomputeDay.recompute(day, asOf, p, zone))
    }

    @Test
    fun `duplicating or reordering raw events changes nothing`() {
        val original = events { at("20:00:00") resume ("A"); at("20:30:00") pause ("A"); at("20:30:00") resume ("B"); at("20:31:00") pause ("B") }
        val duplicated = (original + original).distinct() // FakeEventStore has no dedupe; simulate the real store's unique key
        val reordered = original.reversed()

        val fromOriginal = RecomputeDay.recompute(day, asOf, ports(rawEvents = original), zone)
        val fromDuplicated = RecomputeDay.recompute(day, asOf, ports(rawEvents = duplicated), zone)
        val fromReordered = RecomputeDay.recompute(day, asOf, ports(rawEvents = reordered), zone)

        assertEquals(fromOriginal, fromDuplicated)
        assertEquals(fromOriginal, fromReordered)
    }

    @Test
    fun `events after asOf change nothing`() {
        val upToAsOf = events { at("20:00:00") resume ("A"); at("21:00:00") pause ("A") }
        val afterAsOf = upToAsOf + listOf(
            RawEvent(epochMsAt("23:30:00"), RawType.ACTIVITY_RESUMED, Pkg("A")),
        )

        val withoutExtra = RecomputeDay.recompute(day, asOf, ports(rawEvents = upToAsOf), zone)
        val withExtra = RecomputeDay.recompute(day, asOf, ports(rawEvents = afterAsOf), zone)

        assertEquals(withoutExtra, withExtra)
    }
}
