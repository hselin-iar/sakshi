package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-P5 is the spec (DOC 3/DOC4, Pattern Layer: BreakPoint). */
class BreakPointTest {
    private val zone = Zones.KOLKATA
    private val day = StudyDay(0)

    private fun stretch(minutes: Double, endedBy: EndedBy) =
        Stretch(0L, 0L, EpochMs(0), EpochMs((minutes * 60_000).toLong()), minutes, minutes, 0.0, endedBy)

    private fun stay() = Stay(0L, 0L, EpochMs(0), EpochMs(0), Pkg("X"), Pkg("X"), Origin.UNKNOWN, null, false, null, 0)

    private fun window(stretches: List<Stretch>, staysCount: Int) = WindowWithDetail(
        Window(0L, day, EpochMs(0), EpochMs(3_600_000L), WindowSource.INFERRED, partial = false, finalised = true, shape = null),
        stretches, (0 until staysCount).map { stay() }, quietMinutes = 0.0, glances = 0,
    )

    // qualifyingWindows() requires a valid DaySummary for the window's own day -- without one,
    // every window is filtered out before the detector ever sees it.
    private val validDay = DaySummary(
        day, valid = true, windowMinutes = 0.0, quietMinutes = 0.0, inSetMinutes = 0.0, coverage = 1.0,
        pickups = 0, switchesPerHour = null, flinch = null, rampUpMin = null, lastScreenOffTs = null,
        firstStretchMin = null, externalResumes = 0,
    )

    // 35 stays (>= BREAK_MIN_STAYS). bandCounts maps a stretch length (minutes) to how many
    // endings of that length to create; each length falls into its own 5-minute band.
    private fun ctxFromBandCounts(bandCounts: Map<Double, Int>, staysCount: Int = 35) = PatternContext(
        windows = listOf(
            window(
                stretches = bandCounts.flatMap { (minutes, count) -> List(count) { stretch(minutes, EndedBy.STAY) } },
                staysCount = staysCount,
            ),
        ),
        weeks = emptyList(), days = listOf(validDay), baseline = null,
        asOf = EpochMs(0), random = SeededRandomness(1L), zone = zone,
    )

    @Test
    fun `G-P5 22 of 40 endings in one band (55pct) is a pattern`() {
        // 22 endings at 22min (band 20-25) vs 18 at 2min (band 0-5) -> band 20 dominates at 55%.
        val pattern = BreakPoint.detect(ctxFromBandCounts(mapOf(22.0 to 22, 2.0 to 18))).single()

        assertEquals("20", pattern.args["bandStartMin"])
        assertEquals(0.55, pattern.args["bandShare"]!!.toDouble(), 0.0001)
    }

    @Test
    fun `G-P5 18 of 40 endings (45pct) produces no pattern`() {
        // 18 at 22min (band 20) is still the largest single band, but only 45% of 40 -- the
        // other 22 are spread across three other bands so none of them dominates either.
        val patterns = BreakPoint.detect(ctxFromBandCounts(mapOf(22.0 to 18, 2.0 to 7, 7.0 to 7, 12.0 to 8)))
        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `fewer than BREAK_MIN_STAYS stays produces no pattern regardless of band share`() {
        val ctx = PatternContext(
            windows = listOf(window(stretches = List(40) { stretch(22.0, EndedBy.STAY) }, staysCount = 10)),
            weeks = emptyList(), days = listOf(validDay), baseline = null,
            asOf = EpochMs(0), random = SeededRandomness(1L), zone = zone,
        )
        assertTrue(BreakPoint.detect(ctx).isEmpty())
    }

    @Test
    fun `cause is PING with the dominant package when stone share and one package both dominate`() {
        // Same 60/40 stone/self-started ratio as a smaller example, scaled up to clear BREAK_MIN_STAYS (30).
        val pkgB = Pkg("B")
        val stays = List(18) { Stay(0L, 0L, EpochMs(0), EpochMs(0), pkgB, pkgB, Origin.STONE, pkgB, false, null, 0) } +
            List(12) { Stay(0L, 0L, EpochMs(0), EpochMs(0), Pkg("X"), Pkg("X"), Origin.SELF_STARTED, null, false, null, 0) }
        val w = WindowWithDetail(
            Window(0L, day, EpochMs(0), EpochMs(3_600_000L), WindowSource.INFERRED, partial = false, finalised = true, shape = null),
            List(22) { stretch(22.0, EndedBy.STAY) } + List(18) { stretch(2.0, EndedBy.PUT_DOWN) },
            stays, quietMinutes = 0.0, glances = 0,
        )
        val ctx = PatternContext(
            windows = listOf(w), weeks = emptyList(), days = listOf(validDay), baseline = null,
            asOf = EpochMs(0), random = SeededRandomness(1L), zone = zone,
        )

        val pattern = BreakPoint.detect(ctx).single()
        assertEquals("PING:B", pattern.args["cause"])
    }
}
