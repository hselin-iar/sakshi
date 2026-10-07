package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.baseline.BaselineService
import com.kleos.sakshi.engine.mirror.Lapse
import com.kleos.sakshi.engine.mirror.OwnUse
import com.kleos.sakshi.engine.mirror.SentenceBuilder
import com.kleos.sakshi.engine.mirror.TeacherLeaves
import com.kleos.sakshi.engine.metrics.Parts
import com.kleos.sakshi.engine.metrics.WeekMetrics
import com.kleos.sakshi.engine.metrics.Weeks
import com.kleos.sakshi.engine.metrics.steadiness
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * F5: the one place a week's pooled numbers are made. Pooling takes non-partial, finalised windows on valid days; the parts are
 * nullable and never zero. Only COMPLETED weeks are stored: a week in progress is summarised on the fly when a Mirror asks for it,
 * so trends and judging never see half a week.
 */
object BuildWeekSummary {
    fun qualifying(windows: List<WindowWithDetail>, validDays: Set<StudyDay>): List<WindowWithDetail> =
        windows.filter { !it.window.partial && it.window.finalised && it.window.day in validDays }

    /** No windows means no parts at all: the smoothing in WeekMetrics would otherwise invent "once an hour" for an empty week. */
    fun pool(windows: List<WindowWithDetail>): Parts =
        if (windows.isEmpty()) Parts(null, null, null, null, null, null)
        else WeekMetrics.pool(windows.flatMap { it.stretches }, windows.flatMap { it.stays }, windows.map { it.window })

    /** The baseline's mean weekly window minutes (the unusual-week yardstick): its own valid days, scaled to seven. */
    fun baselineMeanWeeklyMinutes(ports: Ports, baseline: Baseline?, zone: ZoneId): Double? {
        baseline ?: return null
        val firstRead = ports.state.settings().firstReadAt ?: return null
        val used = ports.derived.days(StudyDay.of(firstRead, zone), StudyDay.of(baseline.frozenAt, zone))
            .filter { it.valid }.sortedBy { it.day.epochDay }.take(baseline.daysUsed)
        if (used.isEmpty()) return null
        return used.sumOf { it.windowMinutes } / used.size * 7.0
    }

    fun summarize(
        week: WeekStart, daysInWeek: List<DaySummary>, qualifyingWindows: List<WindowWithDetail>, baseline: Baseline?,
        baselineMeanWeeklyMinutes: Double?, own: OwnUse, returnsAfterLapse: Int,
    ): WeekSummary {
        val parts = pool(qualifyingWindows)
        val steady = if (baseline != null) steadiness(parts, baseline) else null
        val weekMinutes = qualifyingWindows.sumOf { (it.window.end.value - it.window.start.value) / 60_000.0 }
        return WeekSummary(
            weekStart = week, stretchMedianMin = parts.stretchMin, longestStretchMin = parts.longestStretchMin,
            staysPerHour = parts.staysPerHour, returnMedianMin = parts.returnMin, quietShare = parts.quietShare, inSetShare = parts.inSetShare,
            steadiness = steady?.value, word = steady?.let { SentenceBuilder.steadinessWord(it.word) },
            windowsCount = qualifyingWindows.size, validDays = daysInWeek.count { it.valid },
            unusual = baselineMeanWeeklyMinutes != null && BaselineService.isUnusualWeek(weekMinutes, baselineMeanWeeklyMinutes),
            appOpens = own.opens, appMinutes = own.minutes, returnsAfterLapse = returnsAfterLapse,
        )
    }

    /** Own-package events of one week, for the teacher meter (F14). */
    fun ownUse(ports: Ports, week: WeekStart, asOf: EpochMs, zone: ZoneId): OwnUse {
        val own = ports.catalog.ownPackage()
        val from = Weeks.startMs(week, zone)
        val to = Weeks.endMs(week, zone)
        val events = ports.events.range(from, EpochMs(minOf(to.value, asOf.value + 1))).filter { it.pkg == own }
        return TeacherLeaves.meter(events, EpochMs(minOf(to.value, asOf.value)))
    }

    /** Builds and stores a summary for every completed week that has any day data. Returns them. */
    fun run(ports: Ports, asOf: EpochMs, zone: ZoneId): List<WeekSummary> {
        val today = StudyDay.of(asOf, zone)
        val allDays = ports.derived.days(StudyDay(0), today)
        if (allDays.isEmpty()) return emptyList()

        val baseline = ports.state.baseline()
        val mean = baselineMeanWeeklyMinutes(ports, baseline, zone)
        val firstReadDay = ports.state.settings().firstReadAt?.let { StudyDay.of(it, zone) }
        val lapse = firstReadDay?.let { Lapse.detect(allDays, ports.gaps.overlapping(EpochMs(0), EpochMs(asOf.value + 1)), it, zone) }
        val returnWeek = lapse?.let { Weeks.startOf(StudyDay(it.endedAt.epochDay + 1)) }

        return allDays.groupBy { Weeks.startOf(it.day) }
            .filterKeys { Weeks.isCompleted(it, asOf, zone) }
            .toSortedMap(compareBy { it.studyDay.epochDay })
            .map { (week, days) ->
                val windows = ports.derived.windows(Weeks.startMs(week, zone), Weeks.endMs(week, zone))
                val summary = summarize(
                    week, days, qualifying(windows, days.filter { it.valid }.map { it.day }.toSet()), baseline, mean,
                    ownUse(ports, week, asOf, zone), if (week == returnWeek) 1 else 0,
                )
                ports.derived.upsertWeek(summary)
                summary
            }
    }

    /** The newest completed week with enough valid days to be a Mirror (F6 step 1). */
    fun latestMirrorWeek(ports: Ports, asOf: EpochMs, zone: ZoneId): WeekStart? {
        val days = ports.derived.days(StudyDay(0), StudyDay.of(asOf, zone))
        return days.groupBy { Weeks.startOf(it.day) }
            .filter { (week, ds) -> Weeks.isCompleted(week, asOf, zone) && ds.count { it.valid } >= MIRROR_MIN_VALID_DAYS }
            .keys.maxByOrNull { it.studyDay.epochDay }
    }

    // DOC 3 F6 step 1 says "a completed week with >= 3 valid days" and F9 repeats "3 valid days"; Tuning.kt has no name for it.
    const val MIRROR_MIN_VALID_DAYS = 3
}
