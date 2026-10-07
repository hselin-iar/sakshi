package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-P6 is the spec (DOC 3/DOC4, Pattern Layer: CrossDay). */
class CrossDayTest {
    private val zone = Zones.KOLKATA

    // lastScreenOffMinutesAfterStart: minutes after this day's own 04:00 start. >= 1230 (20h30m,
    // i.e. 00:30 the next calendar day) is LATE.
    private fun day(index: Long, lastScreenOffMinutesAfterStart: Int, firstStretchMin: Double) = DaySummary(
        day = StudyDay(index),
        valid = true,
        windowMinutes = 0.0, quietMinutes = 0.0, inSetMinutes = 0.0, coverage = 1.0, pickups = 0,
        switchesPerHour = null, flinch = null, rampUpMin = null,
        lastScreenOffTs = EpochMs(StudyDay(index).startEpochMs(zone).value + lastScreenOffMinutesAfterStart * 60_000L),
        firstStretchMin = firstStretchMin,
        externalResumes = 0,
    )

    private fun ctx(days: List<DaySummary>) = PatternContext(
        windows = emptyList(), weeks = emptyList(), days = days, baseline = null,
        asOf = EpochMs(0), random = SeededRandomness(1L), zone = zone,
    )

    // Days 0-4 end late (00:30+); days 1-5 (following them) have an 8-minute first stretch.
    // Days 5-12 end at a normal hour; days 6-13 (following them) have a 14-minute first stretch.
    private fun fourteenDays(lateTransitionCount: Int): List<DaySummary> {
        val days = mutableListOf<DaySummary>()
        for (i in 0L until 14L) {
            val isLateTransition = i < lateTransitionCount
            val lastScreenOff = if (isLateTransition) 1230 else 600 // 00:30 vs 14:00
            // This day's own firstStretchMin reflects whether the PRECEDING transition was late.
            val precedingWasLate = i > 0 && (i - 1) < lateTransitionCount
            val firstStretch = if (precedingWasLate) 8.0 else 14.0
            days += day(i, lastScreenOff, firstStretch)
        }
        return days
    }

    @Test
    fun `G-P6 5 late nights with 8min vs 14min first stretch is a pattern`() {
        val pattern = CrossDay.detect(ctx(fourteenDays(lateTransitionCount = 5))).single()

        assertEquals("5", pattern.args["lateNights"])
    }

    @Test
    fun `G-P6 3 late nights produces no pattern`() {
        val patterns = CrossDay.detect(ctx(fourteenDays(lateTransitionCount = 3)))
        assertTrue(patterns.isEmpty())
    }
}
