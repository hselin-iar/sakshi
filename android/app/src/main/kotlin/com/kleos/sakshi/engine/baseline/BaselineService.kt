package com.kleos.sakshi.engine.baseline

import com.kleos.sakshi.engine.metrics.WeekMetrics
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.tuning.Tuning

/** One valid day's worth of pooling input, already filtered to non-partial windows. */
data class ValidDayData(
    val day: StudyDay,
    val endOfDay: EpochMs,
    val windows: List<Window>,
    val stretches: List<Stretch>,
    val stays: List<Stay>,
)

/**
 * F5: freeze/re-anchor both compute a Baseline the same way (same pooling
 * code) over a chosen set of BASELINE_VALID_DAYS valid days — they differ
 * only in which 8 days the caller selects (earliest-since-first-read for
 * freeze, most-recent for re-anchor), so there is one shared computation
 * here. Deciding whether a freeze/re-anchor is actually allowed right now
 * (never overwrite an active baseline, re-anchor only once and only at
 * week >= 4) needs persisted state (StateStore) and belongs to the T2.8
 * orchestrator (UpdateBaseline.kt), not this pure function.
 */
object BaselineService {
    fun computeBaseline(days: List<ValidDayData>): Baseline? {
        if (days.size < Tuning.BASELINE_VALID_DAYS) return null
        val used = days.sortedBy { it.day.epochDay }.take(Tuning.BASELINE_VALID_DAYS)

        val parts = WeekMetrics.pool(
            used.flatMap { it.stretches },
            used.flatMap { it.stays },
            used.flatMap { it.windows },
        )
        val c0 = parts.stretchMin
        val p0 = parts.staysPerHour
        val r0 = parts.returnMin
        val q0 = parts.quietShare
        // Division by a zero baseline value is impossible: all four must be > 0 or the
        // baseline is not frozen (F5 error handling).
        if (c0 == null || p0 == null || r0 == null || q0 == null) return null
        if (c0 <= 0.0 || p0 <= 0.0 || r0 <= 0.0 || q0 <= 0.0) return null

        return Baseline(
            id = 0L,
            frozenAt = used.last().endOfDay,
            c0 = c0,
            p0 = p0,
            r0 = r0,
            q0 = q0,
            daysUsed = used.size,
            isActive = true,
        )
    }

    fun isUnusualWeek(weekWindowMinutes: Double, baselineMeanWeeklyMinutes: Double): Boolean =
        weekWindowMinutes < Tuning.UNUSUAL_WEEK_WINDOW_MIN_RATIO * baselineMeanWeeklyMinutes
}
