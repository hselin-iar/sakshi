package com.kleos.sakshi.engine.windows

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StudyBlock
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.testkit.GOLDEN_DATE
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** G-W1..G-W4 are the spec (DOC 3, F3. Window Finder). */
class WindowFinderTest {
    private val day = StudyDay(GOLDEN_DATE.toEpochDay())
    private val zone = Zones.KOLKATA
    private val farAsOf = epochMsAt("23:59:59") // well past end+15min for every case below

    private fun interval(pkg: String, start: String, end: String) =
        ForegroundInterval(Pkg(pkg), epochMsAt(start), epochMsAt(end))

    @Test
    fun `G-W1 a study block and an overlapping-after-padding run merge to BOTH`() {
        val studyBlocks = listOf(StudyBlock(startMinute = 20 * 60, endMinute = 23 * 60))
        val intervals = listOf(interval("A", "22:50:00", "23:30:00"))
        val classes = listOf(AppClass.IN_SET)

        val windows = WindowFinder.find(day, intervals, classes, studyBlocks, emptyList(), farAsOf, zone)

        assertEquals(
            listOf(Window(0L, day, epochMsAt("20:00:00"), epochMsAt("23:40:00"), WindowSource.BOTH, false, true, null)),
            windows,
        )
    }

    @Test
    fun `G-W2a a 4-minute run is below RUN_MIN_IN_SET_MIN and produces no window`() {
        val intervals = listOf(interval("A", "21:00:00", "21:04:00"))
        val classes = listOf(AppClass.IN_SET)

        val windows = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), farAsOf, zone)

        assertEquals(emptyList<Window>(), windows)
    }

    @Test
    fun `G-W2b a 5-minute run meets RUN_MIN_IN_SET_MIN and is padded INFERRED`() {
        val intervals = listOf(interval("A", "21:00:00", "21:05:00"))
        val classes = listOf(AppClass.IN_SET)

        val windows = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), farAsOf, zone)

        assertEquals(
            listOf(Window(0L, day, epochMsAt("20:50:00"), epochMsAt("21:15:00"), WindowSource.INFERRED, false, true, null)),
            windows,
        )
    }

    @Test
    fun `G-W3a a 3-minute gap between runs joins into one chain`() {
        val intervals = listOf(
            interval("A", "21:00:00", "21:10:00"),
            interval("B", "21:13:00", "21:20:00"), // gap exactly 3 minutes
        )
        val classes = listOf(AppClass.IN_SET, AppClass.IN_SET)

        val windows = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), farAsOf, zone)

        assertEquals(
            listOf(Window(0L, day, epochMsAt("20:50:00"), epochMsAt("21:30:00"), WindowSource.INFERRED, false, true, null)),
            windows,
        )
    }

    @Test
    fun `G-W3b a 40-minute gap between runs produces two windows`() {
        val intervals = listOf(
            interval("A", "21:00:00", "21:10:00"),
            interval("B", "21:50:00", "22:00:00"),
        )
        val classes = listOf(AppClass.IN_SET, AppClass.IN_SET)

        val windows = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), farAsOf, zone)

        assertEquals(
            listOf(
                Window(0L, day, epochMsAt("20:50:00"), epochMsAt("21:20:00"), WindowSource.INFERRED, false, true, null),
                Window(0L, day, epochMsAt("21:40:00"), epochMsAt("22:10:00"), WindowSource.INFERRED, false, true, null),
            ),
            windows,
        )
    }

    @Test
    fun `G-W4 a valid day needs at least 45 non-partial minutes`() {
        val fortyFour = listOf(
            Window(0L, day, epochMsAt("08:00:00"), epochMsAt("08:44:00"), WindowSource.INFERRED, false, true, null),
        )
        val fortyFive = listOf(
            Window(0L, day, epochMsAt("08:00:00"), epochMsAt("08:45:00"), WindowSource.INFERRED, false, true, null),
        )

        assertFalse(WindowFinder.isValidDay(fortyFour))
        assertTrue(WindowFinder.isValidDay(fortyFive))
    }

    @Test
    fun `a window overlapping a data gap is partial`() {
        val intervals = listOf(interval("A", "21:00:00", "21:05:00"))
        val classes = listOf(AppClass.IN_SET)
        val gap = DataGap(GapKind.NOT_SEEN, epochMsAt("21:00:00"), epochMsAt("21:02:00"))

        val windows = WindowFinder.find(day, intervals, classes, emptyList(), listOf(gap), farAsOf, zone)

        assertEquals(1, windows.size)
        assertTrue(windows.single().partial)
    }

    @Test
    fun `a window not overlapping any data gap is not partial`() {
        val intervals = listOf(interval("A", "21:00:00", "21:05:00"))
        val classes = listOf(AppClass.IN_SET)
        val gap = DataGap(GapKind.NOT_SEEN, epochMsAt("08:00:00"), epochMsAt("08:05:00"))

        val windows = WindowFinder.find(day, intervals, classes, emptyList(), listOf(gap), farAsOf, zone)

        assertEquals(1, windows.size)
        assertFalse(windows.single().partial)
    }

    @Test
    fun `finalised is end plus WINDOW_FINALISE_LAG_MIN compared to asOf`() {
        val intervals = listOf(interval("A", "21:00:00", "21:05:00")) // padded window ends 21:15:00
        val classes = listOf(AppClass.IN_SET)

        val notYet = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), epochMsAt("21:20:00"), zone)
        val now = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), epochMsAt("21:30:00"), zone)

        assertFalse(notYet.single().finalised) // 21:15 + 15min = 21:30, asOf 21:20 is before that
        assertTrue(now.single().finalised) // asOf 21:30 == end + 15min, "<=" so this counts
    }

    // Property: windows never overlap; every window.start < window.end. Seeds used: 1..10.
    @Test
    fun `random in-set interval sets never produce overlapping or inverted windows`() {
        for (seed in 1L..10L) {
            val random = Random(seed)
            val intervals = randomInSetIntervals(random, count = 1 + random.nextInt(20))
            val classes = intervals.map { AppClass.IN_SET }

            val windows = WindowFinder.find(day, intervals, classes, emptyList(), emptyList(), farAsOf, zone)

            for (w in windows) {
                assertTrue("seed=$seed", w.start.value < w.end.value)
            }
            val sorted = windows.sortedBy { it.start.value }
            for (i in 1 until sorted.size) {
                assertTrue("seed=$seed overlap at $i", sorted[i].start.value >= sorted[i - 1].end.value)
            }
        }
    }

    private fun randomInSetIntervals(random: Random, count: Int): List<ForegroundInterval> {
        var ts = epochMsAt("06:00:00").value
        val result = mutableListOf<ForegroundInterval>()
        repeat(count) {
            val start = ts
            val duration = 60_000L + random.nextInt(600_000) // 1-10 minutes
            val end = start + duration
            result += ForegroundInterval(Pkg("A"), EpochMs(start), EpochMs(end))
            ts = end + 1_000L + random.nextInt(600_000) // 1s-10min gap before the next one
        }
        return result
    }
}
