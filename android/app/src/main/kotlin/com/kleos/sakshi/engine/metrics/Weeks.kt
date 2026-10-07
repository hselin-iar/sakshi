package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import java.time.LocalDate
import java.time.ZoneId

/** A week starts on Monday at the study-day start (04:00 local). All week arithmetic lives here. */
object Weeks {
    const val DAY_MS = 86_400_000L

    fun startOf(day: StudyDay): WeekStart {
        val date = LocalDate.ofEpochDay(day.epochDay)
        return WeekStart(StudyDay(date.minusDays((date.dayOfWeek.value - 1).toLong()).toEpochDay()))
    }

    fun of(ts: EpochMs, zone: ZoneId): WeekStart = startOf(StudyDay.of(ts, zone))

    fun startMs(week: WeekStart, zone: ZoneId): EpochMs = week.studyDay.startEpochMs(zone)

    /** Exclusive end: the next Monday's study-day start. */
    fun endMs(week: WeekStart, zone: ZoneId): EpochMs = WeekStart(StudyDay(week.studyDay.epochDay + 7)).studyDay.startEpochMs(zone)

    fun days(week: WeekStart): List<StudyDay> = (0L until 7L).map { StudyDay(week.studyDay.epochDay + it) }

    fun next(week: WeekStart): WeekStart = WeekStart(StudyDay(week.studyDay.epochDay + 7))
    fun previous(week: WeekStart): WeekStart = WeekStart(StudyDay(week.studyDay.epochDay - 7))

    /** Whole weeks from `from` to `to`, both week starts. */
    fun between(from: WeekStart, to: WeekStart): Int = ((to.studyDay.epochDay - from.studyDay.epochDay) / 7).toInt()

    /** A week is completed once its end has passed. */
    fun isCompleted(week: WeekStart, asOf: EpochMs, zone: ZoneId): Boolean = endMs(week, zone).value <= asOf.value
}
