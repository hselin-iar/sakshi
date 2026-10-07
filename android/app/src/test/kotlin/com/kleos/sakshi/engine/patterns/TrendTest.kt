package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-P2 is the spec (DOC 3/DOC4, Pattern Layer: Trend). */
class TrendTest {

    private fun week(index: Long, returnMedianMin: Double) = WeekSummary(
        weekStart = WeekStart(StudyDay(index * 7)),
        stretchMedianMin = null, longestStretchMin = null, staysPerHour = null,
        returnMedianMin = returnMedianMin, quietShare = null, inSetShare = null,
        steadiness = null, word = null, windowsCount = 10, validDays = 5, unusual = false,
        appOpens = 0, appMinutes = 0.0, returnsAfterLapse = 0,
    )

    private fun ctx(weeks: List<WeekSummary>) = PatternContext(
        windows = emptyList(), weeks = weeks, days = emptyList(), baseline = null,
        asOf = EpochMs(0), random = SeededRandomness(1L), zone = Zones.KOLKATA,
    )

    @Test
    fun `G-P2 a falling return series of 7,6,5,4 is a BETTER trend with run3`() {
        val weeks = listOf(7.0, 6.0, 5.0, 4.0).mapIndexed { i, v -> week(i.toLong(), v) }
        val pattern = Trend.detect(ctx(weeks)).single { it.key == "trend:return" }

        assertTrue(pattern.args["direction"] == "BETTER")
        assertTrue(pattern.args["run3"] == "true")
        assertTrue(pattern.args["values"] == "7.0,6.0,5.0,4.0")
    }

    @Test
    fun `G-P2 a near-flat return series of 5,5-1,4-9,5-0 produces no pattern`() {
        val weeks = listOf(5.0, 5.1, 4.9, 5.0).mapIndexed { i, v -> week(i.toLong(), v) }
        val patterns = Trend.detect(ctx(weeks))

        assertTrue(patterns.none { it.key == "trend:return" })
    }

    @Test
    fun `fewer than 4 non-unusual weekly values produces no pattern for that part`() {
        val weeks = listOf(7.0, 6.0, 5.0).mapIndexed { i, v -> week(i.toLong(), v) }
        assertTrue(Trend.detect(ctx(weeks)).none { it.key == "trend:return" })
    }

    @Test
    fun `an unusual week is excluded and can starve the required 4 values`() {
        val weeks = listOf(7.0, 6.0, 5.0, 4.0).mapIndexed { i, v -> week(i.toLong(), v) }
        val withUnusual = weeks.toMutableList().also { it[1] = it[1].copy(unusual = true) }

        assertTrue(Trend.detect(ctx(withUnusual)).none { it.key == "trend:return" })
    }
}
