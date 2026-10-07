package com.kleos.sakshi.engine.baseline

import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** G-F7's "baseline never changes after freeze" property, at the pure-function level. */
class BaselineServiceTest {

    private fun dayData(dayIndex: Long, stretchMinutes: Double, stayReturn: Double): ValidDayData {
        val day = StudyDay(dayIndex)
        val start = EpochMs(dayIndex * 86_400_000L)
        val end = EpochMs(start.value + 3_600_000L) // a 1-hour window
        val window = Window(
            id = 0L, day = day, start = start, end = end,
            source = WindowSource.INFERRED, partial = false, finalised = true, shape = null,
        )
        val stretch = Stretch(
            id = 0L, windowId = 0L, start = start, end = end,
            minutes = stretchMinutes, inSetMinutes = stretchMinutes - 5.0, quietMinutes = 5.0, endedBy = EndedBy.WINDOW_END,
        )
        val stay = Stay(
            id = 0L, windowId = 0L, start = start, end = start,
            firstPkg = Pkg("X"), pkgMain = Pkg("X"), origin = Origin.UNKNOWN,
            stonePkg = null, notifClicked = false, returnMinutes = stayReturn, glancesBefore = 0,
        )
        return ValidDayData(day = day, endOfDay = end, windows = listOf(window), stretches = listOf(stretch), stays = listOf(stay))
    }

    @Test
    fun `fewer than BASELINE_VALID_DAYS valid days yields no baseline`() {
        val days = (1..7L).map { dayData(it, 50.0, 3.0) }
        assertNull(BaselineService.computeBaseline(days))
    }

    @Test
    fun `exactly 8 valid days freezes a baseline from the pooled parts`() {
        val days = (1..8L).map { dayData(it, 50.0, 3.0) }
        val result = BaselineService.computeBaseline(days)!!

        assertEquals(8, result.daysUsed)
        assertTrue(result.isActive)
        assertEquals(50.0, result.c0, 0.0001)
        assertEquals(3.0, result.r0, 0.0001)
    }

    @Test
    fun `freeze uses the earliest 8 valid days, not any later ones, regardless of input order`() {
        val earliest8 = (1..8L).map { dayData(it, 50.0, 3.0) }
        val later2 = (9..10L).map { dayData(it, 999.0, 999.0) } // would change the result if wrongly included
        val days = (earliest8 + later2).shuffled(Random(42))

        val result = BaselineService.computeBaseline(days)!!

        assertEquals(50.0, result.c0, 0.0001)
        assertEquals(8, result.daysUsed)
    }

    @Test
    fun `computeBaseline is deterministic -- calling it twice on the same data gives identical rows`() {
        val days = (1..8L).map { dayData(it, 50.0, 3.0) }
        assertEquals(BaselineService.computeBaseline(days), BaselineService.computeBaseline(days))
    }

    @Test
    fun `isUnusualWeek flags a week under half the baseline's mean weekly minutes`() {
        assertTrue(BaselineService.isUnusualWeek(weekWindowMinutes = 100.0, baselineMeanWeeklyMinutes = 300.0))
        assertFalse(BaselineService.isUnusualWeek(weekWindowMinutes = 200.0, baselineMeanWeeklyMinutes = 300.0))
    }
}
