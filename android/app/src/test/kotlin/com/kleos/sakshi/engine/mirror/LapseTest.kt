package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** F15's golden checks (DOC 3/DOC4): 3 inactive days is a lapse, 2 is not, a gap excludes a day. */
class LapseTest {
    private val zone = Zones.KOLKATA
    private val firstReadDay = StudyDay(LocalDate.of(2026, 10, 1).toEpochDay())

    // externalResumes == 0 is "inactive" regardless of whether the zero phone-time included
    // Sakshi's own events or nothing at all -- D12 explicitly only counts non-own resumes.
    private fun inactiveDay(epochDay: Long) = DaySummary(
        day = StudyDay(epochDay), valid = true, windowMinutes = 0.0, quietMinutes = 0.0, inSetMinutes = 0.0,
        coverage = 1.0, pickups = 0, switchesPerHour = null, flinch = null, rampUpMin = null,
        lastScreenOffTs = null, firstStretchMin = null, externalResumes = 0,
    )

    private fun activeDay(epochDay: Long) = inactiveDay(epochDay).copy(externalResumes = 5)

    @Test
    fun `3 inactive seen days is a lapse of 3`() {
        val base = LocalDate.of(2026, 10, 10).toEpochDay()
        val days = (0..2L).map { inactiveDay(base + it) }

        val lapse = Lapse.detect(days, emptyList(), firstReadDay, zone)!!

        assertEquals(3, lapse.days)
        assertEquals(StudyDay(base + 2), lapse.endedAt)
    }

    @Test
    fun `2 inactive days is not a lapse`() {
        val base = LocalDate.of(2026, 10, 10).toEpochDay()
        val days = (0..1L).map { inactiveDay(base + it) }

        assertNull(Lapse.detect(days, emptyList(), firstReadDay, zone))
    }

    @Test
    fun `3 days inside a NOT_SEEN gap is not a lapse`() {
        val base = LocalDate.of(2026, 10, 10).toEpochDay()
        val days = (0..2L).map { inactiveDay(base + it) }
        val gap = DataGap(
            GapKind.NOT_SEEN,
            StudyDay(base).startEpochMs(zone),
            StudyDay(base + 2).endEpochMs(zone),
        )

        assertNull(Lapse.detect(days, listOf(gap), firstReadDay, zone))
    }

    @Test
    fun `an active day breaks the run even if inactive days surround it`() {
        val base = LocalDate.of(2026, 10, 10).toEpochDay()
        val days = listOf(inactiveDay(base), inactiveDay(base + 1), activeDay(base + 2), inactiveDay(base + 3))

        assertNull(Lapse.detect(days, emptyList(), firstReadDay, zone))
    }

    @Test
    fun `days before first_read_at never start or extend a run`() {
        // 2 inactive days before first_read_at, then only 1 inactive day after -- not enough on its own.
        val before = firstReadDay.epochDay - 2
        val days = (0..2L).map { inactiveDay(before + it) } // before, before+1 (=firstReadDay-1), before+2 (=firstReadDay)

        assertNull(Lapse.detect(days, emptyList(), firstReadDay, zone))
    }
}
