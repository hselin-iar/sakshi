package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-P3 is the spec (DOC 3/DOC4, Pattern Layer: Shift/CUSUM). */
class ShiftTest {

    private fun week(index: Long, staysPerHour: Double) = WeekSummary(
        weekStart = WeekStart(StudyDay(index * 7)),
        stretchMedianMin = null, longestStretchMin = null, staysPerHour = staysPerHour,
        returnMedianMin = null, quietShare = null, inSetShare = null,
        steadiness = null, word = null, windowsCount = 10, validDays = 5, unusual = false,
        appOpens = 0, appMinutes = 0.0, returnsAfterLapse = 0,
    )

    private fun ctx(weeks: List<WeekSummary>) = PatternContext(
        windows = emptyList(), weeks = weeks, days = emptyList(), baseline = null,
        asOf = EpochMs(0), random = SeededRandomness(1L), zone = Zones.KOLKATA,
    )

    @Test
    fun `G-P3 a step from 4 to 6 at index 3 is a persistent SHIFT up`() {
        val weeks = listOf(4.0, 4.0, 4.0, 6.0, 6.0, 6.0).mapIndexed { i, v -> week(i.toLong(), v) }
        val pattern = Shift.detect(ctx(weeks)).single { it.key == "shift:stays" }

        assertEquals("3", pattern.args["weekIndex"])
        assertEquals("UP", pattern.args["direction"])
    }

    @Test
    fun `G-P3 a noisy series around 4 never alarms`() {
        val weeks = listOf(4.0, 4.0, 4.0, 4.2, 3.9, 4.1).mapIndexed { i, v -> week(i.toLong(), v) }
        val patterns = Shift.detect(ctx(weeks))

        assertTrue(patterns.none { it.key == "shift:stays" })
    }

    @Test
    fun `fewer than 4 weekly values never alarms`() {
        val weeks = listOf(4.0, 4.0, 6.0).mapIndexed { i, v -> week(i.toLong(), v) }
        assertTrue(Shift.detect(ctx(weeks)).none { it.key == "shift:stays" })
    }
}
