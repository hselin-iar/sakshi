package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * G-P7 is the spec, but hand-tracing 40 points through k-means++ convergence
 * isn't something I can verify by hand the way the other golden examples
 * were -- this uses deliberately well-separated clusters (only stretchMin
 * and staysPerHour vary meaningfully; every other vector dimension is held
 * identical across all windows) so the result doesn't depend on exact
 * initialization/iteration behavior, and guards the "none" case with the
 * stays/hour gate directly rather than relying on silhouette for random
 * data. Flagging this as the one piece of T2.11 I'm least confident is bug-free.
 */
class ClusteringTest {
    private val zone = Zones.KOLKATA

    private val monday = run {
        var d = LocalDate.of(2026, 10, 5)
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.plusDays(1)
        d
    }
    private val saturday = monday.plusDays(5)

    private fun eightWeeks() = (0 until 8).map { i ->
        WeekSummary(
            weekStart = WeekStart(StudyDay(i.toLong() * 7)), stretchMedianMin = null, longestStretchMin = null,
            staysPerHour = null, returnMedianMin = null, quietShare = null, inSetShare = null, steadiness = null,
            word = null, windowsCount = 5, validDays = 5, unusual = false, appOpens = 0, appMinutes = 0.0, returnsAfterLapse = 0,
        )
    }

    private fun windowWithDetail(date: LocalDate, hour: Int, stretchMin: Double, staysCount: Int): WindowWithDetail {
        val day = StudyDay.of(epochMs(date, LocalTime.of(hour, 30), zone), zone)
        val start = epochMs(date, LocalTime.of(hour, 0), zone)
        val end = EpochMs(start.value + 60 * 60_000L) // 1-hour window
        val w = Window(0L, day, start, end, WindowSource.INFERRED, partial = false, finalised = true, shape = null)
        val stretch = Stretch(0L, 0L, start, end, stretchMin, stretchMin, 0.0, EndedBy.WINDOW_END)
        val stays = (0 until staysCount).map {
            Stay(0L, 0L, start, start, Pkg("X"), Pkg("X"), Origin.SELF_STARTED, null, false, null, 0)
        }
        return WindowWithDetail(w, listOf(stretch), stays, quietMinutes = 0.0, glances = 0)
    }

    // qualifyingWindows() requires a valid DaySummary per window day, or every window is
    // filtered out before the detector ever sees it -- derive one for each day actually used.
    private fun validDaySummaryFor(day: StudyDay) = DaySummary(
        day, valid = true, windowMinutes = 0.0, quietMinutes = 0.0, inSetMinutes = 0.0, coverage = 1.0,
        pickups = 0, switchesPerHour = null, flinch = null, rampUpMin = null, lastScreenOffTs = null,
        firstStretchMin = null, externalResumes = 0,
    )

    private fun ctx(windows: List<WindowWithDetail>, seed: Long = 1L) = PatternContext(
        windows = windows, weeks = eightWeeks(),
        days = windows.map { it.window.day }.distinct().map { validDaySummaryFor(it) },
        baseline = null, asOf = EpochMs(0), random = SeededRandomness(seed), zone = zone,
    )

    @Test
    fun `G-P7 two well-separated groups produce a cluster pattern`() {
        val groupA = (0 until 5).flatMap { day -> (0 until 5).map { windowWithDetail(monday.plusDays(day.toLong()), 14, 15.0, 2) } }
        val groupB = (0 until 15).map { windowWithDetail(if (it % 2 == 0) saturday else saturday.plusDays(1), 19, 5.0, 10) }

        val patterns = Clustering.detect(ctx(groupA + groupB))

        assertTrue(patterns.isNotEmpty())
        assertTrue(patterns.any { it.key == "cluster:EVENING-WEEKEND" })
    }

    @Test
    fun `uniform noise near the overall rate produces no pattern`() {
        val random = Random(7L)
        // staysPerHour is identical (2) for every window, so overall == every possible cluster
        // centre's own staysPerHour == 2, and 2 >= 1.5x2 is false -- the gate can never pass
        // regardless of how k-means splits the (randomly varying) stretchMin dimension.
        val windows = (0 until 40).map {
            val stretch = 5.0 + random.nextInt(20)
            windowWithDetail(monday.plusDays((it % 5).toLong()), 10 + (it % 8), stretch, 2)
        }

        val patterns = Clustering.detect(ctx(windows))

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `the same seed produces identical cluster assignments across runs`() {
        val groupA = (0 until 5).flatMap { day -> (0 until 5).map { windowWithDetail(monday.plusDays(day.toLong()), 14, 15.0, 2) } }
        val groupB = (0 until 15).map { windowWithDetail(if (it % 2 == 0) saturday else saturday.plusDays(1), 19, 5.0, 10) }
        val windows = groupA + groupB

        val first = Clustering.detect(ctx(windows, seed = 42L))
        val second = Clustering.detect(ctx(windows, seed = 42L))

        assertEquals(first, second)
    }

    @Test
    fun `fewer than CLUSTER_MIN_WINDOWS qualifying windows produces no pattern`() {
        val windows = (0 until 10).map { windowWithDetail(monday, 14, 15.0, 2) }
        assertTrue(Clustering.detect(ctx(windows)).isEmpty())
    }
}
