package com.kleos.sakshi.engine.stays

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.testkit.GOLDEN_DATE
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** G-S1..G-S3 are the spec (DOC 3, F4. Stone and Wave: StayDetector). */
class StayDetectorTest {
    private val day = StudyDay(GOLDEN_DATE.toEpochDay())

    private fun window(start: String, end: String) = Window(
        id = 1L, day = day, start = epochMsAt(start), end = epochMsAt(end),
        source = WindowSource.INFERRED, partial = false, finalised = true, shape = null,
    )

    private fun interval(pkg: String, start: String, end: String) =
        ForegroundInterval(Pkg(pkg), epochMsAt(start), epochMsAt(end))

    @Test
    fun `G-S1a a 29s off-set run is a glance, not a stay`() {
        val w = window("10:00:00", "10:20:00")
        val intervals = listOf(
            interval("A", "10:00:00", "10:10:00"),
            interval("B", "10:10:00", "10:10:29"),
            interval("A", "10:10:29", "10:20:00"),
        )
        val classes = listOf(AppClass.IN_SET, AppClass.OFF_SET, AppClass.IN_SET)

        val result = StayDetector.detect(w, intervals, classes, emptyList())

        assertEquals(1, result.glances)
        assertEquals(emptyList(), result.stays)
    }

    @Test
    fun `G-S1b a 30s off-set run is a stay`() {
        val w = window("10:00:00", "10:20:00")
        val intervals = listOf(
            interval("A", "10:00:00", "10:10:00"),
            interval("B", "10:10:00", "10:10:30"),
            interval("A", "10:10:30", "10:20:00"),
        )
        val classes = listOf(AppClass.IN_SET, AppClass.OFF_SET, AppClass.IN_SET)

        val result = StayDetector.detect(w, intervals, classes, emptyList())

        assertEquals(0, result.glances)
        assertEquals(1, result.stays.size)
        val stay = result.stays.single()
        assertEquals(epochMsAt("10:10:00"), stay.start)
        assertEquals(epochMsAt("10:10:30"), stay.end)
        assertEquals(Pkg("B"), stay.firstPkg)
        assertEquals(Pkg("B"), stay.pkgMain)
        assertEquals(0, stay.glancesBefore)
        assertEquals(Origin.UNKNOWN, stay.origin)
        assertEquals(null, stay.returnMinutes)
    }

    @Test
    fun `G-S2 a 15s gap between off-set runs merges into one stay`() {
        val w = window("10:00:00", "10:20:00")
        // No marker event between B and C at all -- a plain recording gap, not an IN_SET blip.
        val intervals = listOf(
            interval("B", "10:10:00", "10:10:40"),
            interval("C", "10:10:55", "10:11:30"),
        )
        val classes = listOf(AppClass.OFF_SET, AppClass.OFF_SET)

        val result = StayDetector.detect(w, intervals, classes, emptyList())

        assertEquals(1, result.stays.size)
        val stay = result.stays.single()
        assertEquals(epochMsAt("10:10:00"), stay.start)
        assertEquals(epochMsAt("10:11:30"), stay.end)
        assertEquals(Pkg("B"), stay.firstPkg)
        assertEquals(Pkg("B"), stay.pkgMain) // 40s vs 35s
    }

    @Test
    fun `G-S3 a 21s gap between off-set runs stays separate`() {
        val w = window("10:00:00", "10:20:00")
        val intervals = listOf(
            interval("B", "10:10:00", "10:10:40"),
            interval("C", "10:11:01", "10:11:36"), // gap = 21s
        )
        val classes = listOf(AppClass.OFF_SET, AppClass.OFF_SET)

        val result = StayDetector.detect(w, intervals, classes, emptyList())

        assertEquals(2, result.stays.size)
        assertEquals(epochMsAt("10:10:00"), result.stays[0].start)
        assertEquals(epochMsAt("10:10:40"), result.stays[0].end)
        assertEquals(epochMsAt("10:11:01"), result.stays[1].start)
        assertEquals(epochMsAt("10:11:36"), result.stays[1].end)
    }

    // Property: stays never overlap and each lies inside the window. Seeds used: 1..10.
    @Test
    fun `random off-set interval sets never produce overlapping or out-of-window stays`() {
        for (seed in 1L..10L) {
            val random = Random(seed)
            val w = window("06:00:00", "20:00:00")
            val intervals = randomOffSetIntervals(random, count = 1 + random.nextInt(15))
            val classes = intervals.map { AppClass.OFF_SET }

            val result = StayDetector.detect(w, intervals, classes, emptyList())

            val sorted = result.stays.sortedBy { it.start.value }
            for (i in 1 until sorted.size) {
                assertTrue("seed=$seed", sorted[i].start.value >= sorted[i - 1].end.value)
            }
            for (stay in result.stays) {
                assertTrue("seed=$seed", stay.start.value >= w.start.value && stay.end.value <= w.end.value)
                assertTrue("seed=$seed", stay.start.value < stay.end.value)
            }
        }
    }

    private fun randomOffSetIntervals(random: Random, count: Int): List<ForegroundInterval> {
        var ts = epochMsAt("07:00:00").value
        val result = mutableListOf<ForegroundInterval>()
        repeat(count) {
            val start = ts
            val duration = 5_000L + random.nextInt(120_000) // 5s-2min
            val end = start + duration
            result += ForegroundInterval(Pkg("X"), EpochMs(start), EpochMs(end))
            ts = end + 1_000L + random.nextInt(120_000) // ensure a real gap each time, >= 1s
        }
        return result
    }
}
