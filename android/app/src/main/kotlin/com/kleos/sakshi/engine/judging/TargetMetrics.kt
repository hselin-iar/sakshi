package com.kleos.sakshi.engine.judging

import com.kleos.sakshi.engine.metrics.WeekMetrics
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F12: the one number each suggestion tried to move, measured over a list of windows. Null means "cannot be measured here"
 * (no hours, no stretches, no returns): never zero, so a gap is never compared as if it were a result.
 *
 * Rates are per window-hour so two periods of different length compare fairly. Where DOC 3 names a target but not its
 * exact measure, the choice is made here and flagged:
 *  - STRETCH_IN_SLOT: the Experiment row carries no slot, so this is the length-weighted median stretch (F5's C) over the
 *    windows it is given. A caller that wants a slot passes only that slot's windows.
 *  - PINGS_FROM_PKG_IN_WINDOWS: the judge only sees windows, not raw notifications, so it counts the stays that began with
 *    a ping from the package (origin STONE, stonePkg = subject). Pings that led to no stay are invisible to it.
 *  - NEXT_DAY_FIRST_STRETCH: the mean DaySummary.firstStretchMin over the days given.
 */
object TargetMetrics {
    private val higherIsBetter = setOf(TargetMetric.STRETCH_IN_SLOT, TargetMetric.NEXT_DAY_FIRST_STRETCH, TargetMetric.PUTDOWN_SHARE)

    fun higherIsBetter(target: TargetMetric): Boolean = target in higherIsBetter

    fun value(target: TargetMetric, subject: Pkg?, windows: List<WindowWithDetail>, days: List<DaySummary> = emptyList()): Double? =
        when (target) {
            TargetMetric.STAYS_PER_HOUR_FROM_PKG -> subject?.let { perHour(windows) { s -> s.firstPkg == it } }
            TargetMetric.SELF_STARTED_PER_HOUR -> perHour(windows) { it.origin == Origin.SELF_STARTED }
            TargetMetric.PINGS_FROM_PKG_IN_WINDOWS -> subject?.let { perHour(windows) { s -> s.origin == Origin.STONE && s.stonePkg == it } }
            TargetMetric.MEDIAN_RETURN -> median(windows.flatMap { it.stays }.mapNotNull { it.returnMinutes })
            TargetMetric.FLINCH_RATE -> flinchRate(windows)
            TargetMetric.WORKSET_COVERAGE -> coverage(windows, subject)
            TargetMetric.STRETCH_IN_SLOT -> WeekMetrics.pool(windows.flatMap { it.stretches }, emptyList(), windows.map { it.window }).stretchMin
            TargetMetric.NEXT_DAY_FIRST_STRETCH -> days.mapNotNull { it.firstStretchMin }.takeIf { it.isNotEmpty() }?.average()
            TargetMetric.PUTDOWN_SHARE -> putDownShare(windows)
        }

    private fun hours(windows: List<WindowWithDetail>): Double =
        windows.sumOf { (it.window.end.value - it.window.start.value) / 3_600_000.0 }

    private fun perHour(windows: List<WindowWithDetail>, counts: (com.kleos.sakshi.engine.model.Stay) -> Boolean): Double? {
        val h = hours(windows)
        if (h <= 0.0) return null
        return windows.sumOf { w -> w.stays.count(counts) } / h
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    // Same definition as S6: the window's first stay begins within FLINCH_FIRST_MIN minutes of the window's start.
    private fun flinchRate(windows: List<WindowWithDetail>): Double? {
        if (windows.isEmpty()) return null
        val flinched = windows.count { w ->
            val first = w.stays.minByOrNull { it.start.value }
            first != null && first.start.value - w.window.start.value <= Tuning.FLINCH_FIRST_MIN * 60_000L
        }
        return flinched.toDouble() / windows.size
    }

    // Same measure S7 used: the package's stay time over window time (a stay is the engine's own off-set departure time).
    private fun coverage(windows: List<WindowWithDetail>, subject: Pkg?): Double? {
        val windowMs = windows.sumOf { it.window.end.value - it.window.start.value }
        if (windowMs <= 0L) return null
        val stayMs = windows.flatMap { it.stays }.filter { subject == null || it.pkgMain == subject }.sumOf { it.end.value - it.start.value }
        return stayMs.toDouble() / windowMs
    }

    private fun putDownShare(windows: List<WindowWithDetail>): Double? {
        val endings = windows.flatMap { it.stretches }.filter { it.endedBy == EndedBy.STAY || it.endedBy == EndedBy.PUT_DOWN }
        if (endings.isEmpty()) return null
        return endings.count { it.endedBy == EndedBy.PUT_DOWN }.toDouble() / endings.size
    }
}
