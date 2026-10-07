package com.kleos.sakshi.engine.judging

import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StartReason
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.testkit.Zones
import com.kleos.sakshi.engine.testkit.epochMs
import java.time.LocalDate
import java.time.LocalTime

/**
 * Shared builders for the F12 judging tests.
 *
 * The experiment starts Monday 2026-10-05 04:00 (study-day start), so:
 *   after  = [Oct 5, Oct 19)  -> 10 weekdays (Mon-Fri twice)
 *   before = [Sep 21, Oct 5)  -> 10 weekdays
 * Putting every window on a weekday keeps both periods in the same day-class, so a test that is not about the
 * weekday mix cannot be changed by re-weighting.
 */
object JudgeFixtures {
    val zone = Zones.KOLKATA
    val subject = Pkg("com.chat")
    const val DAY_MS = 86_400_000L
    const val HOUR_MS = 3_600_000L

    val monday: LocalDate = LocalDate.of(2026, 10, 5)
    val started: EpochMs = epochMs(monday, LocalTime.of(4, 0), zone)
    val end: EpochMs = EpochMs(started.value + 14 * DAY_MS)

    /** The 10 weekdays that start at [firstMonday] (a Monday), 20:00 local. */
    fun weekdayStarts(firstMonday: LocalDate): List<EpochMs> =
        (0 until 14).map { firstMonday.plusDays(it.toLong()) }
            .filter { it.dayOfWeek.value <= 5 }
            .map { epochMs(it, LocalTime.of(20, 0), zone) }

    val afterStarts: List<EpochMs> = weekdayStarts(monday)
    val beforeStarts: List<EpochMs> = weekdayStarts(monday.minusDays(14))

    private var nextId = 1L

    fun stay(windowId: Long, startMs: Long, pkg: Pkg = subject, origin: Origin = Origin.SELF_STARTED, stone: Pkg? = null, returnMin: Double? = 6.0) =
        Stay(nextId++, windowId, EpochMs(startMs), EpochMs(startMs + 60_000), pkg, pkg, origin, stone, false, returnMin, 0)

    fun stretch(windowId: Long, startMs: Long, minutes: Double, quiet: Double, endedBy: EndedBy = EndedBy.STAY) =
        Stretch(nextId++, windowId, EpochMs(startMs), EpochMs(startMs + (minutes * 60_000).toLong()), minutes, minutes, quiet, endedBy)

    /**
     * One 1-hour window at [startMs] with [staysFromSubject] stays from the subject package, one stretch and stays that
     * all return in [returnMin] minutes. Day = the study day containing startMs.
     */
    fun window(
        startMs: EpochMs, staysFromSubject: Int, stretchMin: Double = 10.0, quiet: Double = 21.6, returnMin: Double = 6.0,
        partial: Boolean = false, otherStays: List<(Long) -> Stay> = emptyList(),
    ): WindowWithDetail {
        val id = nextId++
        val stays = (0 until staysFromSubject).map { stay(id, startMs.value + it * 120_000L, returnMin = returnMin) } +
            otherStays.map { it(id) }
        return WindowWithDetail(
            Window(id, StudyDay.of(startMs, zone), startMs, EpochMs(startMs.value + HOUR_MS), WindowSource.INFERRED, partial, true, null),
            listOf(stretch(id, startMs.value, stretchMin, quiet)), stays, quiet, 0,
        )
    }

    fun validDay(day: StudyDay, firstStretchMin: Double? = null) = DaySummary(
        day, valid = true, windowMinutes = 60.0, quietMinutes = 0.0, inSetMinutes = 0.0, coverage = 1.0, pickups = 0,
        switchesPerHour = null, flinch = null, rampUpMin = null, lastScreenOffTs = null, firstStretchMin = firstStretchMin, externalResumes = 0,
    )

    fun daysOf(vararg groups: List<WindowWithDetail>) = groups.flatMap { it }.map { it.window.day }.distinct().map { validDay(it) }

    fun week(monday: LocalDate, unusual: Boolean) = WeekSummary(
        WeekStart(StudyDay(monday.toEpochDay())), null, null, null, null, null, null, null, null, 0, 0, unusual, 0, 0.0, 0,
    )

    fun experiment(
        target: TargetMetric = TargetMetric.STAYS_PER_HOUR_FROM_PKG, kind: SuggestionKind = SuggestionKind.S2, subject: Pkg? = this.subject,
        verdict: Verdict = Verdict.PENDING, startedAt: EpochMs = started, id: Long = 1,
    ) = Experiment(
        id, kind, subject, startedAt, StartReason.TAP, target, null, null, EpochMs(startedAt.value + 14 * DAY_MS), verdict, false, false,
    )

    /** Baseline whose four parts match a window built with the defaults above. */
    val baseline = Baseline(1, EpochMs(0), c0 = 10.0, p0 = 4.0, r0 = 6.0, q0 = 0.36, daysUsed = 8, isActive = true)

    /** [n] windows (one per weekday start), `perWindow(i)` stays from the subject in window i. */
    fun windows(starts: List<EpochMs>, n: Int = starts.size, perWindow: (Int) -> Int, build: (EpochMs, Int) -> WindowWithDetail = { s, k -> window(s, k) }) =
        starts.take(n).mapIndexed { i, s -> build(s, perWindow(i)) }
}
