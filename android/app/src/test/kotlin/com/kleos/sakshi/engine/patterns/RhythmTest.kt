package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** G-P1 is the spec (DOC 3/DOC4, Pattern Layer: Rhythm). */
class RhythmTest {
    private val zone = Zones.KOLKATA

    // Three guaranteed-consecutive weekdays, found at runtime so the test never depends on
    // what day of the week any particular hardcoded date happens to be.
    private val monday = run {
        var d = LocalDate.of(2026, 10, 5)
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.plusDays(1)
        d
    }
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)

    private fun window(date: LocalDate, hour: Int, durationMin: Long, staysCount: Int): WindowWithDetail {
        val day = StudyDay.of(epochAt(date, hour, 30), zone) // 30 past the hour, safely inside the study day
        val start = epochAt(date, hour, 0)
        val end = EpochMs(start.value + durationMin * 60_000L)
        val w = Window(0L, day, start, end, WindowSource.INFERRED, partial = false, finalised = true, shape = null)
        val stays = (0 until staysCount).map { dummyStay() }
        return WindowWithDetail(w, emptyList(), stays, quietMinutes = 0.0, glances = 0)
    }

    private fun epochAt(date: LocalDate, hour: Int, minute: Int) =
        epochMs(date, LocalTime.of(hour, minute), zone)

    private fun dummyStay() = Stay(
        0L, 0L, EpochMs(0), EpochMs(0), Pkg("X"), Pkg("X"), Origin.UNKNOWN, null, false, null, 0,
    )

    private fun validDaySummary(day: StudyDay) = DaySummary(
        day, valid = true, windowMinutes = 0.0, quietMinutes = 0.0, inSetMinutes = 0.0, coverage = 1.0,
        pickups = 0, switchesPerHour = null, flinch = null, rampUpMin = null, lastScreenOffTs = null,
        firstStretchMin = null, externalResumes = 0,
    )

    private fun ctx(windows: List<WindowWithDetail>, days: List<StudyDay>) = PatternContext(
        windows = windows, weeks = emptyList(), days = days.distinct().map { validDaySummary(it) },
        baseline = null, asOf = EpochMs(0), random = SeededRandomness(1L), zone = zone,
    )

    // Cell windows: 23:00, 1h, 6 stays -> rate 6. Other windows: 10:00, 1h, 3 stays -> rate 3.
    // Overall median across all 9 windows = 3.0 (5 threes, 4 sixes). Cell median = 6.0.
    // ratio = (6+0.5)/(3+0.5) = 1.857... -> passes (4 windows, 3 days) -> CHOPPY.
    private fun cellWindows(count: Int, days: List<LocalDate>) =
        days.zip(distributeCounts(count, days.size)).flatMap { (date, n) -> List(n) { window(date, 23, 60, 6) } }

    private fun otherWindows() = listOf(
        window(monday, 10, 60, 3), window(monday, 10, 60, 3),
        window(tuesday, 10, 60, 3), window(tuesday, 10, 60, 3),
        window(wednesday, 10, 60, 3),
    )

    private fun distributeCounts(total: Int, buckets: Int): List<Int> {
        val base = total / buckets
        val remainder = total % buckets
        return (0 until buckets).map { base + if (it < remainder) 1 else 0 }
    }

    @Test
    fun `G-P1 4 windows over 3 days at ratio 1-857 is a CHOPPY pattern`() {
        val cell = cellWindows(4, listOf(monday, tuesday, wednesday))
        val windows = cell + otherWindows()
        val days = windows.map { it.window.day }

        val patterns = Rhythm.detect(ctx(windows, days))
        val cellPattern = patterns.single { it.key == "cell:NIGHT-WEEKDAY" }

        assertEquals(1.8571428571, cellPattern.strength, 0.0001)
        assertEquals("CHOPPY", cellPattern.args["direction"])
        assertEquals(4, cellPattern.evidenceWindows)
        assertEquals(3, cellPattern.evidenceDays)
    }

    @Test
    fun `G-P1 the same shape with only 3 cell windows produces no pattern`() {
        val cell = cellWindows(3, listOf(monday, tuesday, wednesday))
        val windows = cell + otherWindows()
        val patterns = Rhythm.detect(ctx(windows, windows.map { it.window.day }))

        assertTrue(patterns.none { it.key == "cell:NIGHT-WEEKDAY" })
    }

    @Test
    fun `G-P1 4 cell windows over only 2 days produces no pattern`() {
        val cell = cellWindows(4, listOf(monday, tuesday)) // only 2 distinct days
        val windows = cell + otherWindows()
        val patterns = Rhythm.detect(ctx(windows, windows.map { it.window.day }))

        assertTrue(patterns.none { it.key == "cell:NIGHT-WEEKDAY" })
    }

    @Test
    fun `G-P1 a ratio of 1-17 (below the 1-5 gate) produces no pattern`() {
        // 100-minute windows with 6 stays -> rate 6 / (100/60) = 3.6, giving ratio (3.6+0.5)/(3+0.5) = 1.1714...
        val cell = listOf(monday, monday, tuesday, wednesday).map { window(it, 23, 100, 6) }
        val windows = cell + otherWindows()
        val patterns = Rhythm.detect(ctx(windows, windows.map { it.window.day }))

        assertTrue(patterns.none { it.key == "cell:NIGHT-WEEKDAY" })
    }

    @Test
    fun `clearHour finds a slot that is quiet and long-stretched relative to the rest`() {
        // Overall: lots of short, busy windows. One slot (14:00-16:00) is long and quiet.
        val busy = (0 until 6).map { window(monday, 10, 20, 4) } // 20min, rate 4/(1/3)=12/h -- busy and short
        val quiet = (0 until 3).map { window(tuesday, 14, 120, 0) } // 2h, 0 stays -- quiet and long
        val windows = busy + quiet
        val result = Rhythm.clearHour(ctx(windows, windows.map { it.window.day }))

        assertEquals(14, result!!.startHour)
        assertEquals(16, result.endHour)
    }

    @Test
    fun `clearHour is null with fewer than 3 windows in the quiet slot`() {
        val busy = (0 until 6).map { window(monday, 10, 20, 4) }
        val quiet = (0 until 2).map { window(tuesday, 14, 120, 0) } // only 2, below the minimum
        val windows = busy + quiet

        assertNull(Rhythm.clearHour(ctx(windows, windows.map { it.window.day })))
    }
}
