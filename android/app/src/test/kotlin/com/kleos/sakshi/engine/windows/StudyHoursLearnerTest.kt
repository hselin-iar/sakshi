package com.kleos.sakshi.engine.windows

import com.kleos.sakshi.engine.model.StudyBlock
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StudyHoursLearnerTest {
    private val zone = Zones.KOLKATA
    private fun window(day: Int, start: String, end: String, source: WindowSource = WindowSource.INFERRED, partial: Boolean = false): Window {
        val d = LocalDate.of(2026, 9, 14).plusDays(day.toLong())
        val endDate = if (LocalTime.parse(end) < LocalTime.parse(start)) d.plusDays(1) else d
        return Window(0, StudyDay(d.toEpochDay()), epochMs(d, LocalTime.parse(start), zone), epochMs(endDate, LocalTime.parse(end), zone), source, partial, true, null)
    }
    private fun evenings(n: Int, start: String = "19:00", end: String = "21:00") = (0 until n).map { window(it, start, end) }

    @Test fun `fewer than 14 inferred windows learn nothing`() = assertNull(StudyHoursLearner.learn(evenings(13), zone))

    @Test fun `14 windows give the median start and end`() =
        assertEquals(StudyBlock(19 * 60, 21 * 60), StudyHoursLearner.learn(evenings(14), zone))

    @Test fun `one odd window does not move the median`() {
        val w = evenings(14) + window(20, "07:00", "09:00")
        assertEquals(StudyBlock(19 * 60, 21 * 60), StudyHoursLearner.learn(w, zone))
    }

    @Test fun `windows from saved study hours and partial windows are not inferred`() {
        val w = evenings(10) + (0 until 6).map { window(30 + it, "19:00", "21:00", WindowSource.STUDY_HOURS) } + (0 until 6).map { window(40 + it, "19:00", "21:00", partial = true) }
        assertNull(StudyHoursLearner.learn(w, zone))
    }

    @Test fun `a block that crosses midnight is learned across midnight`() =
        assertEquals(StudyBlock(23 * 60, 1 * 60), StudyHoursLearner.learn((0 until 14).map { window(it, "23:00", "01:00") }, zone))

    @Test fun `a saved block within an hour at both ends means nothing to suggest`() {
        val learned = StudyBlock(19 * 60, 21 * 60)
        assertNull(StudyHoursLearner.suggest(learned, listOf(StudyBlock(19 * 60 + 30, 21 * 60 + 20))))
        assertEquals(learned, StudyHoursLearner.suggest(learned, listOf(StudyBlock(17 * 60, 21 * 60))))
        assertEquals(learned, StudyHoursLearner.suggest(learned, emptyList()))
        assertNull(StudyHoursLearner.suggest(null, emptyList()))
    }
}
