package com.kleos.sakshi.engine

import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Done-when: StudyDay.of tests (03:59, 04:00, a midnight-crossing window,
 * two time zones) pass. StudyDay.of itself is DOC 3's locked LC-1
 * definition: "local time minus 4 hours, then the local date."
 */
class StudyDayTest {

    private val oct6 = LocalDate.of(2026, 10, 6)
    private val oct7 = LocalDate.of(2026, 10, 7)

    @Test
    fun `03-59 local still belongs to the previous study day`() {
        val ts = epochMs(oct7, LocalTime.of(3, 59), Zones.KOLKATA)
        val previousDayAtFour = epochMs(oct6, LocalTime.of(4, 0), Zones.KOLKATA)
        assertEquals(StudyDay.of(previousDayAtFour, Zones.KOLKATA), StudyDay.of(ts, Zones.KOLKATA))
    }

    @Test
    fun `04-00 local starts the new study day`() {
        val ts = epochMs(oct7, LocalTime.of(4, 0), Zones.KOLKATA)
        val expected = StudyDay(oct7.toEpochDay())
        assertEquals(expected, StudyDay.of(ts, Zones.KOLKATA))
    }

    @Test
    fun `a midnight-crossing window stays on one study day until 04-00`() {
        // DOC 3's own example: 2026-10-07 01:30 local belongs to study day 2026-10-06.
        val afterMidnightBeforeFour = epochMs(oct7, LocalTime.of(1, 30), Zones.KOLKATA)
        val lateEveningBefore = epochMs(oct6, LocalTime.of(23, 30), Zones.KOLKATA)

        val sameDay = StudyDay(oct6.toEpochDay())
        assertEquals(sameDay, StudyDay.of(lateEveningBefore, Zones.KOLKATA))
        assertEquals(sameDay, StudyDay.of(afterMidnightBeforeFour, Zones.KOLKATA))
    }

    @Test
    fun `the same instant can land on different study days in different zones`() {
        // 2026-10-07 04:30 Kolkata (IST, UTC+5:30) is 2026-10-06 23:00 UTC.
        val ts = epochMs(oct7, LocalTime.of(4, 30), Zones.KOLKATA)

        val kolkataDay = StudyDay.of(ts, Zones.KOLKATA) // local 04:30 -> >= 04:00 -> new study day
        val utcDay = StudyDay.of(ts, Zones.UTC) // local (UTC) 23:00 the day before -> previous study day

        assertEquals(StudyDay(oct7.toEpochDay()), kolkataDay)
        assertEquals(StudyDay(oct6.toEpochDay()), utcDay)
        assertNotEquals(kolkataDay, utcDay)
    }
}
